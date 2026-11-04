package com.devilfruits.fruits.doru_doru.wax;

import net.minecraft.world.item.DyeColor;

public final class WaxPalette {
   public static final int COLORS = 16;
   public static final int CREAM = 16776696;
   private static final int[] CURED = new int[16];
   private static final int[] FRESH = new int[16];

   private static int mix(int a, int b, float k) {
      return Math.round((float)a + (float)(b - a) * k);
   }

   public static int tint(int index, boolean fresh) {
      int i = Math.floorMod(index, 16);
      return fresh ? FRESH[i] : CURED[i];
   }

   public static int nearest(int rgb) {
      int r = rgb >> 16 & 0xFF;
      int g = rgb >> 8 & 0xFF;
      int b = rgb & 0xFF;
      int best = 0;
      long bestD = Long.MAX_VALUE;

      for (DyeColor dye : DyeColor.values()) {
         int c = dye.getTextureDiffuseColor();
         long dr = (long)(r - (c >> 16 & 0xFF));
         long dg = (long)(g - (c >> 8 & 0xFF));
         long db = (long)(b - (c & 0xFF));
         long d = 3L * dr * dr + 4L * dg * dg + 2L * db * db;
         if (d < bestD) {
            bestD = d;
            best = dye.getId();
         }
      }

      return best;
   }

   private WaxPalette() {
   }

   static {
      for (DyeColor dye : DyeColor.values()) {
         int c = dye.getTextureDiffuseColor();
         int r = c >> 16 & 0xFF;
         int g = c >> 8 & 0xFF;
         int b = c & 0xFF;
         float k = dye == DyeColor.WHITE ? 0.0F : (dye == DyeColor.BLACK ? 0.72F : 0.62F);
         int cr = mix(255, r, k);
         int cg = mix(253, g, k);
         int cb = mix(248, b, k);
         CURED[dye.getId()] = cr << 16 | cg << 8 | cb;
         FRESH[dye.getId()] = Math.min(255, cr + 2) << 16 | cg << 8 | Math.max(0, cb - 6);
      }
   }
}
