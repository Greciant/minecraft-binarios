package com.awakenedmod.awakened.core.terrain;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

final class TerrainWriter {
   private static final int QUIET = 50;

   private TerrainWriter() {
   }

   static void take(ServerLevel level, BlockPos pos) {
      level.setBlock(pos, Blocks.AIR.defaultBlockState(), 50);
   }

   static void put(ServerLevel level, BlockPos pos, BlockState state) {
      level.setBlock(pos, state, 50);
   }

   static boolean live(ServerLevel level, BlockPos pos) {
      return level.shouldTickBlocksAt(ChunkPos.asLong(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ())));
   }
}
