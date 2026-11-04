package com.awakenedmod.awakened.core.skill;

public final class SkillFormula {
   public static final int POINTS_PER_LEVEL = 1;
   private static final double BASE_XP = 100.0;
   private static final double XP_GROWTH = 50.0;

   private SkillFormula() {
   }

   public static double xpForNext(int level) {
      return 100.0 + (double)Math.max(0, level - 1) * 50.0;
   }

   public static int pointsForLevel(int level) {
      return Math.max(0, level - 1) * 1;
   }

   public static float fraction(int level, double xp) {
      double need = xpForNext(level);
      return need <= 0.0 ? 0.0F : (float)Math.max(0.0, Math.min(1.0, xp / need));
   }
}
