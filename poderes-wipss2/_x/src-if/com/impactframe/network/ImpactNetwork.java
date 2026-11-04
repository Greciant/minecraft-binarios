package com.impactframe.network;

import com.impactframe.client.FreezeLockBridge;
import com.impactframe.client.ImpactFeedbackBridge;
import com.impactframe.server.ServerFreeze;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(
   modid = "impactframe",
   bus = Bus.MOD
)
public final class ImpactNetwork {
   private static final String PROTOCOL_VERSION = "2";

   private ImpactNetwork() {
   }

   @SubscribeEvent
   public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
      PayloadRegistrar registrar = event.registrar("2").optional();
      registrar.playToServer(FreezeRequestPayload.TYPE, FreezeRequestPayload.CODEC, (payload, context) -> {
         if (context.player() instanceof ServerPlayer player) {
            ServerFreeze.request(player, payload.ticks());
         }
      });
      registrar.playToClient(FreezeStatePayload.TYPE, FreezeStatePayload.CODEC, (payload, context) -> FreezeLockBridge.onFreezeState(payload.active()));
      registrar.playToClient(
         HighDamageAttackPayload.TYPE,
         HighDamageAttackPayload.CODEC,
         (payload, context) -> ImpactFeedbackBridge.onHighDamageAttack(payload.targetId(), payload.damage())
      );
   }
}
