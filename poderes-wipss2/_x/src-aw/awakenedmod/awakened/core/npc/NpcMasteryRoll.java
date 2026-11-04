package com.awakenedmod.awakened.core.npc;

import net.minecraft.util.RandomSource;

public final class NpcMasteryRoll {
   private static final float CLIMB = 0.6666667F;
   private static final long DAYS_PER_STEP = 20L;
   private static final int MAX_AGE_STEPS = 3;
   private static final long TICKS_PER_DAY = 24000L;

   private NpcMasteryRoll() {
   }

   public static int roll(RandomSource random, long gameTime) {
      int max = NpcConfig.maxMastery();
      long days = gameTime / 24000L;
      int mastery = 3 + (int)Math.min(3L, days / 20L);

      while (mastery < max && random.nextFloat() < 0.6666667F) {
         mastery++;
      }

      return Math.min(mastery, max);
   }
}
