package com.devilfruits.mobs.pirate.goal;

import com.devilfruits.core.npc.FactionFighter;
import com.devilfruits.mobs.pirate.PirateClips;
import com.devilfruits.mobs.pirate.PirateConfig;
import com.devilfruits.mobs.pirate.entity.PirateFighter;
import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class PirateMeleeGoal extends Goal {
   private final PirateFighter mob;
   private final double speed;
   private final boolean barges;
   private int cooldown;
   private int bargeCooldown;
   private int repath;

   public PirateMeleeGoal(PirateFighter mob, double speed, boolean barges) {
      this.mob = mob;
      this.speed = speed;
      this.barges = barges;
      this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
   }

   public boolean canUse() {
      LivingEntity target = this.mob.getTarget();
      return target != null && target.isAlive();
   }

   public boolean canContinueToUse() {
      return this.canUse();
   }

   public void start() {
      this.mob.setAggressive(true);
      this.repath = 0;
      this.cooldown = Math.max(this.cooldown, 6);
   }

   public void stop() {
      this.mob.setAggressive(false);
      this.mob.getNavigation().stop();
   }

   public boolean requiresUpdateEveryTick() {
      return true;
   }

   public void tick() {
      LivingEntity target = this.mob.getTarget();
      if (target != null) {
         this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
         if (this.cooldown > 0) {
            this.cooldown--;
         }

         if (this.bargeCooldown > 0) {
            this.bargeCooldown--;
         }

         if (this.mob.isActing()) {
            if (this.mob.currentAction() != 3) {
               this.mob.getNavigation().stop();
            }
         } else {
            if (--this.repath <= 0) {
               this.repath = 4 + this.mob.getRandom().nextInt(5);
               Vec3 goal = this.flank(target);
               this.mob.getNavigation().moveTo(goal.x, goal.y, goal.z, this.speed);
            }

            if (this.mob.armedTicks() >= PirateClips.LENGTH[0] && this.cooldown <= 0) {
               if (this.mob.isWithinMeleeAttackRange(target) && this.mob.getSensing().hasLineOfSight(target)) {
                  if (this.barges && this.bargeCooldown <= 0 && this.mob.getRandom().nextFloat() < 0.3F) {
                     this.mob.playAction(3, PirateClips.LENGTH[3]);
                     this.bargeCooldown = (Integer)PirateConfig.DECKHAND_BARGE_COOLDOWN.get();
                     this.cooldown = PirateClips.LENGTH[3] + 8;
                  } else {
                     this.mob.playAction(1, PirateClips.LENGTH[1]);
                     this.cooldown = PirateClips.LENGTH[1] + PirateClips.LENGTH[2] + 6;
                  }
               } else if (this.barges
                  && this.bargeCooldown <= 0
                  && this.mob.distanceToSqr(target) < 10.240000000000002
                  && this.mob.getRandom().nextFloat() < 0.08F) {
                  this.mob.playAction(3, PirateClips.LENGTH[3]);
                  this.bargeCooldown = (Integer)PirateConfig.DECKHAND_BARGE_COOLDOWN.get();
                  this.cooldown = PirateClips.LENGTH[3] + 8;
               }
            }
         }
      }
   }

   private Vec3 flank(LivingEntity target) {
      Vec3 t = target.position();
      int busy = this.mob.level().getEntitiesOfClass(FactionFighter.class, new AABB(t, t).inflate(2.5), f -> f != this.mob && f.isAlliedTo(this.mob)).size();
      if (busy != 0 && !(this.mob.distanceToSqr(target) < 6.25)) {
         Vec3 from = this.mob.position().subtract(t).multiply(1.0, 0.0, 1.0);
         if (from.lengthSqr() < 1.0E-4) {
            return t;
         } else {
            Vec3 dir = from.normalize();
            double sign = (this.mob.getId() & 1) == 0 ? 1.0 : -1.0;
            Vec3 side = new Vec3(-dir.z * sign, 0.0, dir.x * sign);
            return t.add(side.scale(1.6)).add(dir.scale(0.6));
         }
      } else {
         return t;
      }
   }
}
