package com.awakenedmod.awakened.content.brawler;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

public final class ReelInAbility extends BrawlerAbility {
   private static final String CLIP_HOOK = "brawler_reel_in";
   private static final String CLIP_YANK = "brawler_reel_yank";
   private static final String CLIP_WHIFF = "brawler_reel_whiff";
   private final Map<UUID, ReelInAbility.Grasp> grasps = this.playerState();

   public ReelInAbility() {
      super("brawler_reel_in", 6.0F, 7);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().damage(3.0).build();
   }

   @Override
   protected boolean cancelsOnDimensionChange() {
      return true;
   }

   @Override
   public float staminaCost(ServerPlayer player) {
      return pick(player).isEmpty() ? 0.0F : 6.0F;
   }

   @Override
   public String activationAnimation(ServerPlayer player) {
      return this.grasps.containsKey(player.getUUID()) ? "brawler_reel_in" : "brawler_reel_whiff";
   }

   @Override
   protected boolean press(ServerPlayer player) {
      if (this.grasps.containsKey(player.getUUID())) {
         return false;
      } else {
         List<LivingEntity> picked = pick(player);
         if (picked.isEmpty()) {
            BrawlerFx.whiff(player.serverLevel(), player);
            return true;
         } else {
            this.grasps.put(player.getUUID(), new ReelInAbility.Grasp(this.effectiveCost(player), picked));
            BrawlerFx.reelStart(player.serverLevel(), player);
            return true;
         }
      }
   }

   @Override
   public void serverTick(ServerPlayer player) {
      ReelInAbility.Grasp grasp = this.grasps.get(player.getUUID());
      if (grasp == null) {
         BrawlerVisualState.tickRetract(player);
      } else {
         if (!grasp.closed) {
            if (++grasp.ticks < 5) {
               this.hookOut(player, grasp, grasp.ticks);
               return;
            }

            grasp.closed = true;
            this.close(player, grasp);
         }

         this.drag(player, grasp);
         if (grasp.pulls.isEmpty()) {
            this.grasps.remove(player.getUUID());
            BrawlerVisualState.retract(player);
            PowerService.stopAnimation(player);
            PowerService.playAnimation(player, "brawler_reel_yank");
         }
      }
   }

   private void hookOut(ServerPlayer player, ReelInAbility.Grasp grasp, int tick) {
      int outStart = 2;
      if (tick >= outStart) {
         List<LivingEntity> held = this.committed(player, grasp);
         if (!held.isEmpty()) {
            BrawlerVisualState.extend(player, held.get(0));
            if (tick == outStart) {
               BrawlerFx.reelArmOut(player.serverLevel(), player);
            }
         }
      }
   }

   private List<LivingEntity> committed(ServerPlayer player, ReelInAbility.Grasp grasp) {
      if (!grasp.locked.isEmpty() && !holdable(player, grasp.locked.get(0)) && !grasp.repicked) {
         grasp.repicked = true;
         grasp.locked = pick(player);
      }

      List<LivingEntity> held = new ArrayList<>(grasp.locked.size());

      for (LivingEntity target : grasp.locked) {
         if (holdable(player, target)) {
            held.add(target);
         }
      }

      return held;
   }

   private static boolean holdable(ServerPlayer player, LivingEntity target) {
      if (target.isAlive() && !target.isRemoved() && target.level() == player.serverLevel() && Harm.allowed(player, target)) {
         double leash = 11.0 + (double)target.getBbWidth() * 0.5;
         return target.getBoundingBox().getCenter().distanceToSqr(player.getEyePosition()) <= leash * leash;
      } else {
         return false;
      }
   }

   private void close(ServerPlayer player, ReelInAbility.Grasp grasp) {
      ServerLevel level = player.serverLevel();
      List<LivingEntity> caught = this.committed(player, grasp);
      if (caught.isEmpty()) {
         this.whiff(player, grasp.paid);
      } else {
         BrawlerVisualState.hold(player, caught.get(0));
         List<LivingEntity> landed = new ArrayList<>(caught.size());
         boolean transition = false;

         for (int i = 0; i < caught.size(); i++) {
            LivingEntity target = caught.get(i);
            Mark was = Marks.current(target);
            float bonus = MarkBonus.of(MarkBonus.REEL_IN, was, Marks.flavour(target));
            transition |= Marks.consumeTransition(player, target, this.id());
            float damage = MasteryScaling.scaledDamage(3.0F, player, this.balanceKey(), bonus, BrawlerCombo.bonusFraction(player));
            if (Dmg.hurtThrough(target, level, Dmg.by(player, DamageTypes.PLAYER_ATTACK), damage)) {
               landed.add(target);
            }

            interrupt(target);
            Marks.release(target);
            if (BrawlerTargets.unliftable(target)) {
               this.arrive(player, new ReelInAbility.Pull(target, Vec3.ZERO, was));
            } else {
               BrawlerHitStop.moving(target, 20);
               grasp.pulls.add(new ReelInAbility.Pull(target, dropOffset(player, i, caught.size()), was));
            }
         }

         BrawlerFx.reelHook(level, player, caught);
         if (!landed.isEmpty()) {
            BrawlerCombo.abilityHit(player, landed.get(0), landed.subList(1, landed.size()), transition ? 3 : 2, -1);
         }
      }
   }

