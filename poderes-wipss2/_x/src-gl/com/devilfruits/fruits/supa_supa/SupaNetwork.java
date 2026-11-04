package com.devilfruits.fruits.supa_supa;

import com.devilfruits.fruits.supa_supa.ability.BladeSkatesAbility;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class SupaNetwork {
   public static void register(RegisterPayloadHandlersEvent event) {
      PayloadRegistrar registrar = event.registrar("10");
      registrar.playToServer(SupaNetwork.LungePayload.TYPE, SupaNetwork.LungePayload.STREAM_CODEC, (payload, ctx) -> {
         if (ctx.player() instanceof ServerPlayer player) {
            BladeSkatesAbility.lunge(player);
         }
      });
   }

   private SupaNetwork() {
   }

   public static record LungePayload() implements CustomPacketPayload {
      public static final Type<SupaNetwork.LungePayload> TYPE = new Type(SupaSupa.id("lunge"));
      public static final StreamCodec<ByteBuf, SupaNetwork.LungePayload> STREAM_CODEC = StreamCodec.unit(new SupaNetwork.LungePayload());

      public Type<? extends CustomPacketPayload> type() {
         return TYPE;
      }
   }
}
