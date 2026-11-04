package com.awakenedmod.awakened.core.awaken;

import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.net.CatalystOfferClosedPayload;
import com.awakenedmod.awakened.core.power.Power;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent.Post;
import net.neoforged.neoforge.network.PacketDistributor;

public final class RerollOffers {
   private static final int LIFETIME_TICKS = 6000;
   private static final Map<UUID, RerollOffers.Offer> PENDING = new ConcurrentHashMap<>();

   private RerollOffers() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(RerollOffers::onLogout);
      gameBus.addListener(RerollOffers::onServerTick);
      gameBus.addListener(RerollOffers::onServerStopping);
   }

   public static void put(ServerPlayer player, Power power) {
      PENDING.put(player.getUUID(), new RerollOffers.Offer(power, (long)(player.serverLevel().getServer().getTickCount() + 6000)));
   }

   public static Power pending(UUID playerId) {
      RerollOffers.Offer offer = PENDING.get(playerId);
      return offer != null ? offer.power : null;
   }

   public static boolean has(ServerPlayer player) {
      return PENDING.containsKey(player.getUUID());
   }

   public static Power take(ServerPlayer player) {
      RerollOffers.Offer offer = PENDING.remove(player.getUUID());
      return offer != null ? offer.power : null;
   }

   public static void drop(ServerPlayer player) {
      if (PENDING.remove(player.getUUID()) != null) {
         PacketDistributor.sendToPlayer(player, CatalystOfferClosedPayload.INSTANCE, new CustomPacketPayload[0]);
      }
   }

   private static void onServerTick(Post event) {
      MinecraftServer server = event.getServer();
      if (!PENDING.isEmpty() && server.getTickCount() % 20 == 0) {
         long now = (long)server.getTickCount();
         PENDING.forEach((id, offer) -> {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player == null) {
               PENDING.remove(id, offer);
            } else if (now >= offer.expires) {
               PowerService.answerOffer(player, false);
               PacketDistributor.sendToPlayer(player, CatalystOfferClosedPayload.INSTANCE, new CustomPacketPayload[0]);
            }
         });
      }
   }

   private static void onLogout(PlayerLoggedOutEvent event) {
      PENDING.remove(event.getEntity().getUUID());
   }

   private static void onServerStopping(ServerStoppingEvent event) {
      PENDING.clear();
   }

   private static record Offer(Power power, long expires) {
   }
}
