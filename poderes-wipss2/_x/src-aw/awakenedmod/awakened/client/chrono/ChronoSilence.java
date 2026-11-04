package com.awakenedmod.awakened.client.chrono;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;

public final class ChronoSilence {
   private static final String KIT_PREFIX = "chrono.";

   private ChronoSilence() {
   }

   static void register(IEventBus gameBus) {
      gameBus.addListener(ChronoSilence::onPlaySound);
   }

   public static boolean active() {
      ChronoFxClient.Effect field = ChronoView.governing();
      return field != null && ChronoTimeStop.silent(field.age(0.0F)) || ChronoView.frozen();
   }

   private static void onPlaySound(PlaySoundEvent event) {
      SoundInstance sound = event.getSound();
      if (sound != null && active() && !heard(sound)) {
         event.setSound(null);
      }
   }

   private static boolean heard(SoundInstance sound) {
      if (!(sound instanceof TickableSoundInstance) && !sound.isLooping()) {
         SoundSource source = sound.getSource();
         if (source != SoundSource.PLAYERS && source != SoundSource.MASTER && source != SoundSource.MUSIC && source != SoundSource.RECORDS) {
            ResourceLocation id = sound.getLocation();
            return "awakened".equals(id.getNamespace()) && id.getPath().startsWith("chrono.");
         } else {
            return true;
         }
      } else {
         return true;
      }
   }
}
