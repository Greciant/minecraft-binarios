package com.awakenedmod.awakened.mixin;

import com.awakenedmod.awakened.client.SoundDuck;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({SoundEngine.class})
public abstract class SoundDuckMixin {
   @Inject(
      method = {"calculateVolume(Lnet/minecraft/client/resources/sounds/SoundInstance;)F"},
      at = {@At("RETURN")},
      cancellable = true,
      require = 0
   )
   private void awakened$soundDuck(SoundInstance sound, CallbackInfoReturnable<Float> cir) {
      float gain = SoundDuck.gain(sound);
      if (gain < 1.0F) {
         cir.setReturnValue(cir.getReturnValueF() * gain);
      }
   }

   @ModifyExpressionValue(
      method = {"play"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/sounds/SoundEngine;calculateVolume(FLnet/minecraft/sounds/SoundSource;)F"
      )},
      require = 0
   )
   private float awakened$soundDuckStart(float volume, @Local(argsOnly = true) SoundInstance sound) {
      return volume * SoundDuck.gain(sound);
   }
}
