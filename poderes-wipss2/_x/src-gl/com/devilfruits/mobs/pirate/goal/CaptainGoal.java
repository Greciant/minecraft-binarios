package com.devilfruits.mobs.pirate.goal;

import com.devilfruits.mobs.pirate.PirateClips;
import com.devilfruits.mobs.pirate.PirateConfig;
import com.devilfruits.mobs.pirate.entity.PirateCaptain;
import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;

public class CaptainGoal extends Goal {
   private final PirateCaptain mob;
   private float slam;
   private float lunge;
   private float pistol;
   private float rally;
   private float swing;
   private int repath;

   public CaptainGoal(PirateCaptain mob) {
      this.mob = mob;
      this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
   }

   public float[] cooldowns() {
      return new float[]{this.slam, this.lunge, this.pistol, this.rally, this.swing};
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
      this.rally = Math.max(this.rally, 30.0F);
      this.slam = Math.max(this.slam, 40.0F);
      this.repath = 0;
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
         float step = this.mob.enraged() ? ((Double)PirateConfig.CAPTAIN_ENRAGE_COOLDOWN_FACTOR.get()).floatValue() : 1.0F;
         this.slam -= step;
         this.lunge -= step;
         this.pistol -= step;
         this.rally -= step;
         this.swing -= step;
         if (this.mob.isActing()) {
            if (this.mob.currentAction() != 12) {
               this.mob.getNavigation().stop();
               this.mob.faceTarget(target, 18.0F);
            }
         } else {
            double dist = (double)this.mob.distanceTo(target);
            boolean sight = this.mob.getSensing().hasLineOfSight(target);
            if (this.mob.armedTicks() >= PirateClips.LENGTH[0]) {
               if (this.rally <= 0.0F && this.mob.crewNearby((Double)PirateConfig.RALLY_RADIUS.get()) > 0) {
                  this.act(14);
                  this.rally = (float)((Integer)PirateConfig.CAPTAIN_RALLY_COOLDOWN.get()).intValue();
                  return;
               }

               if (this.slam <= 0.0F && dist < 3.6 * (double)this.mob.getScale() / 1.35) {
                  this.act(11);
                  this.slam = (float)((Integer)PirateConfig.CAPTAIN_SLAM_COOLDOWN.get()).intValue();
                  return;
               }

               if (this.lunge <= 0.0F && dist > 4.5 && dist < 9.0 && sight) {
                  this.act(12);
                  this.lunge = (float)((Integer)PirateConfig.CAPTAIN_LUNGE_COOLDOWN.get()).intValue();
                  return;
               }

               if (this.pistol <= 0.0F && dist > 5.5 && dist < 20.0 && sight && this.mob.clearShot(target)) {
                  this.act(13);
                  this.pistol = (float)((Integer)PirateConfig.CAPTAIN_PISTOL_COOLDOWN.get()).intValue();
                  return;
               }

               if (this.swing <= 0.0F && this.mob.isWithinMeleeAttackRange(target) && sight) {
                  this.act(10);
                  this.swing = (float)(PirateClips.LENGTH[10] + 10);
                  return;
               }
            }

            if (--this.repath <= 0) {
               this.repath = 6;
               this.mob.getNavigation().moveTo(target, this.mob.enraged() ? 1.15 : 1.0);
            }
         }
      }
   }

   private void act(int id) {
      this.mob.getNavigation().stop();
      this.mob.playAction(id, PirateClips.LENGTH[id]);
   }
}
