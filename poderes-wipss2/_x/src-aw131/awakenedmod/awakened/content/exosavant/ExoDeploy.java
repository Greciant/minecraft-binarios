package com.awakenedmod.awakened.content.exosavant;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

public final class ExoDeploy {
   private static final int HAND_CANNON_TICKS = 16;
   private static final int BARRAGE_TAIL_TICKS = 8;
   private static final Map<UUID, Long> HAND_CANNON_UNTIL = new ConcurrentHashMap<>();
   private static final Map<UUID, Long> BARRAGE_UNTIL = new ConcurrentHashMap<>();

   private ExoDeploy() {
   }

   public static void deployHandCannon(ServerPlayer player) {
      HAND_CANNON_UNTIL.put(player.getUUID(), player.serverLevel().getGameTime() + 16L);
   }

   public static boolean handCannonDeploying(ServerPlayer player) {
      Long until = HAND_CANNON_UNTIL.get(player.getUUID());
      return until != null && player.serverLevel().getGameTime() < until;
   }

   public static void deployBarrage(ServerPlayer player, int salvoTicks) {
      BARRAGE_UNTIL.put(player.getUUID(), player.serverLevel().getGameTime() + (long)salvoTicks + 8L);
   }

   public static boolean barraging(ServerPlayer player) {
      Long until = BARRAGE_UNTIL.get(player.getUUID());
      return until != null && player.serverLevel().getGameTime() < until;
   }

   public static void clear(ServerPlayer player) {
      HAND_CANNON_UNTIL.remove(player.getUUID());
      BARRAGE_UNTIL.remove(player.getUUID());
   }
}
