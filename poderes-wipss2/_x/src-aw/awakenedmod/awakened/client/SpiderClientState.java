package com.awakenedmod.awakened.client;

import net.minecraft.world.phys.Vec3;

public final class SpiderClientState {
   private static boolean swinging;
   private static boolean hanging;
   private static boolean zipping;
   private static boolean leftArm;
   private static Vec3 anchor = Vec3.ZERO;
   private static String loadedType = "splat";
   private static boolean hasGauntlets;

   private SpiderClientState() {
   }

   public static void setLoadedType(String type, boolean gauntlets) {
      loadedType = type;
      hasGauntlets = gauntlets;
   }

   public static String loadedType() {
      return loadedType;
   }

   public static boolean hasGauntlets() {
      return hasGauntlets;
   }

   public static void set(boolean swingingNow, boolean hangingNow, double ax, double ay, double az, boolean leftArmNow) {
      swinging = swingingNow;
      hanging = hangingNow;
      leftArm = leftArmNow;
      anchor = new Vec3(ax, ay, az);
   }

   public static void setZip(boolean zippingNow, double tx, double ty, double tz, boolean leftArmNow) {
      zipping = zippingNow;
      leftArm = leftArmNow;
      anchor = new Vec3(tx, ty, tz);
   }

   public static boolean swinging() {
      return swinging;
   }

   public static boolean zipping() {
      return zipping;
   }

   public static boolean hanging() {
      return hanging;
   }

   public static boolean leftArm() {
      return leftArm;
   }

   public static Vec3 anchor() {
      return anchor;
   }

   public static void reset() {
      swinging = false;
      hanging = false;
      zipping = false;
      leftArm = false;
      anchor = Vec3.ZERO;
   }
}
