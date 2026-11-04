package com.awakenedmod.awakened.client.render.limitless;

public final class Ease {
   private Ease() {
   }

   public static float clamp01(float x) {
      return x < 0.0F ? 0.0F : Math.min(1.0F, x);
   }

   public static float smooth(float x) {
      x = clamp01(x);
      return x * x * (3.0F - 2.0F * x);
   }

   public static float ramp(float t, float from, float ticks) {
      return smooth((t - from) / Math.max(1.0E-4F, ticks));
   }

   public static float envelope(float t, float rise, float end, float fall) {
      return Math.min(ramp(t, 0.0F, rise), 1.0F - ramp(t, end - fall, fall));
   }

   public static float out(float x, float power) {
      return 1.0F - (float)Math.pow((double)(1.0F - clamp01(x)), (double)power);
   }

   public static float frac(float x) {
      return x - (float)Math.floor((double)x);
   }
}
