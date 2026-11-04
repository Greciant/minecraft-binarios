package com.awakenedmod.awakened.mixin;

import com.awakenedmod.awakened.client.ClickClaims;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {Minecraft.class},
   priority = 500
)
public abstract class ClickClaimFirstMixin {
   @Inject(
      method = {"startAttack"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void awakened$claimBeforeCombatMods(CallbackInfoReturnable<Boolean> cir) {
      if (ClickClaims.takeFirst()) {
         cir.setReturnValue(false);
      }
   }

   @Inject(
      method = {"continueAttack"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void awakened$holdClaimedBeforeCombatMods(boolean leftClick, CallbackInfo ci) {
      if (ClickClaims.holdClaimed()) {
         ci.cancel();
      }
   }
}
