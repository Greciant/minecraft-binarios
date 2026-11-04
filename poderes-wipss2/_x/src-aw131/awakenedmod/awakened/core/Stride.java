package com.awakenedmod.awakened.core;

public final class Stride {
   private static final float MIN_CAP = 1.5F;
   private static final float MAX_CAP = 4.0F;
   private static final float CAP_HEADROOM = 2.5F;
   private static final float RUN_FRACTION = 0.6F;

   private Stride() {
   }

   public static float cap(double expectedSprintStride) {
      return (float)Math.min(4.0, Math.max(1.5, 2.5 * expectedSprintStride));
   }

   public static boolean isRun(float step, double expectedSprintStride) {
      return (double)step >= 0.6F * expectedSprintStride;
   }
}
