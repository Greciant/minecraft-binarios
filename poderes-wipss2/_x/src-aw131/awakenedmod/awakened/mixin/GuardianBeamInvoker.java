package com.awakenedmod.awakened.mixin;

import net.minecraft.world.entity.monster.Guardian;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({Guardian.class})
public interface GuardianBeamInvoker {
   @Invoker("setActiveAttackTarget")
   void awakened$setActiveAttackTarget(int var1);
}
