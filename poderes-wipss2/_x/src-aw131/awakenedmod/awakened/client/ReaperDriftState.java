package com.awakenedmod.awakened.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut;

@EventBusSubscriber(
   modid = "awakened",
   value = {Dist.CLIENT}
)
public final class ReaperDriftState {
   private static boolean drifting;

   private ReaperDriftState() {
   }

   public static void set(boolean on) {
      drifting = on;
   }

   public static boolean drifting() {
      return drifting;
   }

   @SubscribeEvent
   public static void onLoggingOut(LoggingOut event) {
      drifting = false;
   }
}
