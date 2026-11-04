package com.awakenedmod.awakened.core.terrain;

import net.minecraft.core.BlockPos;

final class TerrainColumns {
   private TerrainColumns() {
   }

   static long key(BlockPos pos) {
      return BlockPos.asLong(pos.getX(), 0, pos.getZ());
   }

   static long key(long packed) {
      return BlockPos.asLong(BlockPos.getX(packed), 0, BlockPos.getZ(packed));
   }
}
