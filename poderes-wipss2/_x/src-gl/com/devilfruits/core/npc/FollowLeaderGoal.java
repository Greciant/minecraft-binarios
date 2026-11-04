package com.devilfruits.core.npc;

import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.phys.Vec3;

public class FollowLeaderGoal extends Goal {
   private final FactionFighter mob;
   private final double speed;
   private int repath;

   public FollowLeaderGoal(FactionFighter mob, double speed) {
      this.mob = mob;
      this.speed = speed;
      this.setFlags(EnumSet.of(Flag.MOVE));
   }

   @Nullable
   private Vec3 slot() {
      FactionFighter leader = this.mob.leader();
      if (leader != null && leader != this.mob) {
         float yaw = leader.getYRot() * (float) (Math.PI / 180.0);
         Vec3 forward = new Vec3((double)(-Mth.sin(yaw)), 0.0, (double)Mth.cos(yaw));
         Vec3 side = new Vec3(forward.z, 0.0, -forward.x);
         int n = Math.floorMod(this.mob.getId(), 4);
         double lateral = ((double)n - 1.5) * 1.8;
         double ahead = this.mob.rank() == 0 ? 1.5 : -3.5;
         return leader.position().add(forward.scale(ahead)).add(side.scale(lateral));
      } else {
         return null;
      }
   }

   public boolean canUse() {
      if (this.mob.getTarget() != null) {
         return false;
      } else {
         Vec3 slot = this.slot();
         return slot != null && this.mob.position().distanceToSqr(slot) > 4.0;
      }
   }

   public boolean canContinueToUse() {
      if (this.mob.getTarget() != null) {
         return false;
      } else {
         Vec3 slot = this.slot();
         return slot != null && this.mob.position().distanceToSqr(slot) > 1.0;
      }
   }

   public void start() {
      this.repath = 0;
   }

   public void tick() {
      if (--this.repath <= 0) {
         this.repath = 10;
         Vec3 slot = this.slot();
         if (slot != null) {
            FactionFighter leader = this.mob.leader();
            double far = leader != null ? this.mob.distanceToSqr(leader) : 0.0;
            this.mob.getNavigation().moveTo(slot.x, slot.y, slot.z, far > 144.0 ? this.speed * 1.25 : this.speed);
         }
      }
   }

   public void stop() {
      this.mob.getNavigation().stop();
   }
}
