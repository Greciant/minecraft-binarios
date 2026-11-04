package com.devilfruits.core.physics;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public record StructureHit(PhysicsStructure structure, BlockPos localPos, Vec3 worldPoint, Vec3 worldNormal) {
}
