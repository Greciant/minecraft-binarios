package com.awakenedmod.awakened.mixin;

import com.awakenedmod.awakened.client.EventHorizonClient;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({GameRenderer.class})
public abstract class GameRendererDarkenMixin {
   @Inject(
      method = {"getDarkenWorldAmount"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void awakened$holeDarken(float partialTick, CallbackInfoReturnable<Float> cir) {
      float ours = EventHorizonClient.darken(partialTick);
      if (ours > cir.getReturnValueF()) {
         cir.setReturnValue(ours);
      }
   }
}
