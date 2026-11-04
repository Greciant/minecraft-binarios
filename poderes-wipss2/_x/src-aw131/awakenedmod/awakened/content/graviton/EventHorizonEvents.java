package com.awakenedmod.awakened.content.graviton;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

@EventBusSubscriber(
   modid = "awakened"
)
public final class EventHorizonEvents {
   private EventHorizonEvents() {
   }

   @SubscribeEvent
   public static void onServerStopping(ServerStoppingEvent event) {
      EventHorizonField.releaseAll();
   }
}
