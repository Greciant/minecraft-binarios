package com.awakenedmod.awakened.content.entity.ai;

import com.awakenedmod.awakened.content.entity.StalkerEntity;
import com.awakenedmod.awakened.core.Fx;
import java.util.EnumSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.phys.Vec3;

public class StalkerPounceGoal extends Goal {
   private static final int WINDUP_TICKS = 8;
   private static final int COOLDOWN_TICKS = 50;
   private static final int MAX_LEAP_TICKS = 18;
   private static final double MIN_RANGE = 2.5;
   private static final double MAX_RANGE = 6.0;
   private final StalkerEntity mob;
   private int cooldown;
   private int windup;
   private int leapTicks;
   private boolean leaping;
   private boolean leftGround;
   private LivingEntity target;

   public StalkerPounceGoal(StalkerEntity mob) {
      this.mob = mob;
      this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
   }

   public boolean canUse() {
      if (this.cooldown > 0) {
         this.cooldown--;
         return false;
      } else {
         LivingEntity t = this.mob.getTarget();
         if (t != null && t.isAlive() && this.mob.onGround()) {
            double d2 = this.mob.distanceToSqr(t);
            if (!(d2 < 6.25) && !(d2 > 36.0)) {
               if (!this.mob.getSensing().hasLineOfSight(t)) {
                  return false;
               } else {
                  this.target = t;
                  return true;
               }
            } else {
               return false;
            }
         } else {
            return false;
         }
      }
   }

   public boolean canContinueToUse() {
      return this.target != null && this.target.isAlive() && (this.windup > 0 || this.leaping);
   }

   public void start() {
      this.windup = 8;
      this.leaping = false;
      this.leftGround = false;
      this.leapTicks = 0;
      this.mob.getNavigation().stop();
      if (this.mob.level() instanceof ServerLevel server) {
         Fx.burst(server, Fx.OPTIC_RED, this.mob.position().add(0.0, (double)this.mob.getBbHeight() * 0.9, 0.0), 8, 0.2, 0.01);
         Fx.sound(server, this.mob.position(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 0.6F, 0.5F);
      }
   }

   public void stop() {
      this.cooldown = 50;
      this.leaping = false;
      this.target = null;
   }

   public void tick() {
      if (this.target != null) {
         this.mob.getLookControl().setLookAt(this.target, 30.0F, 30.0F);
         if (this.windup > 0) {
            Vec3 v = this.mob.getDeltaMovement();
            this.mob.setDeltaMovement(v.x * 0.4, v.y, v.z * 0.4);
            if (--this.windup == 0) {
               this.launch();
            }
         } else {
            if (this.leaping) {
               this.leapTicks++;
               if (!this.mob.onGround()) {
                  this.leftGround = true;
               }

               double reach = (double)this.mob.getBbWidth() * 0.5 + (double)this.target.getBbWidth() * 0.5 + 1.0;
               if (this.mob.distanceToSqr(this.target) <= reach * reach) {
                  this.bite();
                  this.leaping = false;
               } else if (this.leftGround && this.mob.onGround() || this.leapTicks >= 18) {
                  this.leaping = false;
               }
            }
         }
      }
   }

   private void launch() {
      Vec3 dir = this.target.position().subtract(this.mob.position());
      dir = new Vec3(dir.x, 0.0, dir.z);
      if (dir.lengthSqr() < 1.0E-4) {
         dir = this.mob.getLookAngle();
      }

      dir = dir.normalize();
      this.mob.setDeltaMovement(dir.x * 0.9, 0.42, dir.z * 0.9);
      this.mob.hurtMarked = true;
      this.leaping = true;
      if (this.mob.level() instanceof ServerLevel server) {
         Fx.sound(server, this.mob.position(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 0.8F, 0.8F);
      }
   }

   private void bite() {
      this.mob.swing(InteractionHand.MAIN_HAND);
      if (this.mob.level() instanceof ServerLevel server) {
         this.mob.doHurtTarget(this.target);
      }

      Vec3 v = this.mob.getDeltaMovement();
      this.mob.setDeltaMovement(v.x * 0.2, v.y, v.z * 0.2);
   }
}
