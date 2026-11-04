package com.awakenedmod.awakened.content.limitless;

import com.awakenedmod.awakened.client.limitless.LimitlessClientPackets;
import com.awakenedmod.awakened.core.net.AwakenedNetworking;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(
   modid = "awakened",
   bus = Bus.MOD
)
public final class LimitlessNetworking {
   private static final String VERSION = "limitless-1";

   private LimitlessNetworking() {
   }

   @SubscribeEvent
   public static void register(RegisterPayloadHandlersEvent event) {
      PayloadRegistrar registrar = event.registrar("limitless-1");
      if (FMLEnvironment.dist == Dist.CLIENT) {
         LimitlessClientPackets.register(registrar);
      } else {
         AwakenedNetworking.serverToClient(registrar, InfinityStopPayload.TYPE, InfinityStopPayload.STREAM_CODEC);
      }
   }
}
