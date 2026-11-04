package com.awakenedmod.awakened.content.summoning;

import com.awakenedmod.awakened.mixin.FlyingHoverAccessor;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class MinionFollowGoal extends Goal {
   private final Mob mob;
   private final double speed;
   private LivingEntity owner;
   private static final double HOLD_DIST = 2.0;
   private static final double FOLLOW_STOP = 5.0;
   private static final double FOLLOW_SPREAD = 3.0;
   private static final double FOLLOW_HYSTERESIS = 2.5;
   private static final double TELEPORT_DIST = 26.0;
   private static final double HOVER_LIFT = 0.75;
   private static final double HOVER_SPEED = 0.2;
   private boolean closing;

   public MinionFollowGoal(Mob mob, double speed) {
      this.mob = mob;
      this.speed = speed;
      this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
   }

   private boolean ridden() {
      return this.mob.getControllingPassenger() instanceof Player;
   }

   public boolean canUse() {
      if (this.hasLiveTarget() || this.ridden()) {
         return false;
      } else if (MinionSupport.isHolding(this.mob)) {
         return true;
      } else {
         LivingEntity o = MinionSupport.owner(this.mob);
         if (o != null && !o.isSpectator()) {
            this.owner = o;
            return true;
         } else {
            return false;
         }
      }
   }

   public boolean canContinueToUse() {
      if (this.hasLiveTarget() || this.ridden()) {
         return false;
      } else {
         return MinionSupport.isHolding(this.mob) ? true : this.owner != null && this.owner.isAlive() && !this.owner.isSpectator();
      }
   }

   public void start() {
      if (this.mob.getTarget() != null) {
         this.mob.setTarget(null);
      }
   }

   private boolean hasLiveTarget() {
      LivingEntity target = this.mob.getTarget();
      return target != null && target.isAlive();
   }

   public void stop() {
      this.owner = null;
      this.mob.getNavigation().stop();
   }

   public boolean requiresUpdateEveryTick() {
      return true;
   }

   public void tick() {
      Vec3 hold = MinionSupport.holdSpot(this.mob);
      if (hold != null) {
         if (this.mob.distanceToSqr(hold) > 4.0) {
            this.mob.getNavigation().moveTo(hold.x, hold.y, hold.z, this.speed);
         } else {
            this.park();
         }
      } else if (this.owner != null) {
         this.mob.getLookControl().setLookAt(this.owner, 10.0F, (float)this.mob.getMaxHeadXRot());
         double distSqr = this.mob.distanceToSqr(this.owner);
         if (distSqr >= 676.0) {
            this.mob.teleportTo(this.owner.getX(), this.owner.getY(), this.owner.getZ());
            this.park();
            this.closing = false;
         } else {
            double stop = 5.0 + (double)(this.mob.getId() % 5) / 4.0 * 3.0;
            double start = stop + 2.5;
            if (distSqr > start * start) {
               this.closing = true;
            } else if (distSqr < stop * stop) {
               this.closing = false;
            }

            if (this.closing) {
               this.mob.getNavigation().moveTo(this.owner, this.speed);
            } else {
               this.park();
            }
         }
      }
   }

   private void park() {
      this.mob.getNavigation().stop();
      if (this.mob.getMoveControl() instanceof FlyingMoveControl control && ((FlyingHoverAccessor)control).awakened$hoversInPlace()) {
         double x = this.mob.getX();
         double y = this.mob.getY();
         double z = this.mob.getZ();
         if (this.mob.onGround() && this.freeToRise()) {
            float yaw = this.mob.getYRot() * (float) (Math.PI / 180.0);
            x -= (double)Mth.sin(yaw) * 0.01;
            z += (double)Mth.cos(yaw) * 0.01;
            y += 0.75;
         }

         control.setWantedPosition(x, y, z, 0.2);
         return;
      }
   }

   private boolean freeToRise() {
      AABB risen = this.mob.getBoundingBox().move(0.0, 0.75, 0.0);
      return this.mob.level().noCollision(this.mob, risen)
         && this.mob.level().getFluidState(BlockPos.containing(this.mob.getX(), risen.minY, this.mob.getZ())).isEmpty();
   }
}
