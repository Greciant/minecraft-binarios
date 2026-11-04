package com.awakenedmod.awakened.core.npc;

import com.awakenedmod.awakened.content.entity.ThreatMoves;
import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

public final class NpcFootwork {
   private static final int REPATH_TICKS = 10;
   private static final double SERPENTINE_AMP_MIN = 1.5;
   private static final double SERPENTINE_AMP_MAX = 2.5;
   private static final double SERPENTINE_STRIDE = 8.0;
   private static final int BACKSTEP_TICKS = 15;
   private static final double BACKSTEP_REACH = 4.0;
   private static final int FLIP_MIN = 120;
   private static final int FLIP_SPAN = 120;
   private static final int JUKE_FLIP_MIN = 10;
   private static final int JUKE_FLIP_SPAN = 20;
   private static final float LOOK_SPEED = 30.0F;
   private static final int CIRCLE_REPATH = 5;
   private static final int CIRCLE_LEAD = 8;
   private static final int NO_PATH_BACKOFF = 20;
   private static final int PARTIAL_PATH_BACKOFF = 40;
   private static final double[] BACKSTEP_TURNS = new double[]{0.0, 40.0, -40.0, 80.0, -80.0, 120.0, -120.0};
   private final Mob mob;
   private NpcFootwork.Mode mode = NpcFootwork.Mode.STILL;
   private int repathAt;
   private boolean serpentineRight;
   private int retreatUntil;
   private boolean clockwise;
   private int flipAt;
   private int frame;
   private double phase;
   private int lastCircleTick = -1000;
   private int circleRepathAt;
   private int noPathUntil;
   private boolean juke;

   public NpcFootwork(Mob mob) {
      this.mob = mob;
   }

   public NpcFootwork.Mode mode() {
      return this.mode;
   }

   public void keepBand(LivingEntity target, double min, double max, double speed) {
      this.keepBand(target, min, max, speed, false);
   }

   public void keepBand(LivingEntity target, double min, double max, double speed, boolean juke) {
      this.juke = juke;
      this.face(target);
      if (speed <= 0.0) {
         this.mob.getNavigation().stop();
         this.mode = NpcFootwork.Mode.STILL;
      } else {
         double dist = (double)this.mob.distanceTo(target);
         if (this.mob.tickCount >= this.retreatUntil && !(dist < min)) {
            if (dist > max) {
               this.approach(target, (min + max) * 0.5, speed);
            } else {
               this.circle(target, (min + max) * 0.5, speed);
            }
         } else {
            if (this.mob.tickCount >= this.retreatUntil) {
               this.retreatUntil = this.mob.tickCount + 15;
            }

            this.retreat(target, speed);
         }
      }
   }

   public static void wadeTo(Mob mob, Vec3 land, double speed) {
      mob.getNavigation().stop();
      mob.getMoveControl().setWantedPosition(land.x, land.y, land.z, speed);
      if (mob.horizontalCollision && mob.onGround() || mob.horizontalCollision && mob.isInWater()) {
         mob.getJumpControl().jump();
      }
   }

   public void still() {
      this.mob.getNavigation().stop();
      this.mode = NpcFootwork.Mode.STILL;
   }

   public void face(LivingEntity target) {
      this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
   }

   public void approach(LivingEntity target, double orbit, double speed) {
      this.mode = NpcFootwork.Mode.APPROACH;
      if (this.mob.tickCount >= this.noPathUntil && (this.mob.tickCount >= this.repathAt || this.mob.getNavigation().isDone())) {
         this.repathAt = this.mob.tickCount + 10;
         double dist = (double)this.mob.distanceTo(target);
         if (dist <= orbit + 1.0) {
            this.path(this.mob.getNavigation().moveTo(target, speed));
         } else {
            Vec3 dir = new Vec3(target.getX() - this.mob.getX(), 0.0, target.getZ() - this.mob.getZ()).normalize();
            this.serpentineRight = !this.serpentineRight;
            double amp = 1.5 + this.mob.getRandom().nextDouble() * 1.0;
            Vec3 perp = new Vec3(-dir.z, 0.0, dir.x).scale(this.serpentineRight ? amp : -amp);
            Vec3 dest = this.mob.position().add(dir.scale(Math.min(8.0, dist - orbit))).add(perp);
            this.path(this.mob.getNavigation().moveTo(dest.x, dest.y, dest.z, speed));
         }
      }
   }

