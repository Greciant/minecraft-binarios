package com.devilfruits.compat.sable.mixin;

import com.devilfruits.compat.sable.client.SableShipLook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   targets = {"com.simibubi.create.content.contraptions.render.ContraptionVisual"},
   remap = false
)
public abstract class CreateContraptionVisualMixin {
   @Inject(
      method = {"beginFrame"},
      at = {@At(
         value = "INVOKE",
         shift = Shift.AFTER,
         target = "Lcom/simibubi/create/content/contraptions/render/ContraptionVisual;setEmbeddingMatrices(F)V"
      )}
   )
   private void devilfruits$hiddenShip(CallbackInfo ci) {
      SableShipLook.collapseContraption(this);
   }
}
