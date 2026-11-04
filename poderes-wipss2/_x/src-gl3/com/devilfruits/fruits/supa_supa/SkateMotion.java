package com.devilfruits.fruits.supa_supa;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class SkateMotion {
   public static Vec3 step(Vec3 v, double forward, double strafe, float yaw, boolean onGround, boolean braking, SkateMotion.Params p) {
      double r = Math.toRadians((double)yaw);
      double fx = -Math.sin(r);
      double fz = Math.cos(r);
      double sz = -fx;
      double vx = v.x;
      double vz = v.z;
      double speed = Math.sqrt(vx * vx + vz * vz);
      if (!onGround) {
         vx = vx * 0.99 + fx * p.acceleration() * 0.1 * Math.max(0.0, forward);
         vz = vz * 0.99 + fz * p.acceleration() * 0.1 * Math.max(0.0, forward);
         return cap(vx, vz, p.maxSpeed());
      } else {
         double ix = fx * Math.max(0.35, forward) + fz * strafe * 0.8;
         double iz = fz * Math.max(0.35, forward) + sz * strafe * 0.8;
         double il = Math.sqrt(ix * ix + iz * iz);
         if (speed > 1.0E-4 && il > 1.0E-4) {
            double a0 = Math.atan2(vz, vx);
            double a1 = Math.atan2(iz / il, ix / il);
            double d = Mth.wrapDegrees(Math.toDegrees(a1 - a0));
            double turn = Math.toRadians(Mth.clamp(d, -Math.toDegrees(p.turnRate()), Math.toDegrees(p.turnRate())));
            double a = a0 + turn;
            double keep = 1.0 - 0.06 * Math.abs(turn);
            vx = Math.cos(a) * speed * keep;
            vz = Math.sin(a) * speed * keep;
         }

         if (forward > 0.0) {
            vx += fx * p.acceleration() * forward;
            vz += fz * p.acceleration() * forward;
         }

         if (strafe != 0.0) {
            vx += fz * p.acceleration() * 0.3 * strafe;
            vz += sz * p.acceleration() * 0.3 * strafe;
         }

         double k = !braking && !(forward < 0.0) ? p.glide() : p.brake();
         return cap(vx * k, vz * k, p.maxSpeed());
      }
   }

   private static Vec3 cap(double vx, double vz, double max) {
      double s = Math.sqrt(vx * vx + vz * vz);
      if (s > max) {
         vx *= max / s;
         vz *= max / s;
      }

      return new Vec3(vx, 0.0, vz);
   }

   public static boolean tooFast(double travel, double maxSpeed) {
      return travel > maxSpeed * 1.25 + 0.15;
   }

   private SkateMotion() {
   }

   public static record Params(double maxSpeed, double acceleration, double glide, double brake, double turnRate) {
   }
}
