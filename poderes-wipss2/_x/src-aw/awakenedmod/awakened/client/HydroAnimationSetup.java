package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.content.hydro.HydroVisual;
import com.awakenedmod.awakened.core.AwId;
import com.zigythebird.playeranimcore.easing.EasingType;

public final class HydroAnimationSetup {
   public static final String SURF_CLIP = "hydro_surf";
   private static final String GRIP_CLIP = "hydro_hook_grip";
   private static final int SURF_IN_TICKS = 4;
   private static final int SURF_OUT_TICKS = 5;
   private static final int GRIP_IN_TICKS = 3;

   private HydroAnimationSetup() {
   }

   public static void register() {
      AwakenedAnimations.holdLoop(AwId.of("hydro_surf"), p -> HydroVisual.of(p).surfing());
      AwakenedAnimations.vanillaFirstPerson(AwId.of("hydro_surf"), p -> false);
      AwakenedAnimations.transitions(AwId.of("hydro_surf"), 4, EasingType.EASE_OUT_QUAD, 5, EasingType.EASE_IN_OUT_SINE);
      AwakenedAnimations.transitions(AwId.of("hydro_hook_grip"), 3, EasingType.EASE_OUT_QUAD, 0, EasingType.LINEAR);
   }
}
