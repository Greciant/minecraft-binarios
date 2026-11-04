package com.awakenedmod.awakened.core.terrain;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.ScaffoldingBlock;
import net.minecraft.world.level.block.SpongeBlock;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.TripWireBlock;
import net.minecraft.world.level.block.WetSpongeBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

final class TerrainSafety {
   private TerrainSafety() {
   }

   static boolean canTake(ServerLevel level, BlockPos pos, BlockState state) {
      if (!state.isAir() && !state.hasBlockEntity() && state.getFluidState().isEmpty()) {
         if (level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos)) {
            return false;
         } else if (state.getDestroySpeed(level, pos) < 0.0F
            || state.is(BlockTags.PORTALS)
            || state.is(BlockTags.WITHER_IMMUNE)
            || state.is(BlockTags.DRAGON_IMMUNE)) {
            return false;
         } else if (PoiTypes.forState(state).isPresent()) {
            return false;
         } else {
            Block block = state.getBlock();
            return !(block instanceof GameMasterBlock) && !hasSideEffects(state, block);
         }
      } else {
         return false;
      }
   }

   private static boolean hasSideEffects(BlockState state, Block block) {
      return state.isSignalSource()
         || state.hasAnalogOutputSignal()
         || block instanceof BaseRailBlock
         || block instanceof RedStoneWireBlock
         || block instanceof DiodeBlock
         || block instanceof ObserverBlock
         || block instanceof PistonBaseBlock
         || block instanceof PistonHeadBlock
         || block instanceof TntBlock
         || block instanceof CarvedPumpkinBlock
         || block instanceof SpongeBlock
         || block instanceof WetSpongeBlock
         || block instanceof ScaffoldingBlock
         || block instanceof TripWireBlock
         || block instanceof BaseFireBlock;
   }

   static boolean isDouble(BlockState state) {
      return state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF);
   }

   static BlockPos partner(BlockPos pos, BlockState state) {
      return state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
   }

   static boolean isPartner(BlockState half, BlockState other) {
      return other.is(half.getBlock())
         && isDouble(other)
         && other.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) != half.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
   }
}
