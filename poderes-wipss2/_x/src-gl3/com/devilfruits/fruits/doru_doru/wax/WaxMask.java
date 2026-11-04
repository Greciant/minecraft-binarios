package com.devilfruits.fruits.doru_doru.wax;

import net.minecraft.core.Direction;

public final class WaxMask {
   public static final int N = 4;
   public static final int CELLS = 64;
   public static final long FULL = -1L;
   public static final long EMPTY = 0L;
   public static final int LAYER_BITS = 65535;

   public static int index(int x, int y, int z) {
      return x | z << 2 | y << 4;
   }

   public static boolean has(long mask, int x, int y, int z) {
      return (mask >>> index(x, y, z) & 1L) != 0L;
   }

   public static long with(long mask, int x, int y, int z) {
      return mask | 1L << index(x, y, z);
   }

   public static long without(long mask, int x, int y, int z) {
      return mask & ~(1L << index(x, y, z));
   }

   public static int count(long mask) {
      return Long.bitCount(mask);
   }

   public static float fill(long mask) {
      return (float)Long.bitCount(mask) / 64.0F;
   }

   public static int layer(long mask, int y) {
      return (int)(mask >>> (y << 4)) & 65535;
   }

   public static long withLayer(long mask, int y, int bits) {
      long shift = (long)(bits & 65535) << (y << 4);
      return mask & ~(65535L << (y << 4)) | shift;
   }

   public static int top(long mask) {
      for (int y = 3; y >= 0; y--) {
         if (layer(mask, y) != 0) {
            return y;
         }
      }

      return -1;
   }

   public static boolean faceClosed(long mask, Direction dir) {
      for (int a = 0; a < 4; a++) {
         for (int b = 0; b < 4; b++) {
            int x;
            int y;
            int z;
            switch (dir) {
               case DOWN:
                  x = a;
                  y = 0;
                  z = b;
                  break;
               case UP:
                  x = a;
                  y = 3;
                  z = b;
                  break;
               case NORTH:
                  x = a;
                  y = b;
                  z = 0;
                  break;
               case SOUTH:
                  x = a;
                  y = b;
                  z = 3;
                  break;
               case WEST:
                  x = 0;
                  y = b;
                  z = a;
                  break;
               default:
                  x = 3;
                  y = b;
                  z = a;
            }

            if (!has(mask, x, y, z)) {
               return false;
            }
         }
      }

      return true;
   }

   public static boolean touches(long mask, Direction dir) {
      for (int a = 0; a < 4; a++) {
         for (int b = 0; b < 4; b++) {
            int x;
            int y;
            int z;
            switch (dir) {
               case DOWN:
                  x = a;
                  y = 0;
                  z = b;
                  break;
               case UP:
                  x = a;
                  y = 3;
                  z = b;
                  break;
               case NORTH:
                  x = a;
                  y = b;
                  z = 0;
                  break;
               case SOUTH:
                  x = a;
                  y = b;
                  z = 3;
                  break;
               case WEST:
                  x = 0;
                  y = b;
                  z = a;
                  break;
               default:
                  x = 3;
                  y = b;
                  z = a;
            }

            if (has(mask, x, y, z)) {
               return true;
            }
         }
      }

      return false;
   }

   public static long rotateY(long mask, int quarterTurns) {
      int q = Math.floorMod(quarterTurns, 4);
      if (q != 0 && mask != -1L && mask != 0L) {
         long out = 0L;

         for (int i = 0; i < 64; i++) {
            if ((mask >>> i & 1L) != 0L) {
               int x = i & 3;
               int z = i >> 2 & 3;
               int y = i >> 4;

               for (int t = 0; t < q; t++) {
                  int nx = 3 - z;
                  z = x;
                  x = nx;
               }

               out |= 1L << index(x, y, z);
            }
         }

         return out;
      } else {
         return mask;
      }
   }

   public static long mirrorX(long mask) {
      if (mask != -1L && mask != 0L) {
         long out = 0L;

         for (int i = 0; i < 64; i++) {
            if ((mask >>> i & 1L) != 0L) {
               out |= 1L << index(3 - (i & 3), i >> 4, i >> 2 & 3);
            }
         }

         return out;
      } else {
         return mask;
      }
   }

   public static long mirrorZ(long mask) {
      if (mask != -1L && mask != 0L) {
         long out = 0L;

         for (int i = 0; i < 64; i++) {
            if ((mask >>> i & 1L) != 0L) {
               out |= 1L << index(i & 3, i >> 4, 3 - (i >> 2 & 3));
            }
         }

         return out;
      } else {
         return mask;
      }
   }

   private WaxMask() {
   }
}
