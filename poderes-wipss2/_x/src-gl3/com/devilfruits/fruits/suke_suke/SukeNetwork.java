package com.devilfruits.fruits.suke_suke;

import com.devilfruits.fruits.suke_suke.clear.ShipView;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class SukeNetwork {
   public static void register(RegisterPayloadHandlersEvent event) {
      PayloadRegistrar registrar = event.registrar("11");
      registrar.playToClient(SukeNetwork.ShipPayload.TYPE, SukeNetwork.ShipPayload.STREAM_CODEC, (payload, ctx) -> ShipView.accept(payload));
   }

   public static void toLevel(ServerLevel level, SukeNetwork.ShipPayload payload) {
      PacketDistributor.sendToPlayersInDimension(level, payload, new CustomPacketPayload[0]);
   }

   public static void toPlayer(ServerPlayer player, SukeNetwork.ShipPayload payload) {
      PacketDistributor.sendToPlayer(player, payload, new CustomPacketPayload[0]);
   }

   private SukeNetwork() {
   }

   public static record ShipPayload(UUID ship, UUID group, long start, long end) implements CustomPacketPayload {
      public static final Type<SukeNetwork.ShipPayload> TYPE = new Type(SukeSuke.id("ship"));
      public static final StreamCodec<ByteBuf, SukeNetwork.ShipPayload> STREAM_CODEC = StreamCodec.composite(
         UUIDUtil.STREAM_CODEC,
         SukeNetwork.ShipPayload::ship,
         UUIDUtil.STREAM_CODEC,
         SukeNetwork.ShipPayload::group,
         ByteBufCodecs.VAR_LONG,
         SukeNetwork.ShipPayload::start,
         ByteBufCodecs.VAR_LONG,
         SukeNetwork.ShipPayload::end,
         SukeNetwork.ShipPayload::new
      );

      public Type<? extends CustomPacketPayload> type() {
         return TYPE;
      }
   }
}
