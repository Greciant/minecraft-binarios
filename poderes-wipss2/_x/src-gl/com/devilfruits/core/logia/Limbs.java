package com.devilfruits.core.logia;

public final class Limbs {
   public static final int HEAD = 1;
   public static final int BODY = 2;
   public static final int RIGHT_ARM = 4;
   public static final int LEFT_ARM = 8;
   public static final int RIGHT_LEG = 16;
   public static final int LEFT_LEG = 32;
   public static final int ARMS = 12;
   public static final int LEGS = 48;
   public static final int ALL = 63;
   public static final int COUNT = 6;

   public static boolean has(int mask, int limb) {
      return (mask & limb) != 0;
   }

   private Limbs() {
   }
}
