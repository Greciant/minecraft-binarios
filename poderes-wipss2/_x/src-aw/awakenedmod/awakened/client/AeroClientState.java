package com.awakenedmod.awakened.client;

public final class AeroClientState {
   private static boolean armed;
   private static boolean flying;
   private static boolean gliding;
   private static boolean forcedGliding;

   private AeroClientState() {
   }

   public static boolean armed() {
      return armed;
   }

   public static boolean flying() {
      return flying;
   }

   public static boolean gliding() {
      return gliding;
   }

   public static boolean forcedGliding() {
      return forcedGliding;
   }

   public static void set(boolean armedNow, boolean flyingNow, boolean glidingNow, boolean forcedNow) {
      armed = armedNow;
      flying = flyingNow;
      gliding = glidingNow;
      forcedGliding = forcedNow;
   }

   public static void reset() {
      armed = false;
      flying = false;
      gliding = false;
      forcedGliding = false;
   }
}
