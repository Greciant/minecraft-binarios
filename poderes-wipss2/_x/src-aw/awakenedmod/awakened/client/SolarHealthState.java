package com.awakenedmod.awakened.client;

public final class SolarHealthState {
   private static final long WINDOW_MS = 500L;
   private static volatile long benignUntil = 0L;

   private SolarHealthState() {
   }

   public static void markBenign() {
      benignUntil = System.currentTimeMillis() + 500L;
   }

   public static boolean consumeBenign() {
      if (System.currentTimeMillis() < benignUntil) {
         benignUntil = 0L;
         return true;
      } else {
         return false;
      }
   }
}
