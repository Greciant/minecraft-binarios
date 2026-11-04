package com.awakenedmod.awakened.client.chrono.ending;

import com.awakenedmod.awakened.client.chrono.ChronoFxClient;

public record StopMoment(float r, float since) {
   public static StopMoment of(ChronoFxClient.Effect field, float partialTick) {
      return new StopMoment(field.resolveAge(partialTick), field.sinceEnd(partialTick));
   }

   public boolean released() {
      return this.since >= 0.0F;
   }
}
