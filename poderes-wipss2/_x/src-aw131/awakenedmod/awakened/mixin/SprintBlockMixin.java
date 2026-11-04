package com.awakenedmod.awakened.mixin;

import com.awakenedmod.awakened.client.SprintBlock;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({LocalPlayer.class})
public abstract class SprintBlockMixin {
   @Inject(
      method = {"canStartSprinting"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void awakened$sprintBlocked(CallbackInfoReturnable<Boolean> cir) {
      LocalPlayer self = (LocalPlayer)this;
      if (SprintBlock.blocked(self)) {
         if (self.isSprinting()) {
            self.setSprinting(false);
         }

         cir.setReturnValue(false);
      }
   }
}
