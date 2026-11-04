package com.devilfruits.fruits.kage_kage;

import com.devilfruits.fruits.kage_kage.ability.DoppelmanAbility;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class KageNetwork {
   public static void register(RegisterPayloadHandlersEvent event) {
      PayloadRegistrar registrar = event.registrar("11");
      registrar.playToServer(KageNetwork.SwapPayload.TYPE, KageNetwork.SwapPayload.STREAM_CODEC, (payload, ctx) -> {
         if (ctx.player() instanceof ServerPlayer player) {
            DoppelmanAbility.swap(player);
         }
      });
   }

   private KageNetwork() {
   }

   public static record SwapPayload() implements CustomPacketPayload {
      public static final Type<KageNetwork.SwapPayload> TYPE = new Type(KageKage.id("doppel_swap"));
      public static final StreamCodec<ByteBuf, KageNetwork.SwapPayload> STREAM_CODEC = StreamCodec.unit(new KageNetwork.SwapPayload());

      public Type<? extends CustomPacketPayload> type() {
         return TYPE;
      }
   }
}
