package com.awakenedmod.awakened.content.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;

public interface MultipartThreat {
   boolean hurtPart(BossPart<?> var1, ServerLevel var2, DamageSource var3, float var4);
}