   public void retreat(LivingEntity target, double speed) {
      this.retreat(target.position(), target.getEyePosition(), speed);
   }

   public void retreatFrom(Vec3 from, double speed) {
      this.retreat(from, null, speed);
   }

   private void retreat(Vec3 from, @Nullable Vec3 keepSight, double speed) {
      this.mode = NpcFootwork.Mode.RETREAT;
      Vec3 away = new Vec3(this.mob.getX() - from.x, 0.0, this.mob.getZ() - from.z);
      away = away.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0.0F, this.mob.getYRot() + 180.0F) : away.normalize();
      if (this.mob.tickCount >= this.repathAt) {
         this.repathAt = this.mob.tickCount + 10;
         Vec3 back = this.mob.position().add(away.scale(4.0));

         for (double turn : BACKSTEP_TURNS) {
            Vec3 p = this.mob.position().add(away.yRot((float)Math.toRadians(turn)).scale(4.0));
            Vec3 stand = NpcNav.standInColumn(this.mob.level(), p, 1, 2);
            if (stand != null
               && (keepSight == null || NpcNav.clearLine(this.mob.level(), this.mob, stand.add(0.0, (double)this.mob.getEyeHeight(), 0.0), keepSight))) {
               back = stand;
               break;
            }
         }

         this.path(this.mob.getNavigation().moveTo(back.x, back.y, back.z, speed));
      }
   }

   public void walkTo(Vec3 dest, double speed) {
      this.mode = NpcFootwork.Mode.WALK;
      if (this.mob.tickCount >= this.noPathUntil && (this.mob.tickCount >= this.repathAt || this.mob.getNavigation().isDone())) {
         this.repathAt = this.mob.tickCount + 10;
         this.path(this.mob.getNavigation().moveTo(dest.x, dest.y, dest.z, speed));
      }
   }

   private void path(boolean found) {
      if (!found) {
         this.repathAt = this.mob.tickCount + 20;
         this.noPathUntil = this.repathAt;
      } else if (this.mob.getNavigation().getPath() != null && !this.mob.getNavigation().getPath().canReach()) {
         this.noPathUntil = this.mob.tickCount + 40;
      }
   }

   public void flip() {
      this.clockwise = !this.clockwise;
      this.flipAt = this.mob.tickCount + 10 + this.mob.getRandom().nextInt(20);
      this.lastCircleTick = -1000;
      this.circleRepathAt = 0;
   }

   public void repathNow() {
      this.repathAt = 0;
      this.noPathUntil = 0;
      this.retreatUntil = 0;
   }

   public void circle(LivingEntity target, double radius, double speed) {
      this.mode = NpcFootwork.Mode.CIRCLE;
      if (this.mob.onGround()) {
         if (this.mob.tickCount >= this.flipAt) {
            boolean was = this.clockwise;
            this.clockwise = this.juke ? this.mob.getRandom().nextInt(4) == 0 == this.clockwise : this.mob.getRandom().nextBoolean();
            this.flipAt = this.mob.tickCount + (this.juke ? 10 + this.mob.getRandom().nextInt(20) : 120 + this.mob.getRandom().nextInt(120));
            if (was != this.clockwise) {
               this.lastCircleTick = -1000;
               this.circleRepathAt = 0;
            }
         } else if (this.juke && this.flipAt - this.mob.tickCount > 30) {
            this.flipAt = this.mob.tickCount + 10 + this.mob.getRandom().nextInt(20);
         }

         if (this.mob.tickCount - this.lastCircleTick > 1) {
            this.phase = Math.atan2(this.mob.getZ() - target.getZ(), this.mob.getX() - target.getX());
            this.frame = 0;
         }

         this.lastCircleTick = this.mob.tickCount;
         this.frame++;
         if (this.mob.tickCount >= this.circleRepathAt || this.mob.getNavigation().isDone()) {
            ThreatMoves.circleAround(this.mob, target, radius, 1.0, this.clockwise, this.frame + 8, this.phase, speed);
            Path p = this.mob.getNavigation().getPath();
            this.circleRepathAt = this.mob.tickCount + (p == null ? 20 : (!p.canReach() ? 40 : 5));
         }
      }
   }

   public static enum Mode {
      STILL,
      APPROACH,
      RETREAT,
      CIRCLE,
      WALK;
   }
}
