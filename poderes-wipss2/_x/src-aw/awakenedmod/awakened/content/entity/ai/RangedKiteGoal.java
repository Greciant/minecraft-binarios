package com.awakenedmod.awakened.content.entity.ai;

import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.phys.Vec3;

public class RangedKiteGoal extends Goal {
   private final PathfinderMob mob;
   private final double minDistance;
   private final double speed;
   private int repathDelay;

   public RangedKiteGoal(PathfinderMob mob, double minDistance, double speed) {
      this.mob = mob;
      this.minDistance = minDistance;
      this.speed = speed;
      this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
   }

   public boolean canUse() {
      LivingEntity target = this.mob.getTarget();
      return target != null && target.isAlive() && this.mob.distanceToSqr(target) < this.minDistance * this.minDistance;
   }

   public boolean canContinueToUse() {
      return this.canUse();
   }

   public void start() {
      this.repathDelay = 0;
   }

   public void stop() {
      this.mob.getNavigation().stop();
   }

   public void tick() {
      LivingEntity target = this.mob.getTarget();
      if (target != null) {
         this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
         if (--this.repathDelay <= 0) {
            this.repathDelay = 8;
            Vec3 away = this.mob.position().subtract(target.position());
            if (away.lengthSqr() < 1.0E-4) {
               away = new Vec3(1.0, 0.0, 0.0);
            }

            Vec3 dest = this.mob.position().add(away.normalize().scale(8.0));
            this.mob.getNavigation().moveTo(dest.x, dest.y, dest.z, this.speed);
         }
      }
   }
}
