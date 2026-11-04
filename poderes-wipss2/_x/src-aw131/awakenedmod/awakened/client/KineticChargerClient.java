package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.content.kineticcharger.KineticVisual;
import com.awakenedmod.awakened.core.AwId;
import com.zigythebird.playeranimcore.easing.EasingType;

public final class KineticChargerClient {
   public static final String STOMP_CLIP = "stockpile_stomp";
   private static final int STOMP_FADE_IN_TICKS = 4;
   private static final int STOMP_FADE_OUT_TICKS = 4;

   private KineticChargerClient() {
   }

   public static void register() {
      AwakenedAnimations.holdLoop(AwId.of("stockpile_stomp"), p -> KineticVisual.of(p).stomping());
      AwakenedAnimations.vanillaFirstPerson(AwId.of("stockpile_stomp"), p -> false);
      AwakenedAnimations.transitions(AwId.of("stockpile_stomp"), 4, EasingType.EASE_OUT_QUAD, 4, EasingType.EASE_IN_OUT_SINE);
   }
}
