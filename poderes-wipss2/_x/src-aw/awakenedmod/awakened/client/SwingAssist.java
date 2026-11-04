package com.awakenedmod.awakened.client;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

final class SwingAssist {
   private static final double[] FAN_YAW_DEG = new double[]{-24.0, -12.0, 0.0, 12.0, 24.0};
   private static final double[] FAN_PITCH_DEG = new double[]{30.0, 42.0, 55.0, 68.0};
   private static final double IDEAL_ELEVATION_DEG = 45.0;
   private static final double IDEAL_GROUNDED_ELEVATION_DEG = 60.0;
   private static final double IDEAL_DISTANCE = 18.0;
   private static final double IDEAL_DISTANCE_PER_SPEED = 7.0;
   private static final double IDEAL_DISTANCE_MAX = 30.0;
   private static final double MIN_AHEAD = 2.0;
   private static final double MIN_AHEAD_PER_SPEED = 4.0;
   private static final double CROSSHAIR_BONUS = 0.6;
   private static final double SWEET_RISE = 0.5;
   private static final double FLOOR_CLEARANCE = 0.8;
   private static final double SHORTEN_PER_TICK = 0.2;
   private static final double PROBE_OFFSET = 1.5;
   private static final double BELOW_ANCHOR = 2.0;
   private static final double CATCH_MIN_FALL = 0.5;
   private static final double CATCH_KEEP = 0.9;
   private static final double CATCH_SINK = 0.2;
   private static final double DIVE_CAP_MAX = 1.3;

   private SwingAssist() {
   }

   static Vec3 bestAnchor(Level level, Player player, Vec3 motion, double maxRange, double maxRope, double minDy, boolean grounded) {
      Vec3 eye = player.getEyePosition();
      Vec3 look = player.getLookAngle();
      Vec3 heading = new Vec3(look.x, 0.0, look.z);
      if (heading.lengthSqr() < 1.0E-4) {
         Vec3 dm = player.getDeltaMovement();
         heading = new Vec3(dm.x, 0.0, dm.z);
      }

      if (heading.lengthSqr() < 1.0E-4) {
         heading = new Vec3(0.0, 0.0, 1.0);
      }

      heading = heading.normalize();
      double idealElevation = grounded ? 60.0 : 45.0;
      double hSpeed = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
      double idealDistance = Math.min(30.0, 18.0 + hSpeed * 7.0);
      double minAhead = grounded ? 0.0 : 2.0 + hSpeed * 4.0;
      Vec3 best = null;
      double bestScore = Double.NEGATIVE_INFINITY;
      Vec3 aimed = cast(level, player, eye, look, maxRange);
      if (aimed != null && qualifies(player, eye, heading, aimed, maxRope, minDy)) {
         best = aimed;
         bestScore = score(player, heading, aimed, idealElevation, idealDistance) + 0.6;
      }

      for (double yaw : FAN_YAW_DEG) {
         Vec3 across = rotateY(heading, Math.toRadians(yaw));

         for (double pitch : FAN_PITCH_DEG) {
            double p = Math.toRadians(pitch);
            Vec3 dir = across.scale(Math.cos(p)).add(0.0, Math.sin(p), 0.0);
            Vec3 hit = cast(level, player, eye, dir, maxRange);
            if (hit != null && qualifies(player, eye, heading, hit, maxRope, minDy) && !(ahead(player, heading, hit) < minAhead)) {
               double s = score(player, heading, hit, idealElevation, idealDistance) - Math.abs(yaw) / 60.0;
               if (s > bestScore) {
                  bestScore = s;
                  best = hit;
               }
            }
         }
      }

      return best;
   }

   static double releaseTiming(Vec3 vel) {
      double speed = vel.length();
      if (speed < 1.0E-4) {
         return 0.0;
      } else {
         double rise = vel.y / speed;
         return rise <= 0.0 ? 0.0 : Math.min(1.0, rise / 0.5);
      }
   }

