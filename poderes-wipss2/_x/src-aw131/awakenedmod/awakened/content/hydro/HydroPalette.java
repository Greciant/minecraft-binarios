package com.awakenedmod.awakened.content.hydro;

public final class HydroPalette {
   public static final int WET_RGB = 4159204;
   public static final int DRY_RGB = 8368878;
   public static final float WET_OPACITY = 1.0F;
   public static final float DRY_OPACITY = 0.62F;
   private static final float BODY_WET_OPACITY = 2.0625F;
   private static final float BODY_DRY_OPACITY = 1.6875F;
   private static final float BODY_DRY_PALE = 0.3F;
   private static final int BODY_DEEP_RGB = 2841540;
   private static final int BODY_CREST_RGB = 9423864;
   private static final float LINE_DRY_OPACITY = 0.85F;

   private HydroPalette() {
   }

   public static int rgb(boolean wet) {
      return wet ? 4159204 : 8368878;
   }

   public static float opacity(boolean wet) {
      return wet ? 1.0F : 0.62F;
   }

   public static float lineOpacity(boolean wet) {
      return wet ? 1.0F : 0.85F;
   }

   public static int bodyRgb(boolean wet) {
      return wet ? 4159204 : mix(4159204, 8368878, 0.3F);
   }

   public static int bodyDeepRgb(boolean wet) {
      return wet ? 2841540 : mix(2841540, 8368878, 0.3F);
   }

   public static int bodyCrestRgb(boolean wet) {
      return wet ? 9423864 : mix(9423864, 8368878, 0.3F);
   }

   public static float bodyOpacity(boolean wet) {
      return wet ? 2.0625F : 1.6875F;
   }

   private static int mix(int a, int b, float t) {
      int r = Math.round((float)(a >> 16 & 0xFF) + (float)((b >> 16 & 0xFF) - (a >> 16 & 0xFF)) * t);
      int g = Math.round((float)(a >> 8 & 0xFF) + (float)((b >> 8 & 0xFF) - (a >> 8 & 0xFF)) * t);
      int bl = Math.round((float)(a & 0xFF) + (float)((b & 0xFF) - (a & 0xFF)) * t);
      return r << 16 | g << 8 | bl;
   }
}
