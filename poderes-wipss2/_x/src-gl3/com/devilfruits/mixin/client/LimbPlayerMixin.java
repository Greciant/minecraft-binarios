package com.devilfruits.mixin.client;

import com.devilfruits.client.limb.LimbTest;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({AbstractClientPlayer.class})
public abstract class LimbPlayerMixin {
   @Inject(
      method = {"getSkin"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void devilfruits$testSkin(CallbackInfoReturnable<PlayerSkin> cir) {
      PlayerSkin skin = LimbTest.skin((AbstractClientPlayer)this);
      if (skin != null) {
         cir.setReturnValue(skin);
      }
   }
}
