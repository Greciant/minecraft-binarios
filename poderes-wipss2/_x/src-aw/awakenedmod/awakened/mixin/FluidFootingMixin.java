package com.awakenedmod.awakened.mixin;

import com.awakenedmod.awakened.core.FluidFooting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({LivingEntity.class})
public abstract class FluidFootingMixin {
   @Inject(
      method = {"canStandOnFluid"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void awakened$fluidFooting(FluidState fluid, CallbackInfoReturnable<Boolean> cir) {
      if (FluidFooting.canStand((LivingEntity)this, fluid)) {
         cir.setReturnValue(true);
      }
   }
}
