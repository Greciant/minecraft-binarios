package com.awakenedmod.awakened.content.kineticcharger;

import com.awakenedmod.awakened.core.PowerService;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

final class Stockpile {
   private static final int START_TICKS = 20;
   private static final int STOMP_EVERY = 10;
   private static final float PER_STOMP = 2.0F;
   private static final double STILL_EPS = 0.01;
   private static final int LOOP_LEAD_TICKS = 10;
   private static final int LIT_AT = 10;
   private static final Map<UUID, int[]> STANCES = new ConcurrentHashMap<>();

   private Stockpile() {
   }

   static void tick(ServerPlayer player, boolean busy) {
      if (!busy && player.isShiftKeyDown() && player.onGround() && !player.isPassenger() && !(player.getKnownMovement().horizontalDistance() > 0.01)) {
         int[] stance = STANCES.computeIfAbsent(player.getUUID(), id -> new int[]{0, 0});
         stance[0]++;
         if (stance[0] == 10) {
            KineticFx.stockpileStart(player);
         }

         if (stance[0] >= 20 && (stance[0] - 20) % 10 == 0) {
            stance[1]++;
            KineticBank.bank(player, PowerService.data(player), 2.0F);
            KineticFx.stomp(player, stance[1]);
         }
      } else {
         end(player, STANCES.remove(player.getUUID()));
      }
   }

   static void forget(ServerPlayer player) {
      if (player != null) {
         end(player, STANCES.remove(player.getUUID()));
      }
   }

   private static void end(ServerPlayer player, int[] stance) {
      if (stance != null && stance[0] >= 10) {
         KineticFx.stockpileEnd(player);
      }
   }
}
