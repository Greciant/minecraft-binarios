package com.awakenedmod.awakened.core.npc;

import net.minecraft.util.Mth;

public record NpcSkill(int reactionTicks, float leadAccuracy, float sidestepChance, float scoreNoise, float turnDegPerTick) {
   private static final int[] AT = new int[]{3, 7, 12};
   private static final float[] REACTION = new float[]{16.0F, 10.0F, 6.0F};
   private static final float[] LEAD = new float[]{0.5F, 0.8F, 1.0F};
   private static final float[] SIDESTEP = new float[]{0.2F, 0.45F, 0.7F};
   private static final float[] NOISE = new float[]{0.35F, 0.18F, 0.06F};
   private static final float[] TURN = new float[]{1.8F, 3.2F, 5.0F};

   public static NpcSkill of(int mastery) {
      return new NpcSkill(Math.round(lerp(REACTION, mastery)), lerp(LEAD, mastery), lerp(SIDESTEP, mastery), lerp(NOISE, mastery), lerp(TURN, mastery));
   }

   public int readDelay() {
      return Math.max(2, this.reactionTicks / 2);
   }

   private static float lerp(float[] values, int mastery) {
      if (mastery <= AT[0]) {
         return values[0];
      } else {
         for (int i = 1; i < AT.length; i++) {
            if (mastery <= AT[i]) {
               float t = (float)(mastery - AT[i - 1]) / (float)(AT[i] - AT[i - 1]);
               return Mth.lerp(t, values[i - 1], values[i]);
            }
         }

         return values[values.length - 1];
      }
   }
}
