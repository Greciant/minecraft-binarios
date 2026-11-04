package com.awakenedmod.awakened.client.graviton.hand;

import com.awakenedmod.awakened.client.AwakenedAnimations;
import com.awakenedmod.awakened.core.AwId;
import com.zigythebird.playeranimcore.easing.EasingType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;

@EventBusSubscriber(
   modid = "awakened",
   value = {Dist.CLIENT}
)
public final class HeavyHandClient {
   private static final String[] RELEASES = new String[]{"heavy_hand_yank", "heavy_hand_shove", "heavy_hand_crush", "heavy_hand_launch"};
   private static final int RELEASE_BLEND_IN = 2;
   private static final int HOLD_BLEND_OUT = 4;
   private static boolean declared;

   private HeavyHandClient() {
   }

   @SubscribeEvent
   public static void onClientTick(Post event) {
      if (!declared) {
         declared = true;

         for (String clip : RELEASES) {
            AwakenedAnimations.transitions(AwId.of(clip), 2, EasingType.EASE_OUT_QUAD, 0, EasingType.EASE_IN_OUT_SINE);
         }

         AwakenedAnimations.transitions(AwId.of("heavy_hand"), 0, EasingType.EASE_OUT_QUAD, 4, EasingType.EASE_IN_OUT_SINE);
      }
   }
}
