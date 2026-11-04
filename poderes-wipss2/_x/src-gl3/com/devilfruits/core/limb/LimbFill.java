package com.devilfruits.core.limb;

public final class LimbFill {
   public static final float SAME = 48.0F;
   public static final float HARD = 120.0F;

   public static int firstRow(int[] px, int w, int h, LimbUv.Layout layout) {
      int edges = 0;
      int hard = 0;

      for (int row = 4; row <= 7; row++) {
         float d = difference(px, w, h, layout, row, row == 7 ? 4 : row + 1);
         if (d >= 48.0F) {
            edges++;
         }

         if (d >= 120.0F) {
            hard++;
         }
      }

      if (edges != 0 && hard < 3 && (hard != 0 || edges < 3)) {
         int first = 7;
         int row = 6;

         while (row >= 4 && !(difference(px, w, h, layout, row, 7) >= 48.0F)) {
            first = row--;
         }

         return first;
      } else {
         return 4;
      }
   }

   public static float difference(int[] px, int w, int h, LimbUv.Layout layout, int rowA, int rowB) {
      int s = Math.max(1, Math.round(layout.scale()));
      int x0 = Math.round(layout.px((float)layout.u()));
      int x1 = Math.min(w, Math.round(layout.px((float)(layout.u() + 2 * layout.depth() + 2 * layout.width()))));
      int ya = Math.round(layout.px(layout.sideTop() + (float)rowA));
      int yb = Math.round(layout.px(layout.sideTop() + (float)rowB));
      long sum = 0L;
      int n = 0;

      for (int dy = 0; dy < s && ya + dy < h && yb + dy < h; dy++) {
         for (int x = x0; x < x1; x++) {
            sum += (long)texel(px[(ya + dy) * w + x], px[(yb + dy) * w + x]);
            n++;
         }
      }

      return n == 0 ? 0.0F : (float)sum / (float)n;
   }

   private static int texel(int a, int b) {
      boolean oa = a >>> 24 >= 128;
      boolean ob = b >>> 24 >= 128;
      if (oa != ob) {
         return 765;
      } else {
         return !oa ? 0 : Math.abs((a & 0xFF) - (b & 0xFF)) + Math.abs((a >> 8 & 0xFF) - (b >> 8 & 0xFF)) + Math.abs((a >> 16 & 0xFF) - (b >> 16 & 0xFF));
      }
   }

   private LimbFill() {
   }
}
