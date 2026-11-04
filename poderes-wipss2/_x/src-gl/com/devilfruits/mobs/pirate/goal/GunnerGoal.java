package com.devilfruits.mobs.pirate.goal;

import com.devilfruits.mobs.pirate.PirateClips;
import com.devilfruits.mobs.pirate.PirateConfig;
import com.devilfruits.mobs.pirate.entity.PirateGunner;
import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;

public class GunnerGoal extends Goal {
   private final PirateGunner mob;
   private int aim;
   private int strafeTime;
   private boolean strafeLeft;
   private int repath;
   private int hopCooldown;

   public GunnerGoal(PirateGunner mob) {
      this.mob = mob;
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
      this.aim = 0;
   }

   public void stop() {
      this.mob.setAggressive(false);
      this.mob.setFlag(2, false);
      this.mob.getMoveControl().strafe(0.0F, 0.0F);
      this.mob.getNavigation().stop();
   }

   public boolean requiresUpdateEveryTick() {
      return true;
   }

   public void tick() {
      LivingEntity target = this.mob.getTarget();
      if (target != null) {
         this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
         boolean musket = this.mob.hasMusket();
         double dist = (double)this.mob.distanceTo(target);
         boolean sight = this.mob.getSensing().hasLineOfSight(target);
         double min = (Double)PirateConfig.GUNNER_MIN_RANGE.get();
         double max = Math.max(min + 2.0, (Double)PirateConfig.GUNNER_MAX_RANGE.get());
         if (this.hopCooldown > 0) {
            this.hopCooldown--;
         }

         if (this.mob.isActing()) {
            int a = this.mob.currentAction();
            if (a == 6 || a == 8 || a == 5 || a == 7) {
               this.mob.getNavigation().stop();
               this.mob.getMoveControl().strafe(dist < min ? -0.25F : 0.0F, 0.0F);
               this.mob.faceTarget(target, 20.0F);
            }

            this.mob.setFlag(2, false);
         } else if (this.mob.armedTicks() >= PirateClips.LENGTH[0]) {
            if (!this.mob.hasFlag(8)) {
               this.mob.setFlag(2, false);
               this.aim = 0;
               int reload = musket ? 8 : 6;
               this.mob.playAction(reload, PirateClips.LENGTH[reload]);
            } else if (dist < 3.0 && this.hopCooldown <= 0) {
               this.mob.playAction(9, PirateClips.LENGTH[9]);
               this.hopCooldown = 60;
            } else {
               if (!(dist > max) && sight) {
                  this.mob.getNavigation().stop();
                  this.mob.faceTarget(target, 25.0F);
                  if (--this.strafeTime <= 0) {
                     this.strafeTime = 30 + this.mob.getRandom().nextInt(40);
                     this.strafeLeft = this.mob.getRandom().nextBoolean();
                  }

                  float back = dist < min ? -0.5F : (dist > max - 2.0 ? 0.3F : 0.0F);
                  this.mob.getMoveControl().strafe(back, this.strafeLeft ? 0.45F : -0.45F);
               } else {
                  if (--this.repath <= 0) {
                     this.repath = 10;
                     this.mob.getNavigation().moveTo(target, 1.0);
                  }

                  this.mob.getMoveControl().strafe(0.0F, 0.0F);
               }

               boolean canShoot = sight && dist <= max + 4.0;
               this.mob.setFlag(2, canShoot);
               if (!canShoot) {
                  this.aim = 0;
               } else {
                  int need = (Integer)PirateConfig.GUNNER_AIM_TICKS.get() * (musket ? 2 : 1);
                  if (++this.aim >= need && this.mob.clearShot(target)) {
                     this.aim = 0;
                     int fire = musket ? 7 : 5;
                     this.mob.playAction(fire, PirateClips.LENGTH[fire]);
                  }
               }
            }
         }
      }
   }
}
