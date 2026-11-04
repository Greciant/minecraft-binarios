package com.awakenedmod.awakened.mixin;

import com.awakenedmod.awakened.client.SolarHealthState;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LocalPlayer.class})
public abstract class SolarHurtSuppressMixin {
   @Shadow
   private boolean flashOnSetHealth;

   @Inject(
      method = {"hurtTo"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void awakened$suppressSunHealthClamp(float health, CallbackInfo ci) {
      if (SolarHealthState.consumeBenign()) {
         ((LocalPlayer)this).setHealth(health);
         this.flashOnSetHealth = true;
         ci.cancel();
      }
   }
}