   private void drag(ServerPlayer player, ReelInAbility.Grasp grasp) {
      ServerLevel level = player.serverLevel();
      if (!grasp.pulls.isEmpty()) {
         BrawlerFx.reelStrain(level, player, ++grasp.ticks);
      }

      grasp.pulls.removeIf(pull -> {
         LivingEntity target = pull.target;
         if (target.isAlive() && !target.isRemoved() && target.level() == level) {
            Vec3 to = player.position().add(pull.offset.scale(1.8)).subtract(target.position());
            double dist = to.length();
            if (!(dist <= 1.0) && ++pull.ticks < 20) {
               Vec3 step = to.scale(Math.min(1.0, dist) / dist);
               double lift = pull.was == Mark.AIRBORNE ? 0.0 : 0.12;
               Harm.setMotion(player, target, step.x, step.y + lift, step.z);
               target.resetFallDistance();
               BrawlerFx.reelTrail(level, player, target);
               return false;
            } else {
               this.arrive(player, pull);
               return true;
            }
         } else {
            return true;
         }
      });
      if (!grasp.pulls.isEmpty()) {
         BrawlerVisualState.hold(player, grasp.pulls.get(0).target);
      }
   }

   private void arrive(ServerPlayer player, ReelInAbility.Pull pull) {
      boolean down = pull.was == Mark.AIRBORNE;
      Marks.apply(player, pull.target, down ? Mark.GROUNDED : Mark.PINNED, down ? 44 : 44, down ? Mark.Flavour.NONE : Mark.Flavour.HAULED, this.id());
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      if (this.grasps.containsKey(player.getUUID())) {
         PowerService.stopAnimation(player);
      }

      BrawlerVisualState.clear(player);
      super.clearPlayer(player);
   }

   private static void interrupt(LivingEntity target) {
      target.stopUsingItem();
      if (target instanceof Mob mob) {
         mob.getNavigation().stop();
         mob.setAggressive(false);
      }
   }

   private static Vec3 dropOffset(ServerPlayer player, int index, int count) {
      Vec3 flat = AimUtil.lookFlat(player);
      double spread = count <= 1 ? 0.0 : ((double)index - (double)(count - 1) / 2.0) * 28.0;
      double a = Math.toRadians(spread);
      double cos = Math.cos(a);
      double sin = Math.sin(a);
      return new Vec3(flat.x * cos - flat.z * sin, 0.0, flat.x * sin + flat.z * cos);
   }

   private static List<LivingEntity> pick(ServerPlayer player) {
      Vec3 look = player.getLookAngle();
      Vec3 eye = player.getEyePosition();
      Predicate<LivingEntity> ours = targetx -> Harm.allowed(player, targetx) && BrawlerTargets.lenientSight(player.serverLevel(), eye, targetx, 4.0);
      List<LivingEntity> cone = new ArrayList<>();

      for (LivingEntity target : AimUtil.livingAround(player, player.position(), 10.0)) {
         if (BrawlerTargets.inCone(player, target, 8.0, 45.0) && ours.test(target)) {
            cone.add(target);
         }
      }

      cone.sort(Comparator.comparingDouble(targetx -> -offAxis(eye, look, targetx)));
      LivingEntity primary = AimUtil.aimedLivingEntity(player, 8.0, ours);
      if (primary == null && !cone.isEmpty()) {
         primary = cone.get(0);
      }

      List<LivingEntity> picked = new ArrayList<>(2);
      if (primary == null) {
         return picked;
      } else {
         picked.add(primary);
         double clearly = Math.cos(Math.toRadians(20.0));

         for (LivingEntity targetx : cone) {
            if (picked.size() >= 2) {
               break;
            }

            if (targetx != primary && offAxis(eye, look, targetx) >= clearly) {
               picked.add(targetx);
            }
         }

         return picked;
      }
   }

   private static double offAxis(Vec3 eye, Vec3 look, LivingEntity target) {
      Vec3 to = target.position().add(0.0, (double)target.getBbHeight() * 0.5, 0.0).subtract(eye);
      double len = to.length();
      return len < 1.0E-4 ? 1.0 : to.scale(1.0 / len).dot(look);
   }

   private static final class Grasp {
      final float paid;
      List<LivingEntity> locked;
      boolean repicked;
      int ticks;
      boolean closed;
      final List<ReelInAbility.Pull> pulls = new ArrayList<>(2);

      Grasp(float paid, List<LivingEntity> locked) {
         this.paid = paid;
         this.locked = locked;
      }
   }

   private static final class Pull {
      final LivingEntity target;
      final Vec3 offset;
      final Mark was;
      int ticks;

      Pull(LivingEntity target, Vec3 offset, Mark was) {
         this.target = target;
         this.offset = offset;
         this.was = was;
      }
   }
}
