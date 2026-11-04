package com.awakenedmod.awakened.client;

import net.minecraft.sounds.SoundEvents;
import net.neoforged.bus.api.IEventBus;

public final class PyroJetSound {
   private PyroJetSound() {
   }

   public static void register(IEventBus gameBus) {
      PyroLoopSound.register(
         gameBus,
         AwakenedAnimations::isJetting,
         PyroJetClock::throttle,
         true,
         null,
         new PyroLoopSound.Layer(SoundEvents.ELYTRA_FLYING, 0.6F, 0.7F, 0.25F, 0.4F),
         new PyroLoopSound.Layer(SoundEvents.FIRE_AMBIENT, 0.35F, 0.95F, 0.1F, 0.5F)
      );
   }
}
