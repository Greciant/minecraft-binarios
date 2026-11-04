package com.awakenedmod.awakened.content.brawler;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class SledgeDropAbility extends BrawlerAbility {
   private final Map<UUID, SledgeDropAbility.Cast> casts = this.playerState();

   public SledgeDropAbility() {
      super("brawler_sledge_drop", 8.0F, 2);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().damage(6.0).build();
   }

   @Override
   protected boolean cancelsOnDimensionChange() {
      return true;
   }

   @Override
   protected boolean press(ServerPlayer player) {
      if (this.casts.containsKey(player.getUUID())) {
         return false;
      } else {
         LivingEntity target = airTarget(player);
         SledgeDropAbility.Mode mode = travelMode(player, target);
         this.casts.put(player.getUUID(), new SledgeDropAbility.Cast(mode, target));
         BrawlerFx.sledgeStart(player.serverLevel(), player);
         return true;
      }
   }

   private static SledgeDropAbility.Mode travelMode(ServerPlayer player, LivingEntity target) {
      if (target != null && !within(player, target, 1.0) && startLeap(player, target)) {
         return SledgeDropAbility.Mode.LEAP;
      } else {
         return player.onGround() ? SledgeDropAbility.Mode.PLANTED : SledgeDropAbility.Mode.DIVE;
      }
   }

   public String simCast(ServerPlayer player) {
      SledgeDropAbility.Cast cast = this.casts.get(player.getUUID());
      return cast == null ? "-" : cast.mode + (cast.target == null ? "" : ":" + cast.target.getId());
   }

   @Override
   public void serverTick(ServerPlayer player) {
      SledgeDropAbility.Cast cast = this.casts.get(player.getUUID());
      if (cast != null) {
         LivingEntity target = alive(cast.target);
         travel(player, cast, target);
         if (cast.ticks + 1 < 7) {
            cast.ticks++;
         } else if (!arrived(player, cast, target)) {
            cast.travelTicks++;
         } else {
            this.casts.remove(player.getUUID());
            this.impact(player, target);
         }
      }
   }

   private static boolean arrived(ServerPlayer player, SledgeDropAbility.Cast cast, LivingEntity target) {
      if (cast.mode == SledgeDropAbility.Mode.PLANTED) {
         return true;
      } else if (target != null && within(player, target, 1.0)) {
         return true;
      } else {
         return cast.travelTicks >= (target == null ? 40 : 20) ? true : player.onGround() || player.verticalCollision;
      }
   }

   private static LivingEntity alive(LivingEntity target) {
      return target != null && target.isAlive() && !target.isRemoved() ? target : null;
   }

   private static boolean within(ServerPlayer player, LivingEntity target, double pad) {
      return target.getBoundingBox().inflate(pad).contains(player.position().add(0.0, (double)player.getBbHeight() * 0.5, 0.0));
   }

   private static LivingEntity airTarget(ServerPlayer player) {
      LivingEntity aimed = AimUtil.aimedLivingSoft(player, 6.0, 40.0, e -> e != player && Harm.allowed(player, e) && Marks.airborne(e));
      if (aimed != null) {
         return aimed;
      } else {
         for (LivingEntity body : Marks.recent(player)) {
            if (body != player && Harm.allowed(player, body) && Marks.airborne(body) && roughlyToward(player, body)) {
               return body;
            }
         }

         return null;
      }
   }

   private static boolean roughlyToward(ServerPlayer player, LivingEntity body) {
      Vec3 to = body.getBoundingBox().getCenter().subtract(player.getEyePosition());
      double dist = to.length();
      if (dist > 8.0) {
         return false;
      } else {
         return !(dist <= 3.0) && !(dist < 1.0E-4) ? to.scale(1.0 / dist).dot(player.getLookAngle()) >= Math.cos(Math.toRadians(65.0)) : true;
      }
   }

   private static double leapHeight(ServerPlayer player, LivingEntity target) {
      return target.getY() + 0.3 - player.getY();
   }

   private static boolean startLeap(ServerPlayer player, LivingEntity target) {
      double climb = leapHeight(player, target);
      if (climb < 0.4) {
         if (!player.onGround()) {
            return false;
         }

         climb = 0.9;
      }

      return BrawlerLift.rideUp(player, climb, 0, homing(player, target));
   }

   private static Vec3 homing(ServerPlayer player, LivingEntity target) {
      Vec3 to = target.position().subtract(player.position());
      double flat = Math.hypot(to.x, to.z);
      if (flat < 0.001) {
         return Vec3.ZERO;
      } else {
         double speed = Math.min(flat, Math.min(flat * 0.35, 0.6));
         return new Vec3(to.x / flat * speed, 0.0, to.z / flat * speed);
      }
   }

   private static void travel(ServerPlayer player, SledgeDropAbility.Cast cast, LivingEntity target) {
      if (cast.mode != SledgeDropAbility.Mode.PLANTED) {
         Vec3 v = player.getDeltaMovement();
         Vec3 across = target == null ? v : homing(player, target);
         player.resetFallDistance();
         if (target != null && within(player, target, 1.0)) {
            sync(player, across.x, target.getDeltaMovement().y, across.z);
         } else {
            double gap = target == null ? Double.MAX_VALUE : player.getY() - target.getY();
            if (gap < 0.0) {
               sync(player, across.x, v.y, across.z);
            } else {
               BrawlerLift.endHover(player);
               double drop = target == null ? 1.2 : Math.max(0.15, Math.min(1.2, gap));
               sync(player, across.x, target == null ? Math.min(v.y, -drop) : -drop, across.z);
            }
         }
      }
   }

   private static void sync(ServerPlayer player, double x, double y, double z) {
      player.setDeltaMovement(x, y, z);
      player.hurtMarked = true;
   }

   private void impact(ServerPlayer player, LivingEntity committed) {
      ServerLevel level = player.serverLevel();
      Vec3 feet = player.position();
      List<LivingEntity> cone = new ArrayList<>();

      for (LivingEntity target : AimUtil.livingAround(player, feet, 5.2)) {
         if (Harm.allowed(player, target) && BrawlerTargets.inCone(player, target, 3.2, 50.0)) {
            cone.add(target);
         }
      }

      rescue(player, cone, committed);
      promote(player, cone, committed);
      boolean spike = cone.stream().anyMatch(Marks::airborne);
      double ringRadius = spike ? 5.0 : 3.5;
      List<LivingEntity> ring = new ArrayList<>();

      for (LivingEntity targetx : AimUtil.livingAround(player, feet, ringRadius)) {
         if (!cone.contains(targetx) && Harm.allowed(player, targetx)) {
            ring.add(targetx);
         }
      }

      List<LivingEntity> landed = new ArrayList<>();
      List<SledgeDropAbility.Spiked> spiked = new ArrayList<>();
      boolean transition = false;

      for (LivingEntity targetxx : cone) {
         transition |= this.strike(player, level, targetxx, landed, spiked);
      }

      for (LivingEntity targetxx : ring) {
         this.shoveOut(player, targetxx, feet);
      }

      if (cone.isEmpty() && ring.isEmpty()) {
         this.whiff(player);
      }

      if (spiked.isEmpty()) {
         this.beat(player, level, feet, ringRadius, spike, cone, landed, transition);
      } else {
         LivingEntity anchor = spiked.get(0).target();
         boolean transitioned = transition;
         Marks.atLanding(
            anchor,
            ground -> {
               if (!player.isRemoved()) {
                  for (SledgeDropAbility.Spiked hit : spiked) {
                     LivingEntity targetxx = hit.target();
                     if (targetxx.isAlive()
                        && !targetxx.isRemoved()
                        && Dmg.hurtThrough(targetxx, ground, Dmg.by(player, DamageTypes.PLAYER_ATTACK), hit.damage())) {
                        landed.add(targetxx);
                     }

                     BrawlerFx.sledgeSpike(ground, targetxx);
                  }

                  this.beat(player, ground, anchor.position(), ringRadius, spike, cone, landed, transitioned);
               }
            }
         );
      }
   }

   private void beat(
      ServerPlayer player,
      ServerLevel level,
      Vec3 where,
      double ringRadius,
      boolean spike,
      List<LivingEntity> cone,
      List<LivingEntity> landed,
      boolean transition
   ) {
      AimUtil.groundedPositionIfFound(level, where).ifPresent(ground -> BrawlerFx.sledgeImpact(level, ground, ringRadius, spike));
      BrawlerFx.sledgeOwnerKick(player, spike);
      if (!landed.isEmpty()) {
         landed.sort(Comparator.comparingInt(cone::indexOf));
         BrawlerCombo.abilityHit(player, landed.get(0), landed.subList(1, landed.size()), transition ? 3 : 2, 4);
      }
   }

   private static void promote(ServerPlayer player, List<LivingEntity> cone, LivingEntity committed) {
      if (cone.size() >= 2) {
         LivingEntity primary = committed != null && cone.contains(committed)
            ? committed
            : AimUtil.aimedLivingSoft(player, 3.2, 50.0, e -> e != player && cone.contains(e));
         if (primary != null && cone.remove(primary)) {
            cone.add(0, primary);
         }
      }
   }

   private static void rescue(ServerPlayer player, List<LivingEntity> cone, LivingEntity committed) {
      if (committed != null
         && !cone.contains(committed)
         && committed.isAlive()
         && !committed.isRemoved()
         && Harm.allowed(player, committed)
         && within(player, committed, 2.0)) {
         cone.add(committed);
      }
   }

   private boolean strike(ServerPlayer player, ServerLevel level, LivingEntity target, List<LivingEntity> landed, List<SledgeDropAbility.Spiked> spiked) {
      Mark was = Marks.shapeFor(target);
      boolean airborne = Marks.airborne(target);
      float bonus = MarkBonus.of(MarkBonus.SLEDGE_DROP, was, Marks.flavour(target));
      boolean transition = Marks.consumeTransition(player, target, this.id());
      float damage = MasteryScaling.scaledDamage(6.0F, player, this.balanceKey(), bonus, BrawlerCombo.bonusFraction(player));
      if (!airborne && Dmg.hurtThrough(target, level, Dmg.by(player, DamageTypes.PLAYER_ATTACK), damage)) {
         landed.add(target);
      }

      Marks.apply(player, target, Mark.GROUNDED, 44, Mark.Flavour.NONE, this.id());
      if (airborne) {
         BrawlerFx.sledgeContact(level, target);
         spiked.add(new SledgeDropAbility.Spiked(target, damage));
      }

      return transition;
   }

   private void shoveOut(ServerPlayer player, LivingEntity target, Vec3 feet) {
      Vec3 out = AimUtil.horizontalDir(feet, target.position()).scale(0.55);
      BrawlerFollowUp.shove(player, target, out, 4, Mark.PINNED, 44, Mark.Flavour.NONE, this.id());
   }

   private static final class Cast {
      final SledgeDropAbility.Mode mode;
      final LivingEntity target;
      int ticks;
      int travelTicks;

      Cast(SledgeDropAbility.Mode mode, LivingEntity target) {
         this.mode = mode;
         this.target = target;
      }
   }

   private static enum Mode {
      PLANTED,
      DIVE,
      LEAP;
   }

   private static record Spiked(LivingEntity target, float damage) {
   }
}
