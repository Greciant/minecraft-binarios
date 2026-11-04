package com.devilfruits.fruits.yomi_yomi;

import com.devilfruits.fruits.yomi_yomi.client.YomiClient;
import com.devilfruits.fruits.yomi_yomi.entity.PlayerCorpse;
import com.devilfruits.fruits.yomi_yomi.soul.SoulPhase;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class YomiNetwork {
   public static void openVesselMenu(ServerPlayer player, PlayerCorpse corpse) {
      PacketDistributor.sendToPlayer(player, new YomiNetwork.OpenVesselMenu(corpse.getId()), new CustomPacketPayload[0]);
   }

   public static void register(RegisterPayloadHandlersEvent event) {
      PayloadRegistrar registrar = event.registrar("11");
      registrar.playToClient(
         YomiNetwork.OpenVesselMenu.TYPE, YomiNetwork.OpenVesselMenu.STREAM_CODEC, (payload, ctx) -> YomiClient.openVesselMenu(payload.corpse())
      );
      registrar.playToServer(
         YomiNetwork.ChooseVessel.TYPE,
         YomiNetwork.ChooseVessel.STREAM_CODEC,
         (payload, ctx) -> {
            if (ctx.player() instanceof ServerPlayer player
               && payload.vessel() >= 0
               && payload.vessel() < Vessel.values().length
               && player.level().getEntity(payload.corpse()) instanceof PlayerCorpse body) {
               SoulPhase.beginPossession(player, body, Vessel.values()[payload.vessel()]);
            }
         }
      );
   }

   private YomiNetwork() {
   }

   public static record ChooseVessel(int corpse, int vessel) implements CustomPacketPayload {
      public static final Type<YomiNetwork.ChooseVessel> TYPE = new Type(YomiYomi.id("choose_vessel"));
      public static final StreamCodec<ByteBuf, YomiNetwork.ChooseVessel> STREAM_CODEC = StreamCodec.composite(
         ByteBufCodecs.VAR_INT, YomiNetwork.ChooseVessel::corpse, ByteBufCodecs.VAR_INT, YomiNetwork.ChooseVessel::vessel, YomiNetwork.ChooseVessel::new
      );

      public Type<? extends CustomPacketPayload> type() {
         return TYPE;
      }
   }

   public static record OpenVesselMenu(int corpse) implements CustomPacketPayload {
      public static final Type<YomiNetwork.OpenVesselMenu> TYPE = new Type(YomiYomi.id("open_vessel_menu"));
      public static final StreamCodec<ByteBuf, YomiNetwork.OpenVesselMenu> STREAM_CODEC = ByteBufCodecs.VAR_INT
         .map(YomiNetwork.OpenVesselMenu::new, YomiNetwork.OpenVesselMenu::corpse);

      public Type<? extends CustomPacketPayload> type() {
         return TYPE;
      }
   }
}
