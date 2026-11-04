package com.devilfruits.client.vfx;

public final class VfxClock {
   private static long ticks;
   private static int hitStop;
   private static float partial;
   private static float heldPartial;
   private static boolean advanced;
   private static int slowTicks;
   private static float slowFactor = 1.0F;
   private static float slowAccumulator;

   static boolean tick(boolean running) {
      advanced = false;
      if (!running) {
         return false;
      } else if (hitStop > 0) {
         hitStop--;
         return false;
      } else {
         if (slowTicks > 0) {
            slowTicks--;
            slowAccumulator = slowAccumulator + slowFactor;
            if (slowAccumulator < 1.0F) {
               return false;
            }

            slowAccumulator--;
         }

         ticks++;
         advanced = true;
         return true;
      }
   }

   static void frame(float levelPartial) {
      partial = hitStop > 0 ? heldPartial : (slowTicks > 0 ? Math.min(0.999F, slowAccumulator + levelPartial * slowFactor) : levelPartial);
   }

   public static void hitStop(int ticks) {
      if (hitStop == 0) {
         heldPartial = partial;
      }

      hitStop = Math.max(hitStop, Math.min(3, ticks));
   }

   public static void slowMo(int clientTicks, float factor) {
      slowTicks = Math.max(slowTicks, Math.min(40, clientTicks));
      slowFactor = Math.max(0.25F, Math.min(1.0F, factor));
   }

   public static boolean frozen() {
      return hitStop > 0;
   }

   public static boolean advanced() {
      return advanced;
   }

   public static long ticks() {
      return ticks;
   }

   public static float partial() {
      return partial;
   }

   public static float time() {
      return (float)(ticks & 1048575L) + partial;
   }

   static void reset() {
      hitStop = 0;
      slowTicks = 0;
      slowAccumulator = 0.0F;
      heldPartial = 0.0F;
      partial = 0.0F;
   }

   private VfxClock() {
   }
}
