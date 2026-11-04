package com.devilfruits.fruits.doru_doru.wax;

public final class WaxSurface {
   public static final int G = 8;
   public static final float STRENGTH = 1.65F;
   public static final float LIMIT = 0.45F;
   private static final float CLAMP = 0.49F;
   private static final float[] W = new float[]{1.0F, 3.0F, 3.0F, 1.0F};
   private static final float[] OFF = new float[]{-1.5F, -0.5F, 0.5F, 1.5F};

   public static boolean filled(long[] grid, int gx, int gy, int gz) {
      return (grid[gy] >>> gx + (gz << 3) & 1L) != 0L;
   }

   public static void set(long[] grid, int gx, int gy, int gz) {
      grid[gy] |= 1L << gx + (gz << 3);
   }

   public static void solve(long[] grid, int lx, int ly, int lz, float[] out) {
      float d = 0.0F;
      float gx = 0.0F;
      float gy = 0.0F;
      float gz = 0.0F;

      for (int j = 0; j < 4; j++) {
         long row = grid[ly + j];
         if (row != 0L) {
            for (int k = 0; k < 4; k++) {
               int z = lz + k;

               for (int i = 0; i < 4; i++) {
                  if ((row >>> lx + i + (z << 3) & 1L) != 0L) {
                     float w = W[i] * W[j] * W[k];
                     d += w;
                     gx += w * OFF[i];
                     gy += w * OFF[j];
                     gz += w * OFF[k];
                  }
               }
            }
         }
      }

      d /= 512.0F;
      float len = (float)Math.sqrt((double)(gx * gx + gy * gy + gz * gz));
      if (len < 0.001F) {
         out[0] = out[1] = out[2] = 0.0F;
         out[3] = 0.0F;
         out[4] = 1.0F;
         out[5] = 0.0F;
      } else {
         float nx = gx / len;
         float ny = gy / len;
         float nz = gz / len;
         float s = Math.max(-0.45F, Math.min(0.45F, (0.5F - d) * 1.65F));
         out[0] = clamp(nx * s);
         out[1] = clamp(ny * s);
         out[2] = clamp(nz * s);
         out[3] = -nx;
         out[4] = -ny;
         out[5] = -nz;
      }
   }

   private static float clamp(float v) {
      return Math.abs(v) < 1.0E-5F ? 0.0F : Math.max(-0.49F, Math.min(0.49F, v));
   }

   public static long[] grid(long[] masks) {
      long[] grid = new long[8];

      for (int gy = 0; gy < 8; gy++) {
         int cy = gy - 2;
         int by = cy < 0 ? -1 : (cy > 3 ? 1 : 0);
         int iy = cy - 4 * by;

         for (int gz = 0; gz < 8; gz++) {
            int cz = gz - 2;
            int bz = cz < 0 ? -1 : (cz > 3 ? 1 : 0);
            int iz = cz - 4 * bz;

            for (int gx = 0; gx < 8; gx++) {
               int cx = gx - 2;
               int bx = cx < 0 ? -1 : (cx > 3 ? 1 : 0);
               int ix = cx - 4 * bx;
               long m = masks[bx + 1 + 3 * (bz + 1 + 3 * (by + 1))];
               if (m != 0L && WaxMask.has(m, ix, iy, iz)) {
                  grid[gy] |= 1L << gx + (gz << 3);
               }
            }
         }
      }

      return grid;
   }

   private WaxSurface() {
   }
}
