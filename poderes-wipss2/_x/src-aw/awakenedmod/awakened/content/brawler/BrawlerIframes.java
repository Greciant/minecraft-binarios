package com.awakenedmod.awakened.content.brawler;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

final class BrawlerIframes {
   private static final Map<UUID, Long> UNTIL = new ConcurrentHashMap<>();

   private BrawlerIframes() {
   }

   static void grant(ServerPlayer player, int ticks) {
      if (ticks > 0) {
         UNTIL.merge(player.getUUID(), player.serverLevel().getGameTime() + (long)ticks, Math::max);
      }
   }

   static boolean active(ServerPlayer player) {
      Long until = UNTIL.get(player.getUUID());
      if (until == null) {
         return false;
      } else if (player.serverLevel().getGameTime() < until) {
         return true;
      } else {
         UNTIL.remove(player.getUUID());
         return false;
      }
   }

   static void clearPlayer(ServerPlayer player) {
      UNTIL.remove(player.getUUID());
   }

   static void clearAll() {
      UNTIL.clear();
   }
}
