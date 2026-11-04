package com.devilfruits.core.physics;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public record StructureBlock(PhysicsStructure structure, BlockPos localPos, BlockState state, Vec3 worldCenter, double power) {
}
