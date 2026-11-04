package com.awakenedmod.awakened.content.reaper;

public final class ReaperAuraShape {
   public static final float CHEST_RADIUS = 0.5F;
   public static final float CHEST_HEIGHT = 0.8F;
   public static final float CHEST_Y_FRACTION = 0.55F;

   private ReaperAuraShape() {
   }

   public static ReaperAuraShape.Band band(int tier) {
      if (tier <= 1) {
         return ReaperAuraShape.Band.NONE;
      } else {
         return tier == 2 ? ReaperAuraShape.Band.CHEST : ReaperAuraShape.Band.BODY;
      }
   }

   public static float bandFrac(int tier) {
      float f = (float)(tier - 3) / 5.0F;
      return f < 0.0F ? 0.0F : (f > 1.0F ? 1.0F : f);
   }

   public static float bodyRadius(int tier) {
      return 0.5F + bandFrac(tier) * 0.3F;
   }

   public static float bodyHeight(int tier, float bbHeight, float shown) {
      return bbHeight * 1.05F + bandFrac(tier) * 2.6F * shown;
   }

   public static boolean hasLightningLicks(int tier) {
      return tier >= 4;
   }

   public static enum Band {
      NONE,
      CHEST,
      BODY;
   }
}
