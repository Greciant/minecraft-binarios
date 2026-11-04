package com.awakenedmod.awakened.mixin;

import com.awakenedmod.awakened.client.RevealClient;
import com.awakenedmod.awakened.client.voidview.VoidView;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Minecraft.class})
public abstract class MinecraftAppearGlowingMixin {
   @Inject(
      method = {"shouldEntityAppearGlowing"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void awakened$revealForcesGlow(Entity entity, CallbackInfoReturnable<Boolean> cir) {
      if (VoidView.any() && VoidView.separated(VoidView.cameraPos(), entity.getX(), entity.getY(0.5), entity.getZ())) {
         cir.setReturnValue(false);
      } else {
         if (RevealClient.isRevealed(entity)) {
            cir.setReturnValue(true);
         }
      }
   }
}
