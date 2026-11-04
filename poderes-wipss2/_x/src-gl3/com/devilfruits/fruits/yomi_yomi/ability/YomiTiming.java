package com.devilfruits.fruits.yomi_yomi.ability;

public final class YomiTiming {
   public static final int AUBADE_HIT = 7;
   public static final int AUBADE_LENGTH = 16;
   public static final int GAVOTTE_TAKEOFF = 5;
   public static final int GAVOTTE_SWEEP = 16;
   public static final int GAVOTTE_MAX_AIR = 60;
   public static final int FUBUKI_DASH = 6;
   public static final int FUBUKI_DASH_TICKS = 4;
   public static final int FUBUKI_TURN = 14;
   public static final int FUBUKI_NOTO = 17;
   public static final int FUBUKI_CLICK = 27;
   public static final int FUBUKI_REDRAW = 35;
   public static final int FUBUKI_REDRAWN = 37;
   public static final int FUBUKI_LENGTH = 42;
   private static final float[] NOTO_T = new float[]{17.0F, 19.0F, 21.0F, 23.0F, 25.0F, 26.0F, 27.0F};
   private static final float[] NOTO_P = new float[]{0.0F, 0.15F, 0.38F, 0.6F, 0.82F, 0.92F, 1.0F};
   public static final int POLKA_SLIDE = 4;
   public static final int POLKA_INTERVAL = 2;
   public static final int POLKA_THRUSTS = 38;
   public static final int POLKA_LENGTH = 80;
   public static final int SOUL_DASH_SURGE = 7;
   public static final int SOUL_DASH_TICKS = 3;
   public static final int[] COLD_SOUL_PULSES = new int[]{11, 19, 27};
   public static final int KING_RIFF = 30;
   public static final int KING_SURFACE = 12;
   public static final int KING_SPREAD = 26;
   public static final int KING_SUMMON = 40;
   public static final int[] KING_STRIKE_HIT = new int[]{10, 10, 13};
   public static final int[] KING_STRIKE_LENGTH = new int[]{18, 18, 22};
   public static final int KING_CRUSH = 25;
   public static final int KING_CRUSH_LENGTH = 44;
   public static final int KING_CRUSH_SPREAD = 8;
   public static final int KING_ROAR = 25;
   public static final int KING_ROAR_END = 37;
   public static final int KING_ROAR_LENGTH = 48;
   public static final int KING_VANISH = 28;

   public static float notoInsert(float t) {
      if (t >= 35.0F) {
         float k = Math.min(1.0F, (t - 35.0F) / 2.0F);
         return 1.0F - k * k;
      } else if (t <= NOTO_T[0]) {
         return 0.0F;
      } else {
         for (int i = 1; i < NOTO_T.length; i++) {
            if (t <= NOTO_T[i]) {
               float k = (t - NOTO_T[i - 1]) / (NOTO_T[i] - NOTO_T[i - 1]);
               return NOTO_P[i - 1] + (NOTO_P[i] - NOTO_P[i - 1]) * k;
            }
         }

         return 1.0F;
      }
   }

   private YomiTiming() {
   }
}
