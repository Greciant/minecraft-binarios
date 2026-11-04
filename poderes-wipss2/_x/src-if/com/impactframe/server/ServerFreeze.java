package com.impactframe.server;

import com.impactframe.ImpactCommonConfig;
import com.impactframe.network.FreezeStatePayload;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTickRateManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent.Post;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(
   modid = "impactframe"
)
public final class ServerFreeze {
   private static final Map<UUID, Long> LAST_REQUEST = new HashMap<>();
   private static long serverTicks;
   private static boolean weFroze;
   private static int remainingTicks;

   private ServerFreeze() {
   }

   public static void request(ServerPlayer player, int requestedTicks) {
      MinecraftServer server = player.getServer();
      if (server != null) {
         boolean solo = !server.isDedicatedServer() && server.getPlayerCount() <= 1;
         if (solo || ImpactCommonConfig.allowOnMultiplayer()) {
            long last = LAST_REQUEST.getOrDefault(player.getUUID(), -4611686018427387904L);
            if (serverTicks - last >= (long)ImpactCommonConfig.playerCooldownTicks()) {
               ServerTickRateManager manager = server.tickRateManager();
               if (!manager.isFrozen() && !manager.isSprinting()) {
                  LAST_REQUEST.put(player.getUUID(), serverTicks);
                  remainingTicks = Mth.clamp(requestedTicks, 1, ImpactCommonConfig.maxFreezeTicks());
                  weFroze = true;
                  manager.setFrozen(true);
                  broadcast(server, true);
               }
            }
         }
      }
   }

   @SubscribeEvent
   public static void onServerTickPost(Post event) {
      serverTicks++;
      if (weFroze) {
         ServerTickRateManager manager = event.getServer().tickRateManager();
         if (!manager.isFrozen()) {
            weFroze = false;
            broadcast(event.getServer(), false);
         } else {
            if (--remainingTicks <= 0) {
               weFroze = false;
               manager.setFrozen(false);
               broadcast(event.getServer(), false);
            }
         }
      }
   }

   private static void broadcast(MinecraftServer server, boolean active) {
      FreezeStatePayload payload = new FreezeStatePayload(active);

      for (ServerPlayer target : server.getPlayerList().getPlayers()) {
         if (target.connection.hasChannel(FreezeStatePayload.TYPE)) {
            PacketDistributor.sendToPlayer(target, payload, new CustomPacketPayload[0]);
         }
      }
   }

   @SubscribeEvent
   public static void onPlayerLoggedOut(PlayerLoggedOutEvent event) {
      LAST_REQUEST.remove(event.getEntity().getUUID());
   }

   @SubscribeEvent
   public static void onServerStopping(ServerStoppingEvent event) {
      if (weFroze) {
         event.getServer().tickRateManager().setFrozen(false);
      }

      weFroze = false;
      remainingTicks = 0;
      LAST_REQUEST.clear();
   }
}
