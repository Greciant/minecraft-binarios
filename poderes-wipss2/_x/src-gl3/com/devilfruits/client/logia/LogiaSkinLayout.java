package com.devilfruits.client.logia;

import java.util.Arrays;
import net.minecraft.util.Mth;

public final class LogiaSkinLayout {
   private static final int[][] BOXES = new int[][]{
      {0, 0, 8, 8, 8, 32, 0},
      {16, 16, 8, 12, 4, 0, 16},
      {40, 16, 4, 12, 4, 0, 16},
      {32, 48, 4, 12, 4, 16, 0},
      {0, 16, 4, 12, 4, 0, 16},
      {16, 48, 4, 12, 4, -16, 0}
   };
   private static final float[][] EXTENTS = new float[][]{
      {-0.25F, 0.25F, 1.5F, 2.0F},
      {-0.25F, 0.25F, 0.75F, 1.5F},
      {0.25F, 0.5F, 0.75F, 1.5F},
      {-0.5F, -0.25F, 0.75F, 1.5F},
      {0.0F, 0.25F, 0.0F, 0.75F},
      {-0.25F, 0.0F, 0.0F, 0.75F}
   };
   private static final int[][][] FACES_WIDE = new int[6][][];
   private static final int[][][] FACES_SLIM = new int[6][][];

   static float[] extents(int limb) {
      return EXTENTS[limb];
   }

   public static int index(int limbBit) {
      return Integer.numberOfTrailingZeros(limbBit);
   }

   public static int bit(int index) {
      return 1 << index;
   }

   private static int width(int limb, boolean slim) {
      return !slim || limb != 2 && limb != 3 ? BOXES[limb][2] : 3;
   }

   public static void classify(byte[] limbOf, byte[] along, boolean slim) {
      Arrays.fill(limbOf, (byte)-1);

      for (int limb = 0; limb < 6; limb++) {
         int[] b = BOXES[limb];
         int w = width(limb, slim);
         int h = b[3];
         int d = b[4];

         for (int layer = 0; layer < 2; layer++) {
            int u0 = b[0] + layer * b[5];
            int v0 = b[1] + layer * b[6];

            for (int y = v0; y < v0 + d + h; y++) {
               for (int x = u0; x < u0 + 2 * (d + w); x++) {
                  boolean capRow = y < v0 + d;
                  if (!capRow || x >= u0 + d && x < u0 + d + 2 * w) {
                     int i = y * 64 + x;
                     limbOf[i] = (byte)limb;
                     float t = capRow ? (x < u0 + d + w ? 0.0F : 1.0F) : ((float)(y - (v0 + d)) + 0.5F) / (float)h;
                     along[i] = (byte)Mth.clamp((int)(t * 255.0F), 0, 255);
                  }
               }
            }
         }
      }
   }

   public static int limbAt(float lateral, float height) {
      if (height >= 1.5F) {
         return 0;
      } else if (height >= 0.75F) {
         return lateral > 0.25F ? 2 : (lateral < -0.25F ? 3 : 1);
      } else {
         return lateral >= 0.0F ? 4 : 5;
      }
   }

   public static float across(int limb, float lateral) {
      float[] e = EXTENTS[limb];
      return Mth.clamp((e[1] - lateral) / (e[1] - e[0]), 0.0F, 1.0F);
   }

   public static float down(int limb, float height) {
      float[] e = EXTENTS[limb];
      return Mth.clamp((e[3] - height) / (e[3] - e[2]), 0.0F, 1.0F);
   }

   public static int[][] holeFaces(int limb, boolean slim) {
      return (slim ? FACES_SLIM : FACES_WIDE)[limb];
   }

   private static int[][] faces(int limb, boolean slim) {
      int[] b = BOXES[limb];
      int w = width(limb, slim);
      int h = b[3];
      int d = b[4];
      int[][] faces = new int[4][];

      for (int layer = 0; layer < 2; layer++) {
         int u0 = b[0] + layer * b[5];
         int v0 = b[1] + layer * b[6];
         faces[layer * 2] = new int[]{u0 + d, v0 + d, w, h, 0};
         faces[layer * 2 + 1] = new int[]{u0 + 2 * d + w, v0 + d, w, h, 1};
      }

      return faces;
   }

   private LogiaSkinLayout() {
   }

   static {
      for (int limb = 0; limb < 6; limb++) {
         FACES_WIDE[limb] = faces(limb, false);
         FACES_SLIM[limb] = faces(limb, true);
      }
   }
}
