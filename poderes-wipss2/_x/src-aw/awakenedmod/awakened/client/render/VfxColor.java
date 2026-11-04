package com.awakenedmod.awakened.client.render;

final class VfxColor {
   private VfxColor() {
   }

   static int argb(int a, int rgb) {
      return Math.max(0, Math.min(255, a)) << 24 | rgb & 16777215;
   }

   static int lighten(int rgb, float t) {
      int r = rgb >> 16 & 0xFF;
      int g = rgb >> 8 & 0xFF;
      int b = rgb & 0xFF;
      r += (int)((float)(255 - r) * t);
      g += (int)((float)(255 - g) * t);
      b += (int)((float)(255 - b) * t);
      return r << 16 | g << 8 | b;
   }

   static int hueShift(int rgb, float degrees) {
      float r = (float)(rgb >> 16 & 0xFF) / 255.0F;
      float g = (float)(rgb >> 8 & 0xFF) / 255.0F;
      float b = (float)(rgb & 0xFF) / 255.0F;
      float max = Math.max(r, Math.max(g, b));
      float min = Math.min(r, Math.min(g, b));
      float d = max - min;
      if (d <= 0.0F) {
         return rgb;
      } else {
         float h;
         if (max == r) {
            h = (g - b) / d;
         } else if (max == g) {
            h = (b - r) / d + 2.0F;
         } else {
            h = (r - g) / d + 4.0F;
         }

         h = h * 60.0F + degrees;
         h = (h % 360.0F + 360.0F) % 360.0F;
         float s = d / max;
         float c = max * s;
         float x = c * (1.0F - Math.abs(h / 60.0F % 2.0F - 1.0F));
         float m = max - c;
         int sector = (int)(h / 60.0F) % 6;
         float r1;
         float g1;
         float b1;
         switch (sector) {
            case 0:
               r1 = c;
               g1 = x;
               b1 = 0.0F;
               break;
            case 1:
               r1 = x;
               g1 = c;
               b1 = 0.0F;
               break;
            case 2:
               r1 = 0.0F;
               g1 = c;
               b1 = x;
               break;
            case 3:
               r1 = 0.0F;
               g1 = x;
               b1 = c;
               break;
            case 4:
               r1 = x;
               g1 = 0.0F;
               b1 = c;
               break;
            default:
               r1 = c;
               g1 = 0.0F;
               b1 = x;
         }

         int rr = Math.round((r1 + m) * 255.0F);
         int gg = Math.round((g1 + m) * 255.0F);
         int bb = Math.round((b1 + m) * 255.0F);
         return rr << 16 | gg << 8 | bb;
      }
   }

   static int shade(int rgb, float f) {
      int r = (int)((float)(rgb >> 16 & 0xFF) * f);
      int g = (int)((float)(rgb >> 8 & 0xFF) * f);
      int b = (int)((float)(rgb & 0xFF) * f);
      return r << 16 | g << 8 | b;
   }

   static int core(int body, int color2, float derive) {
      return color2 == -1 ? lighten(body, derive) : color2;
   }
}
