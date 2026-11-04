package com.awakenedmod.awakened.mixin;

import com.awakenedmod.awakened.core.SlowMotion;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin({Entity.class})
public abstract class SlowMotionLerpStepMixin {
   @ModifyVariable(
      method = {"lerpPositionAndRotationStep"},
      at = @At("HEAD"),
      argsOnly = true,
      ordinal = 0
   )
   private int awakened$snapWhileSlowed(int steps) {
      return SlowMotion.smoothed((Entity)this) ? 1 : steps;
   }
}
