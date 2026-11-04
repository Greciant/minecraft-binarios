package com.awakenedmod.awakened.mixin;

import com.awakenedmod.awakened.client.PossessCamera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Entity.class})
public abstract class PossessLookMixin {
   @Inject(
      method = {"turn"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void awakened$possessAimInsteadOfBody(double yRot, double xRot, CallbackInfo ci) {
      if (PossessCamera.isActive() && this == Minecraft.getInstance().player) {
         PossessCamera.applyLook(yRot, xRot);
         ci.cancel();
      }
   }
}
