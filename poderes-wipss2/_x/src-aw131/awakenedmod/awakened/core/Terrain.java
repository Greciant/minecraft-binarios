package com.awakenedmod.awakened.core;

import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class Terrain {
   private Terrain() {
   }

   public static double floorNear(Level level, double x, double z, double refY, int up, int down) {
      return floorNear(level, x, z, refY, up, down, false);
   }

   public static double floorNear(Level level, double x, double z, double refY, int up, int down, boolean fluids) {
      int bx = Mth.floor(x);
      int bz = Mth.floor(z);
      int base = Mth.floor(refY - 0.001);
      MutableBlockPos pos = new MutableBlockPos();

      for (int d = 0; d <= Math.max(up, down); d++) {
         if (d <= down) {
            double y = floorAt(level, pos, bx, base - d, bz, fluids);
            if (!Double.isNaN(y)) {
               return y;
            }
         }

         if (d > 0 && d <= up) {
            double y = floorAt(level, pos, bx, base + d, bz, fluids);
            if (!Double.isNaN(y)) {
               return y;
            }
         }
      }

      return Double.NaN;
   }

   private static double floorAt(Level level, MutableBlockPos pos, int bx, int by, int bz, boolean fluids) {
      pos.set(bx, by, bz);
      VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
      double top = shape.isEmpty() ? Double.NaN : (double)by + shape.max(Axis.Y);
      if (fluids) {
         FluidState fluid = level.getFluidState(pos);
         if (!fluid.isEmpty()) {
            double surface = (double)((float)by + fluid.getHeight(level, pos));
            top = Double.isNaN(top) ? surface : Math.max(top, surface);
         }
      }

      if (Double.isNaN(top)) {
         return Double.NaN;
      } else {
         pos.set(bx, by + 1, bz);
         return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() && (!fluids || level.getFluidState(pos).isEmpty()) ? top : Double.NaN;
      }
   }
}
