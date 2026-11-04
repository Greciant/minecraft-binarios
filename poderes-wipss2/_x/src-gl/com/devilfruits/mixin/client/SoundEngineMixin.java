package com.devilfruits.mixin.client;

import com.devilfruits.client.vfx.VfxAudio;
import javax.annotation.Nullable;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({SoundEngine.class})
public abstract class SoundEngineMixin {
   @Inject(
      method = {"getVolume"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void devilfruits$duck(@Nullable SoundSource category, CallbackInfoReturnable<Float> cir) {
      if (category != null) {
         float f = VfxAudio.factor(category);
         if (f < 1.0F) {
            cir.setReturnValue(cir.getReturnValueF() * f);
         }
      }
   }
}
