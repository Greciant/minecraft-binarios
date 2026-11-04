package com.awakenedmod.awakened.mixin;

import com.awakenedmod.awakened.core.PhaseState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ScreenEffectRenderer.class})
public abstract class PhaseScreenMixin {
   @Inject(
      method = {"renderScreenEffect"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void awakened$phaseSight(Minecraft minecraft, PoseStack poseStack, CallbackInfo ci) {
      awakened$cancelIfPhasing(ci);
   }

   private static void awakened$cancelIfPhasing(CallbackInfo ci) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && PhaseState.clientPhasing(mc.player.getId())) {
         ci.cancel();
      }
   }
}
