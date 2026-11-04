package com.awakenedmod.awakened.mixin;

import com.awakenedmod.awakened.client.voidview.VoidView;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({EntityRenderer.class})
public abstract class VoidEntityLightMixin {
   @Inject(
      method = {"getPackedLightCoords"},
      at = {@At("RETURN")},
      cancellable = true,
      require = 0
   )
   private void awakened$voidLight(Entity entity, float partialTicks, CallbackInfoReturnable<Integer> cir) {
      if (VoidView.any() && VoidView.contains(entity.getX(), entity.getY(0.5), entity.getZ())) {
         cir.setReturnValue(15728880);
      }
   }
}
