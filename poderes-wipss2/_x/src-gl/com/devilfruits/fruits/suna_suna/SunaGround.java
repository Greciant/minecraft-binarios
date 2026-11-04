package com.devilfruits.fruits.suna_suna;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class SunaGround {
   public static double surface(Level level, double x, double y, double z, int up, int down) {
      MutableBlockPos pos = new MutableBlockPos(Mth.floor(x), Mth.floor(y), Mth.floor(z));
      VoxelShape here = level.getBlockState(pos).getCollisionShape(level, pos);
      if (!here.isEmpty() && !((double)pos.getY() + here.max(Axis.Y) <= y)) {
         VoxelShape top = here;

         for (int i = 0; i < up; i++) {
            pos.move(Direction.UP);
            VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
            if (shape.isEmpty()) {
               return (double)(pos.getY() - 1) + top.max(Axis.Y);
            }

            top = shape;
         }

         return Double.NaN;
      } else if (!here.isEmpty()) {
         return (double)pos.getY() + here.max(Axis.Y);
      } else {
         for (int i = 0; i < down; i++) {
            pos.move(Direction.DOWN);
            VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
            if (!shape.isEmpty()) {
               return (double)pos.getY() + shape.max(Axis.Y);
            }
         }

         return Double.NaN;
      }
   }

   public static boolean inside(Level level, double x, double y, double z) {
      BlockPos pos = BlockPos.containing(x, y, z);
      VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
      return !shape.isEmpty() && (double)pos.getY() + shape.max(Axis.Y) > y;
   }

   private SunaGround() {
   }
}
