package com.devilfruits.client.animation;

import net.minecraft.client.player.AbstractClientPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.neoforged.neoforge.client.event.RenderPlayerEvent.Post;
import net.neoforged.neoforge.client.event.RenderPlayerEvent.Pre;

@EventBusSubscriber(
   modid = "devilfruits",
   value = {Dist.CLIENT}
)
public final class AnimationEvents {
   private static int pushedFor = Integer.MIN_VALUE;

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   public static void onRenderPlayerPre(Pre event) {
      if (!event.isCanceled()) {
         if (event.getEntity() instanceof AbstractClientPlayer player && AnimationController.applyRoot(event.getPoseStack(), player, event.getPartialTick())) {
            pushedFor = player.getId();
         }
      }
   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public static void onRenderPlayerPost(Post event) {
      if (pushedFor == event.getEntity().getId()) {
         event.getPoseStack().popPose();
         pushedFor = Integer.MIN_VALUE;
      }
   }

   @SubscribeEvent
   public static void onClientTick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
      AnimationController.tick();
   }

   @SubscribeEvent
   public static void onLogout(LoggingOut event) {
      AnimationController.clear();
   }

   private AnimationEvents() {
   }
}
