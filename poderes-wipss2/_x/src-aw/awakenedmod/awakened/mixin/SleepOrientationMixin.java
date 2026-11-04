package com.awakenedmod.awakened.mixin;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({LivingEntity.class})
public abstract class SleepOrientationMixin {
   @Inject(
      method = {"getBedOrientation"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void awakened$noBedNoOrientation(CallbackInfoReturnable<Direction> cir) {
      if (((LivingEntity)this).getSleepingPos().isEmpty()) {
         cir.setReturnValue(null);
      }
   }
}
