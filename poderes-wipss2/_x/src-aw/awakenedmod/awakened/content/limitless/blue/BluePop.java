package com.awakenedmod.awakened.content.limitless.blue;

import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public record BluePop(Vec3 center, List<LivingEntity> bodies) {
}
