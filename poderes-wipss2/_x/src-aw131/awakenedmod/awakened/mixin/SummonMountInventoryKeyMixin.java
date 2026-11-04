package com.awakenedmod.awakened.mixin;

import com.awakenedmod.awakened.content.summoning.SummonMounts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({MultiPlayerGameMode.class})
public abstract class SummonMountInventoryKeyMixin {
   @Shadow
   @Final
   private Minecraft minecraft;

   @Inject(
      method = {"isServerControlledInventory"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void awakened$ownInventoryOnMount(CallbackInfoReturnable<Boolean> cir) {
      if (this.minecraft.player != null && SummonMounts.isMount(this.minecraft.player.getVehicle())) {
         cir.setReturnValue(false);
      }
   }
}
