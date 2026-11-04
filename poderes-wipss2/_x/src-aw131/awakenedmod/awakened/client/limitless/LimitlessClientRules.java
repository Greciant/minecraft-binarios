package com.awakenedmod.awakened.client.limitless;

import com.awakenedmod.awakened.client.AwakenedAnimations;
import com.awakenedmod.awakened.client.SprintBlock;
import com.awakenedmod.awakened.content.limitless.LimitlessCast;
import com.awakenedmod.awakened.content.limitless.purple.PurpleCast;
import com.awakenedmod.awakened.core.AwId;
import com.zigythebird.playeranimcore.easing.EasingType;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;

@EventBusSubscriber(
   modid = "awakened",
   value = {Dist.CLIENT}
)
public final class LimitlessClientRules {
   private static final String[] HOLDS = new String[]{"limitless_blue_hold", "limitless_red_point", "limitless_purple_charge", "limitless_rct"};
   private static final int HOLD_IN = 3;
   private static final int HOLD_OUT = 4;
   private static final String[] RELEASES = new String[]{"limitless_red_recoil", "limitless_purple_release"};
   private static final int RELEASE_IN = 2;
   private static boolean declared;

   private LimitlessClientRules() {
   }

   @SubscribeEvent
   public static void onClientTick(Post event) {
      if (!declared) {
         declared = true;

         for (String clip : HOLDS) {
            AwakenedAnimations.transitions(AwId.of(clip), 3, EasingType.EASE_OUT_QUAD, 4, EasingType.EASE_IN_OUT_SINE);
         }

         for (String clip : RELEASES) {
            AwakenedAnimations.transitions(AwId.of(clip), 2, EasingType.EASE_OUT_QUAD, 0, EasingType.EASE_IN_OUT_SINE);
         }

         AwakenedAnimations.vanillaFirstPerson(AwId.of("limitless_rct"), p -> false);
         AwakenedAnimations.vanillaFirstPerson(AwId.of("limitless_void_float"), p -> false);
         SprintBlock.add(p -> LimitlessCast.of(p, LimitlessCast.RCT).live());
         SprintBlock.add(LimitlessClientRules::overcharging);
      }
   }

   private static boolean overcharging(Player p) {
      PurpleCast cast = PurpleCast.of(p);
      return cast.live() && cast.over() && p.level().getGameTime() - (long)cast.start() >= 50L;
   }
}
