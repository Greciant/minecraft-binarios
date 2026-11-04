package com.awakenedmod.awakened.mixin;

import com.awakenedmod.awakened.content.summoning.SummonMounts;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ServerPlayer.class})
public abstract class SummonMountInventoryMixin {
   @Inject(
      method = {"openHorseInventory"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 1
   )
   private void awakened$keepMountInventoryShut(AbstractHorse horse, Container inventory, CallbackInfo ci) {
      if (SummonMounts.isMount(horse)) {
         ci.cancel();
      }
   }
}
