package com.awakenedmod.awakened.content.limitless.red;

import com.awakenedmod.awakened.content.limitless.LimitlessCast;
import com.awakenedmod.awakened.content.limitless.LimitlessFeats;
import com.awakenedmod.awakened.content.limitless.LimitlessFx;
import com.awakenedmod.awakened.content.limitless.LimitlessHands;
import com.awakenedmod.awakened.content.limitless.LimitlessPower;
import com.awakenedmod.awakened.content.limitless.blue.BlueClumps;
import com.awakenedmod.awakened.content.limitless.blue.BluePop;
import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.FallGuard;
import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.ForceBlast;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.HitStops;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.RealisticMovement;
import com.awakenedmod.awakened.core.WallSlam;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class RedAbility extends Ability {
   public static final String ID = "limitless_red";
   private static final ResourceLocation FALL_GUARD_ID = AwId.of("limitless_red_fall_guard");
   private static final String ANIM_POINT = "limitless_red_point";
   private static final String ANIM_RECOIL = "limitless_red_recoil";
   private final Map<UUID, RedAbility.Gather> gathers = this.activeState();
   private final Map<UUID, List<RedAbility.Shot>> shots = this.playerState();
   private final Map<UUID, HitStops> flung = this.playerState();
   private final FallGuard guard = new FallGuard(FALL_GUARD_ID, 10, 400);
   private final WallSlam slams = new WallSlam(14, 0.35);
   private final BlueClumps blue;

   public RedAbility(BlueClumps blue) {
      super("limitless_red", 15.0F, 2);
      this.blue = blue;
   }

   @Override
   public String activationAnimation(ServerPlayer player) {
      return null;
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().damage(9.0).build();
   }

   @Override
   protected boolean cancelsOnDimensionChange() {
      return true;
   }

   @Override
   public boolean activate(ServerPlayer player) {
      if (!this.gathers.containsKey(player.getUUID()) && !LimitlessPower.castingOther(player, this)) {
         int now = (int)player.serverLevel().getGameTime();
         boolean shift = player.isShiftKeyDown();
         int start = shift ? now - 9 : now;
         this.gathers.put(player.getUUID(), new RedAbility.Gather(start, shift));
         LimitlessCast.set(player, LimitlessCast.RED, LimitlessCast.started(start));
         PowerService.playAnimation(player, "limitless_red_point", true);
         return true;
      } else {
         return this.fail(player, "awakened.fail.busy", new Object[0]);
      }
   }

   @Override
   public void serverTick(ServerPlayer player) {
      this.guard.tick(player, false);
      this.slams.tick(player, this::slam);
      RedAbility.Gather gather = this.gathers.get(player.getUUID());
      if (gather != null) {
         int ticks = (int)player.serverLevel().getGameTime() - gather.start();
         LimitlessFx.redGathering(player, Math.min(1.0F, (float)ticks / 14.0F));
         if (ticks >= 14) {
            this.fire(player, gather);
         }
      }

      this.tickShots(player);
      this.tickThrows(player);
   }

   private void fire(ServerPlayer player, RedAbility.Gather gather) {
      this.gathers.remove(player.getUUID());
      ServerLevel level = player.serverLevel();
      int now = (int)level.getGameTime();
      Vec3 eye = player.getEyePosition();
      Vec3 look = player.getLookAngle();
      boolean pointBlank = pointBlank(player, gather);
      RedAbility.Path path = this.trace(player, level, eye, look, pointBlank ? 1.5 : 16.0);
      Vec3 burst = eye.add(look.scale(Math.max(0.05, path.dist() - 0.05)));
      Vec3 tip = LimitlessHands.redTip(eye, (double)player.getYRot(), (double)player.getXRot(), true);
      if (eye.distanceTo(burst) < eye.distanceTo(tip) + 0.1 || AimUtil.clipBlocks(level, player, eye, tip).getType() != Type.MISS) {
         tip = eye;
      }

      Vec3 to = burst.subtract(tip);
      double dist = to.length();
      Vec3 dir = dist < 1.0E-4 ? look : to.scale(1.0 / dist);
      int flight = Math.max(1, (int)Math.ceil(dist / 1.8));
      RedBlastEntity.spawn(
         level,
         player.getId(),
         now,
         tip,
         dir,
         burst,
         flight,
         floorUnder(level, burst.subtract(dir.scale(0.3))),
         LimitlessCast.seed(player.getId(), gather.start())
      );
      LimitlessCast.end(player, LimitlessCast.RED, now, true);
      this.shots
         .computeIfAbsent(player.getUUID(), k -> new ArrayList<>())
         .add(new RedAbility.Shot(burst, look, level.getGameTime() + (long)flight, path.target(), pointBlank));
      LimitlessFx.redGatherEnded(player);
      LimitlessFx.redFired(level, tip);
      PowerService.stopHeldAnimation(player, "limitless_red_point");
      PowerService.playAnimation(player, "limitless_red_recoil");
   }

   private RedAbility.Path trace(ServerPlayer player, ServerLevel level, Vec3 from, Vec3 dir, double max) {
      Vec3 end = from.add(dir.scale(max));
      BlockHitResult block = AimUtil.clipBlocks(level, player, from, end);
      double best = block.getType() == Type.MISS ? max : block.getLocation().distanceTo(from);
      LivingEntity target = null;
      Vec3 to = from.add(dir.scale(best));

      for (LivingEntity e : Harm.hostilesOnly(player, AimUtil.livingAlong(player, from, to, 0.25))) {
         if (!e.isSpectator()) {
            AABB box = e.getBoundingBox().inflate(0.25);
            double d = box.contains(from) ? 0.0 : box.clip(from, to).map(p -> p.distanceTo(from)).orElse(best);
            if (d < best) {
               best = d;
               target = e;
            }
         }
      }

      double orb = this.blue.orbAlong(player, from, dir, best);
      return orb < best ? new RedAbility.Path(orb, null) : new RedAbility.Path(best, target);
   }

   private static double floorUnder(ServerLevel level, Vec3 at) {
      BlockHitResult hit = AimUtil.clipBlocks(level, null, at, at.subtract(0.0, 6.0, 0.0));
      return hit.getType() == Type.MISS ? at.y - 64.0 : hit.getLocation().y;
   }

   private void tickShots(ServerPlayer player) {
      List<RedAbility.Shot> list = this.shots.get(player.getUUID());
      if (list != null) {
         long now = player.serverLevel().getGameTime();
         Iterator<RedAbility.Shot> it = list.iterator();

         while (it.hasNext()) {
            RedAbility.Shot shot = it.next();
            if (now >= shot.hitAt()) {
               it.remove();
               this.burst(player, shot);
            }
         }

         if (list.isEmpty()) {
            this.shots.remove(player.getUUID());
         }
      }
   }

   private void burst(ServerPlayer player, RedAbility.Shot shot) {
      ServerLevel level = player.serverLevel();
      Vec3 at = shot.burst();
      float damage = MasteryScaling.scaledDamage(9.0F, player, this.balanceKey());
      List<BluePop> pops = this.blue.popAt(player, at);
      Set<LivingEntity> popped = new LinkedHashSet<>();

      for (BluePop pop : pops) {
         popped.addAll(pop.bodies());
      }

      LivingEntity target = star(player, shot);
      if (target != null && popped.contains(target)) {
         target = null;
      }

      Set<LivingEntity> spare = new LinkedHashSet<>(popped);
      if (target != null) {
         spare.add(target);
      }

      List<LivingEntity> caught = new ArrayList<>(ForceBlast.shockwave(player, level, at, 3.0, damage, 0.85, 0.5, 2.2, spare, true));

      for (LivingEntity body : caught) {
         this.slams.track(player, body, body.position().subtract(at));
      }

      if (target != null) {
         Dmg.hurtNoKnockback(target, level, player.damageSources().playerAttack(player), damage);
         this.throwAfterStop(player, target, shot.look(), 2.0, 0.7);
         caught.add(target);
      }

      List<LivingEntity> comboHit = new ArrayList<>();

      for (BluePop pop : pops) {
         comboHit.addAll(this.combo(player, level, pop, shot.look(), damage * 2.0F));
      }

      caught.addAll(comboHit);
      LimitlessFeats.comboLanded(player, comboHit);
      if (!caught.isEmpty()) {
         MasterySystem.award(player, MasteryReason.HIT, caught);
      }

      LimitlessFx.redBurst(level, at);
      this.throwSelf(player, at, shot.pointBlank() ? 1.8 : 1.35);
   }

   private List<LivingEntity> combo(ServerPlayer player, ServerLevel level, BluePop pop, Vec3 look, float damage) {
      List<LivingEntity> hit = new ArrayList<>();

      for (LivingEntity body : pop.bodies()) {
         if (body.isAlive() && body.level() == level && Harm.allowed(player, body) && !Foes.fightsFor(player, body)) {
            Dmg.hurtThrough(body, level, player.damageSources().playerAttack(player), damage);
            Vec3 out = body.position().subtract(pop.center());
            this.throwAfterStop(player, body, out.horizontalDistanceSqr() < 0.04 ? look : out, 1.1, 0.35);
            hit.add(body);
         }
      }

      return hit;
   }

   @Nullable
   private static LivingEntity star(ServerPlayer player, RedAbility.Shot shot) {
      LivingEntity t = shot.target();
      return t != null
            && t.isAlive()
            && !t.isRemoved()
            && t.level() == player.serverLevel()
            && !(AimUtil.reachToBox(t, shot.burst()) > 3.0)
            && Harm.allowed(player, t)
            && !Foes.fightsFor(player, t)
         ? t
         : null;
   }

   private void throwAfterStop(ServerPlayer player, LivingEntity target, Vec3 dir, double out, double up) {
      Vec3 flat = new Vec3(dir.x, 0.0, dir.z);
      flat = flat.lengthSqr() < 1.0E-4 ? AimUtil.lookFlat(player) : flat.normalize();
      Vec3 push = new Vec3(flat.x * out, up, flat.z * out);
      this.flung.computeIfAbsent(player.getUUID(), k -> new HitStops()).hold(target, 3, body -> {
         if (body.level() == player.serverLevel()) {
            this.fling(player, body, push);
         }
      });
   }

   private void tickThrows(ServerPlayer player) {
      HitStops stops = this.flung.get(player.getUUID());
      if (stops != null) {
         stops.tick();
         if (stops.isEmpty()) {
            this.flung.remove(player.getUUID());
         }
      }
   }

   private void fling(ServerPlayer player, LivingEntity body, Vec3 push) {
      body.setOnGround(false);
      if (Harm.setMotion(player, body, push)) {
         this.slams.track(player, body, push);
      }
   }

   private static boolean pointBlank(ServerPlayer player, RedAbility.Gather gather) {
      return (gather.shift() || player.isShiftKeyDown()) && !RealisticMovement.blocksAirLaunch(player);
   }

   private void throwSelf(ServerPlayer player, Vec3 at, double push) {
      double d = AimUtil.reachToBox(player, at);
      if (!(d >= 3.0) && player.isAlive() && !RealisticMovement.blocksAirLaunch(player)) {
         double strength = Math.min(1.0, (3.0 - d) / 1.5);
         Vec3 away = player.getEyePosition().subtract(at);
         Vec3 dir = away.lengthSqr() < 1.0E-6 ? player.getLookAngle().reverse() : away.normalize();
         Vec3 v = dir.scale(push * strength);
         if (Math.abs(dir.y) < 0.5) {
            v = new Vec3(v.x, Math.max(v.y, 0.45 * strength), v.z);
         }

         Vec3 was = player.getDeltaMovement();
         player.setDeltaMovement(v.add(was.x * 0.5, 0.0, was.z * 0.5));
         player.hurtMarked = true;
         player.resetFallDistance();
         this.guard.arm(player);
      }
   }

   private void slam(ServerPlayer player, LivingEntity body, Vec3 wall) {
      ServerLevel level = player.serverLevel();
      float damage = MasteryScaling.scaledDamage(6.0F, player, this.balanceKey());
      if (Dmg.hurtThrough(body, level, Dmg.by(player, DamageTypes.PLAYER_ATTACK), damage)) {
         MasterySystem.award(player, MasteryReason.HIT, body);
         LimitlessFx.redSlam(level, wall);
      }
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      RedAbility.Gather gather = this.gathers.remove(player.getUUID());
      if (gather != null) {
         this.resource(player).refund(player, this.effectiveCost(player));
         LimitlessFx.redGatherEnded(player);
         PowerService.stopHeldAnimation(player, "limitless_red_point");
      }

      LimitlessCast.end(player, LimitlessCast.RED, (int)player.serverLevel().getGameTime(), false);
      HitStops held = this.flung.remove(player.getUUID());
      if (held != null) {
         held.releaseAll();
      }

      this.slams.clear(player);
      this.guard.clear(player);
      super.clearPlayer(player);
   }

   private static record Gather(int start, boolean shift) {
   }

   private static record Path(double dist, @Nullable LivingEntity target) {
   }

   private static record Shot(Vec3 burst, Vec3 look, long hitAt, @Nullable LivingEntity target, boolean pointBlank) {
   }
}
