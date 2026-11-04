package com.awakenedmod.awakened.core.terrain;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public record RemovedBlock(BlockPos pos, BlockState state) {
}
