package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.content.spider.SpiderTrick;
import com.awakenedmod.awakened.content.spider.WebSwingAbility;
import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.data.SpiderVisual;
import com.zigythebird.playeranimcore.easing.EasingType;

public final class SpiderAnimationSetup {
   private static final int FREEFALL_IN_TICKS = 10;
   private static final int FREEFALL_OUT_TICKS = 4;
   private static final int GRIP_IN_TICKS = 5;
   private static final int CATCH_IN_TICKS = 8;

   private SpiderAnimationSetup() {
   }

   public static void register() {
      for (SpiderTrick trick : SpiderTrick.values()) {
         AwakenedAnimations.vanillaFirstPerson(AwId.of(trick.animation()), p -> false);
      }

      AwakenedAnimations.holdLoop(AwId.of("web_freefall"), p -> ((SpiderVisual)p.getData(AwakenedAttachments.SPIDER_VISUAL)).diving());
      AwakenedAnimations.vanillaFirstPerson(AwId.of("web_freefall"), p -> false);
      AwakenedAnimations.transitions(AwId.of("web_freefall"), 10, EasingType.EASE_IN_OUT_SINE, 4, EasingType.EASE_IN_OUT_SINE);

      for (boolean left : new boolean[]{false, true}) {
         AwakenedAnimations.transitions(AwId.of(WebSwingAbility.gripClip(left)), 5, EasingType.EASE_OUT_QUAD, 0, EasingType.LINEAR);
         AwakenedAnimations.transitions(AwId.of(WebSwingAbility.catchClip(left)), 8, EasingType.EASE_IN_OUT_SINE, 0, EasingType.LINEAR);
      }
   }
}
