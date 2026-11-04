package com.devilfruits.fruits.gomu_gomu.strike;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

public record Breaker(double maxHardness, double maxResistance, double radius, int maxBlocks) {
   public boolean breaks(BlockGetter level, BlockPos pos, BlockState state) {
      float hardness = state.getDestroySpeed(level, pos);
      return !state.isAir()
         && hardness >= 0.0F
         && (double)hardness <= this.maxHardness
         && (double)state.getExplosionResistance(level, pos, null) <= this.maxResistance;
   }
}
