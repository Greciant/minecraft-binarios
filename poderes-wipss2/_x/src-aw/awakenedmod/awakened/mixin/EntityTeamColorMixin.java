package com.awakenedmod.awakened.mixin;

import com.awakenedmod.awakened.client.RevealClient;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Entity.class})
public abstract class EntityTeamColorMixin {
   private static final int AWAKENED_REVEAL_RED = 16711680;

   @Inject(
      method = {"getTeamColor"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void awakened$revealColor(CallbackInfoReturnable<Integer> cir) {
      if (RevealClient.isRevealed((Entity)this)) {
         cir.setReturnValue(16711680);
      }
   }
}
