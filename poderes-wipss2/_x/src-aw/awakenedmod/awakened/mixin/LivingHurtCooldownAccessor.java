package com.awakenedmod.awakened.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({LivingEntity.class})
public interface LivingHurtCooldownAccessor {
   @Accessor("lastHurt")
   void awakened$setLastHurt(float var1);

   @Accessor("lastHurt")
   float awakened$getLastHurt();
}
