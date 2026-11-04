package com.awakenedmod.awakened.client.limitless.domain;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance.Attenuation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

final class VoidTone extends AbstractSoundInstance {
   private VoidTone(SoundEvent sound, float volume, float pitch) {
      super(sound, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
      this.volume = volume;
      this.pitch = pitch;
      this.relative = true;
      this.attenuation = Attenuation.NONE;
      this.x = 0.0;
      this.y = 0.0;
      this.z = 0.0;
   }

   static void play(Minecraft mc, SoundEvent sound, float volume, float pitch) {
      mc.getSoundManager().play(new VoidTone(sound, volume, pitch));
   }
}
