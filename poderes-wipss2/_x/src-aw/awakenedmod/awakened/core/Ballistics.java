package com.awakenedmod.awakened.core;

import net.minecraft.world.phys.Vec3;

public final class Ballistics {
   public static final double AIR_DRAG = 0.99;
   private static final double MOVE_LEAD = 1.0;

   private Ballistics() {
   }

   public static Vec3 arc(Vec3 from, Vec3 to, int flightTicks, double gravity) {
      double travel = travel(flightTicks);
      if (travel < 1.0E-6) {
         return Vec3.ZERO;
      } else {
         Vec3 delta = to.subtract(from);
         double lift = (delta.y + gravity * drop(flightTicks)) / travel;
         return new Vec3(delta.x / travel, lift, delta.z / travel);
      }
   }

   public static double travel(int n) {
      return 1.0 * (1.0 - Math.pow(0.99, (double)n)) / 0.010000000000000009;
   }

   public static double drop(int n) {
      return 99.99999999999991 * ((double)n - travel(n));
   }
}
