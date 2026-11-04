package com.awakenedmod.awakened.core;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class LightBlocks {
   private LightBlocks() {
   }

   public static boolean placeInAir(ServerLevel level, BlockPos pos) {
      return !level.getBlockState(pos).isAir() ? false : level.setBlockAndUpdate(pos, Blocks.LIGHT.defaultBlockState());
   }

   public static void remove(ServerLevel level, BlockPos pos) {
      BlockState state = level.getBlockState(pos);
      if (state.is(Blocks.LIGHT)) {
         level.setBlockAndUpdate(pos, state.getValue(LightBlock.WATERLOGGED) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState());
      }
   }
}
