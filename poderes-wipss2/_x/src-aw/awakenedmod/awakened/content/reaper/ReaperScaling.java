package com.awakenedmod.awakened.content.reaper;

import net.minecraft.util.Mth;

public final class ReaperScaling {
   public static final long CAP_SOULS = 5000L;
   private static final double R5 = Math.sqrt(5000.0);
   private static final double R3 = Math.sqrt(3000.0);
   public static final double BASE_HP = 20.0;
   private static final long[] THRESHOLDS = new long[]{0L, 100L, 300L, 600L, 1000L, 1800L, 3000L, 4000L, 5000L};
   private static final int[] AURA_RADIUS = new int[]{0, 0, 3, 5, 8, 10, 14, 18, 24};
   private static final String[] TITLE_KEYS = new String[]{
      "fledgling", "awakened", "reaper", "soulbound", "deathbringer", "wraith", "wraith_lord", "revenant", "death_incarnate"
   };
   public static final int MILESTONE_COUNT = 9;

   private ReaperScaling() {
   }

   public static double offense(long souls) {
      return 0.5 + 0.08 * Math.sqrt((double)Math.min(souls, 5000L));
   }

   public static double maxHealthHp(long souls) {
      return souls <= 100L ? 10.0 + 0.1 * (double)souls : 20.0 + 20.0 * (Math.sqrt((double)Math.min(souls, 5000L)) - 10.0) / (R5 - 10.0);
   }

   public static double healthDelta(long souls) {
      return maxHealthHp(souls) - 20.0;
   }

   public static float damageTaken(long souls) {
      double m;
      if (souls <= 100L) {
         m = 1.3 - 0.003 * (double)souls;
      } else {
         m = Math.max(0.7, 1.0 - 0.3 * (Math.sqrt((double)Math.min(souls, 3000L)) - 10.0) / (R3 - 10.0));
      }

      return (float)m;
   }

   public static float staminaMult(long souls) {
      double m;
      if (souls <= 100L) {
         m = 0.5 + 0.005 * (double)souls;
      } else {
         m = Math.min(3.0, 1.0 + 2.0 * (Math.sqrt((double)Math.min(souls, 5000L)) - 10.0) / (R5 - 10.0));
      }

      return (float)m;
   }

   public static int milestoneIndex(long souls) {
      int idx = 0;
      int i = 0;

      while (i < THRESHOLDS.length && souls >= THRESHOLDS[i]) {
         idx = i++;
      }

      return idx;
   }

   public static long milestoneThreshold(int idx) {
      return THRESHOLDS[Mth.clamp(idx, 0, THRESHOLDS.length - 1)];
   }

   public static String milestoneTitleKey(int idx) {
      return "awakened.reaper.milestone." + TITLE_KEYS[Mth.clamp(idx, 0, TITLE_KEYS.length - 1)];
   }

   public static int auraRadius(int idx) {
      return AURA_RADIUS[Mth.clamp(idx, 0, AURA_RADIUS.length - 1)];
   }

   public static float tierFraction(int idx) {
      return (float)Mth.clamp(idx, 0, 8) / 8.0F;
   }

   public static long nextThreshold(long souls) {
      int idx = milestoneIndex(souls);
      return idx >= THRESHOLDS.length - 1 ? THRESHOLDS[THRESHOLDS.length - 1] : THRESHOLDS[idx + 1];
   }

   public static float progressToNext(long souls) {
      int idx = milestoneIndex(souls);
      if (idx >= THRESHOLDS.length - 1) {
         return 1.0F;
      } else {
         long lo = THRESHOLDS[idx];
         long hi = THRESHOLDS[idx + 1];
         return Mth.clamp((float)(souls - lo) / (float)(hi - lo), 0.0F, 1.0F);
      }
   }
}
