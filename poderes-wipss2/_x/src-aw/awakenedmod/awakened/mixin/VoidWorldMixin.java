package com.awakenedmod.awakened.mixin;

import com.awakenedmod.awakened.core.voidview.VoidZones;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Entity.class})
public abstract class VoidWorldMixin {
   @Inject(
      method = {"checkInsideBlocks"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void awakened$voidNoBlocks(CallbackInfo ci) {
      if (VoidZones.inside((Entity)this)) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"updateFluidHeightAndDoFluidPushing()V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void awakened$voidNoFluid(CallbackInfo ci) {
      if (VoidZones.inside((Entity)this)) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"isInWall"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void awakened$voidNoWall(CallbackInfoReturnable<Boolean> cir) {
      if (VoidZones.inside((Entity)this)) {
         cir.setReturnValue(false);
      }
   }
}
