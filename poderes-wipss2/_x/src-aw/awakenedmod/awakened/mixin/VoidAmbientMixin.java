package com.awakenedmod.awakened.mixin;

import com.awakenedmod.awakened.client.voidview.VoidView;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ClientLevel.class})
public abstract class VoidAmbientMixin {
   @Inject(
      method = {"animateTick"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void awakened$voidAmbient(int x, int y, int z, CallbackInfo ci) {
      if (VoidView.hidesWorld()) {
         ci.cancel();
      }
   }
}
