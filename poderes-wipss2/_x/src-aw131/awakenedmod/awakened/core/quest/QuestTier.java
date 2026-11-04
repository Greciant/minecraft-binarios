package com.awakenedmod.awakened.core.quest;

import net.minecraft.util.RandomSource;

public enum QuestTier {
   I(-7364432),
   II(-10041089),
   III(-15283);

   private static final int[][] WEIGHTS = new int[][]{{10, 3, 0}, {5, 8, 2}, {2, 8, 5}, {1, 6, 8}};
   private final int color;

   private QuestTier(int color) {
      this.color = color;
   }

   public String glyph() {
      return this.name();
   }

   public int color() {
      return this.color;
   }

   public int weight(int masteryLevel) {
      int band = masteryLevel < 5 ? 0 : (masteryLevel < 10 ? 1 : (masteryLevel < 15 ? 2 : 3));
      return WEIGHTS[band][this.ordinal()];
   }

   public static QuestTier byIndex(int i) {
      QuestTier[] all = values();
      return all[Math.max(0, Math.min(all.length - 1, i))];
   }

   public static QuestTier roll(RandomSource random, int masteryLevel, boolean[] available) {
      int total = 0;

      for (QuestTier tier : values()) {
         if (available[tier.ordinal()]) {
            total += tier.weight(masteryLevel);
         }
      }

      if (total <= 0) {
         return null;
      } else {
         int pick = random.nextInt(total);

         for (QuestTier tierx : values()) {
            if (available[tierx.ordinal()]) {
               pick -= tierx.weight(masteryLevel);
               if (pick < 0) {
                  return tierx;
               }
            }
         }

         return null;
      }
   }
}