   static double clearanceRope(Level level, Player player, Vec3 anchor, double ropeLen, double minRope) {
      if (anchor.y - player.getY() < 2.0) {
         return ropeLen;
      } else {
         Vec3 toPlayer = player.position().subtract(anchor);
         Vec3 flat = new Vec3(toPlayer.x, 0.0, toPlayer.z);
         Vec3 from = flat.lengthSqr() > 1.0E-4 ? anchor.add(flat.normalize().scale(1.5)) : anchor;
         Vec3 to = new Vec3(from.x, anchor.y - ropeLen - 2.0, from.z);
         BlockHitResult floor = level.clip(new ClipContext(from, to, Block.COLLIDER, Fluid.NONE, player));
         if (floor.getType() != Type.BLOCK) {
            return ropeLen;
         } else {
            double clear = anchor.y - floor.getLocation().y - 0.8;
            return clear >= ropeLen ? ropeLen : Math.max(minRope, Math.max(clear, ropeLen - 0.2));
         }
      }
   }

   static Vec3 catchFall(Vec3 vel, Vec3 look) {
      if (-vel.y < 0.5) {
         return vel;
      } else {
         Vec3 heading = new Vec3(look.x, 0.0, look.z);
         if (heading.lengthSqr() < 1.0E-4) {
            heading = new Vec3(vel.x, 0.0, vel.z);
         }

         if (heading.lengthSqr() < 1.0E-4) {
            return vel;
         } else {
            double speed = vel.length() * 0.9;
            return heading.normalize().scale(speed * Math.sqrt(0.96)).add(0.0, -speed * 0.2, 0.0);
         }
      }
   }

   static Vec3 catchOnRope(Vec3 vel, Vec3 pos, Vec3 anchor, Vec3 look) {
      Vec3 toAnchor = anchor.subtract(pos);
      double dist = toAnchor.length();
      double speed = vel.length();
      if (!(dist < 1.0E-4) && !(speed < 1.0E-4)) {
         Vec3 rope = toAnchor.scale(1.0 / dist);
         double inward = vel.dot(rope);
         if (inward >= 0.0) {
            return vel;
         } else {
            Vec3 tangent = vel.subtract(rope.scale(inward));
            if (tangent.lengthSqr() < 0.01 * speed * speed) {
               tangent = look.subtract(rope.scale(look.dot(rope)));
            }

            if (tangent.lengthSqr() < 1.0E-6) {
               return vel;
            } else {
               double away = -inward / speed;
               return tangent.normalize().scale(speed * (1.0 - 0.09999999999999998 * away));
            }
         }
      } else {
         return vel;
      }
   }

   static double diveCap(Vec3 vel, double maxSpeed) {
      return -vel.y < 0.5 ? 1.0 : Math.max(1.0, Math.min(1.3, vel.length() * 0.9 / maxSpeed));
   }

   private static Vec3 cast(Level level, Player player, Vec3 eye, Vec3 dir, double range) {
      BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(dir.scale(range)), Block.COLLIDER, Fluid.NONE, player));
      return hit.getType() == Type.BLOCK ? hit.getLocation() : null;
   }

   private static boolean qualifies(Player player, Vec3 eye, Vec3 heading, Vec3 hit, double maxRope, double minDy) {
      Vec3 offset = hit.subtract(player.position());
      return hit.y >= eye.y + minDy && offset.length() <= maxRope && ahead(player, heading, hit) >= -1.0;
   }

   private static double score(Player player, Vec3 heading, Vec3 hit, double idealElevationDeg, double idealDistance) {
      Vec3 offset = hit.subtract(player.position());
      double elevation = Math.toDegrees(Math.atan2(offset.y, Math.max(ahead(player, heading, hit), 0.001)));
      return -Math.abs(elevation - idealElevationDeg) / 30.0 - Math.abs(offset.length() - idealDistance) / 20.0;
   }

   private static double ahead(Player player, Vec3 heading, Vec3 hit) {
      Vec3 offset = hit.subtract(player.position());
      return offset.x * heading.x + offset.z * heading.z;
   }

   private static Vec3 rotateY(Vec3 v, double radians) {
      double c = Math.cos(radians);
      double s = Math.sin(radians);
      return new Vec3(v.x * c - v.z * s, v.y, v.x * s + v.z * c);
   }
}
