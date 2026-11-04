package com.devilfruits.core.haki;

import net.minecraft.util.RandomSource;

public final class HakiClash {
   public static float score(int level, float coverage, boolean ryuo, float jitter, RandomSource random) {
      return (float)level + coverage * 3.0F + (ryuo ? 2.0F : 0.0F) + random.nextFloat() * jitter;
   }

   public static HakiClash.Result resolve(
      int attackerLevel, float attackerCoverage, boolean attackerRyuo, int defenderLevel, float defenderCoverage, float jitter, RandomSource random
   ) {
      float a = score(attackerLevel, attackerCoverage, attackerRyuo, jitter, random);
      float d = score(defenderLevel, defenderCoverage, false, jitter, random);
      return new HakiClash.Result(a >= d, Math.min(1.0F, 0.3F + (float)(attackerLevel + defenderLevel) / 20.0F * 0.7F));
   }

   private HakiClash() {
   }

   public static record Result(boolean attackerWins, float strength) {
   }
}
