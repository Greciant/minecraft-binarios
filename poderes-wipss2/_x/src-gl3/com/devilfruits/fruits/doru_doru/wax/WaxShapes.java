package com.devilfruits.fruits.doru_doru.wax;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class WaxShapes {
   private static final int CACHE = 4096;
   private static final Map<Long, VoxelShape> SHAPES = new LinkedHashMap<Long, VoxelShape>(256, 0.75F, true) {
      @Override
      protected boolean removeEldestEntry(Entry<Long, VoxelShape> eldest) {
         return this.size() > 4096;
      }
   };

   public static VoxelShape shape(long mask) {
      if (mask == -1L) {
         return Shapes.block();
      } else if (mask == 0L) {
         return Shapes.empty();
      } else {
         synchronized (SHAPES) {
            VoxelShape cached = SHAPES.get(mask);
            if (cached != null) {
               return cached;
            }
         }

         VoxelShape shape = build(mask);
         synchronized (SHAPES) {
            SHAPES.put(mask, shape);
            return shape;
         }
      }
   }

   private static VoxelShape build(long mask) {
      int n = 4;
      long left = mask;
      VoxelShape shape = Shapes.empty();

      while (left != 0L) {
         int i = Long.numberOfTrailingZeros(left);
         int x0 = i & 3;
         int z0 = i >> 2 & 3;
         int y0 = i >> 4;
         int x1 = x0;

         while (x1 + 1 < n && WaxMask.has(left, x1 + 1, y0, z0)) {
            x1++;
         }

         int z1 = z0;

         while (z1 + 1 < n && row(left, x0, x1, y0, z1 + 1)) {
            z1++;
         }

         int y1 = y0;

         while (y1 + 1 < n && slab(left, x0, x1, z0, z1, y1 + 1)) {
            y1++;
         }

         for (int y = y0; y <= y1; y++) {
            for (int z = z0; z <= z1; z++) {
               for (int x = x0; x <= x1; x++) {
                  left = WaxMask.without(left, x, y, z);
               }
            }
         }

         shape = Shapes.or(
            shape, Shapes.box((double)x0 / 4.0, (double)y0 / 4.0, (double)z0 / 4.0, (double)(x1 + 1) / 4.0, (double)(y1 + 1) / 4.0, (double)(z1 + 1) / 4.0)
         );
      }

      return shape.optimize();
   }

   private static boolean row(long m, int x0, int x1, int y, int z) {
      for (int x = x0; x <= x1; x++) {
         if (!WaxMask.has(m, x, y, z)) {
            return false;
         }
      }

      return true;
   }

   private static boolean slab(long m, int x0, int x1, int z0, int z1, int y) {
      for (int z = z0; z <= z1; z++) {
         if (!row(m, x0, x1, y, z)) {
            return false;
         }
      }

      return true;
   }

   private WaxShapes() {
   }
}
