package com.devilfruits.core.haki;

public final class HakiParts {
   public static final int RIGHT_FIST = 1;
   public static final int LEFT_FIST = 2;
   public static final int RIGHT_ARM = 4;
   public static final int LEFT_ARM = 8;
   public static final int RIGHT_LEG = 16;
   public static final int LEFT_LEG = 32;
   public static final int TORSO = 64;
   public static final int HEAD = 128;
   public static final int WEAPON = 256;
   public static final int COUNT = 9;
   public static final int ALL = 511;
   public static final int FISTS = 3;
   public static final int ARMS = 12;
   public static final int LEGS = 48;
   public static final int BODY = 255;
   public static final String[] NAMES = new String[]{"right_fist", "left_fist", "right_arm", "left_arm", "right_leg", "left_leg", "torso", "head", "weapon"};
   private static final float[] WEIGHT = new float[]{0.05F, 0.05F, 0.1F, 0.1F, 0.12F, 0.12F, 0.32F, 0.14F, 0.0F};

   public static int allowed(HakiTier tier) {
      return switch (tier) {
         case NOVICE -> 259;
         case ADEPT -> 319;
         case EXPERT, MASTER -> 511;
      };
   }

   public static int defaults(HakiTier tier) {
      return switch (tier) {
         case NOVICE -> 259;
         case ADEPT -> 271;
         case EXPERT, MASTER -> 511;
      };
   }

   public static int normalize(int mask) {
      if ((mask & 4) != 0) {
         mask |= 1;
      }

      if ((mask & 8) != 0) {
         mask |= 2;
      }

      return mask & 511;
   }

   public static float coverage(int mask) {
      float sum = 0.0F;

      for (int i = 0; i < 9; i++) {
         if ((mask & 1 << i) != 0) {
            sum += WEIGHT[i];
         }
      }

      return Math.min(1.0F, sum);
   }

   public static int count(int mask) {
      return Integer.bitCount(mask & 511);
   }

   public static boolean fist(int mask, boolean right) {
      return (mask & (right ? 5 : 10)) != 0;
   }

   private HakiParts() {
   }
}
