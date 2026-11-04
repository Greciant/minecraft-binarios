package com.awakenedmod.awakened.core.progression;

public final class MasteryFormula {
   private MasteryFormula() {
   }

   public static double progressForNext(int level) {
      return 100.0 + (double)level * 50.0;
   }

   public static double totalPoints(int level, double progress) {
      return 100.0 * (double)level + 25.0 * (double)level * (double)(level - 1) + progress;
   }

   public static int levelAt(double points) {
      if (points <= 0.0) {
         return 0;
      } else {
         long level = (long)Math.floor((-75.0 + Math.sqrt(5625.0 + 100.0 * points)) / 50.0);

         while (level > 0L && totalPoints((int)Math.min(level, 2147483647L), 0.0) > points) {
            level--;
         }

         while (level < 2147483647L && totalPoints((int)(level + 1L), 0.0) <= points) {
            level++;
         }

         return (int)Math.min(level, 2147483647L);
      }
   }

   public static float fraction(int level, double progress) {
      double needed = progressForNext(level);
      return needed <= 0.0 ? 1.0F : (float)Math.max(0.0, Math.min(1.0, progress / needed));
   }
}
