package com.devilfruits.core.limb;

public final class LimbBands {
   public static final int STRIDE = 4;
   public static final float MAX_LENGTH = 1024.0F;

   public static int build(float length, float scale, float fist, float[] out) {
      return build(length, scale, fist, 4, out);
   }

   public static int build(float length, float scale, float fist, int fill, float[] out) {
      length = Math.min(Math.max(length, 0.5F), 1024.0F);
      fist = Math.max(fist, 0.5F);
      fill = Math.max(4, Math.min(7, fill));
      float shoulder = 4.0F;
      float hand = 4.0F * fist;
      int n = 0;
      if (length < shoulder + hand) {
         float k = length / (shoulder + hand);
         n = put(out, n, 0.0F, shoulder * k, 0.0F, 4.0F);
         n = put(out, n, shoulder * k, (shoulder + fist) * k, 8.0F, 9.0F);
         return put(out, n, (shoulder + fist) * k, length, 9.0F, 12.0F);
      } else {
         n = put(out, n, 0.0F, shoulder, 0.0F, 4.0F);
         float middle = length - shoulder - hand;
         float s = shoulder;
         float lead = (float)(fill - 4);
         if (middle < lead) {
            float cut = cut(middle, scale);
            if (cut > 0.0F) {
               n = put(out, n, shoulder, shoulder + cut, 4.0F, 4.0F + cut);
               s = shoulder + cut;
            }
         } else {
            if (lead > 0.0F) {
               n = put(out, n, shoulder, shoulder + lead, 4.0F, (float)fill);
               s = shoulder + lead;
            }

            float tile = (float)(8 - fill);
            float rest = middle - lead;
            if (tile * scale <= 1.001F) {
               float cut = cut(rest, scale);
               if (cut > 0.0F) {
                  n = put(out, n, s, s + cut, (float)fill + 0.5F / scale, (float)fill + 0.5F / scale);
                  s += cut;
               }
            } else {
               int tiles = (int)(rest / tile + 1.0E-4F);

               for (int i = 0; i < tiles; i++) {
                  n = put(out, n, s, s + tile, (float)fill, 8.0F);
                  s += tile;
               }

               float cut = cut(rest - (float)tiles * tile, scale);
               if (cut > 0.0F) {
                  n = put(out, n, s, s + cut, (float)fill, (float)fill + cut);
                  s += cut;
               }
            }
         }

         n = put(out, n, s, s + fist, 8.0F, 9.0F);
         return put(out, n, s + fist, s + hand, 9.0F, 12.0F);
      }
   }

   private static float cut(float length, float scale) {
      return (float)Math.floor((double)(length * scale) + 0.001) / scale;
   }

   public static int capacity(float length) {
      return ((int)Math.min(length, 1024.0F) + 8) * 4;
   }

   public static float length(float[] segments, int count) {
      return count == 0 ? 0.0F : segments[(count - 1) * 4 + 1];
   }

   private static int put(float[] out, int n, float s0, float s1, float r0, float r1) {
      int i = n * 4;
      out[i] = s0;
      out[i + 1] = s1;
      out[i + 2] = r0;
      out[i + 3] = r1;
      return n + 1;
   }

   private LimbBands() {
   }
}
