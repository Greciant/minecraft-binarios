package com.awakenedmod.awakened.content.gamma;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class GammaBeats {
   private static final Map<UUID, List<GammaBeats.Beat>> PENDING = new ConcurrentHashMap<>();

   private GammaBeats() {
   }

   public static void schedule(ServerPlayer player, int delayTicks, Runnable fx) {
      long at = player.serverLevel().getGameTime() + (long)Math.max(0, delayTicks);
      PENDING.computeIfAbsent(player.getUUID(), k -> new ArrayList<>()).add(new GammaBeats.Beat(at, player, player.serverLevel(), fx));
   }

   public static void tick(ServerPlayer player) {
      List<GammaBeats.Beat> beats = PENDING.get(player.getUUID());
      if (beats != null && !beats.isEmpty()) {
         long now = player.serverLevel().getGameTime();
         List<Runnable> due = new ArrayList<>();
         Iterator<GammaBeats.Beat> it = beats.iterator();

         while (it.hasNext()) {
            GammaBeats.Beat beat = it.next();
            if (beat.who() != player || beat.level() != player.serverLevel()) {
               it.remove();
            } else if (now >= beat.at()) {
               it.remove();
               due.add(beat.fx());
            }
         }

         if (beats.isEmpty()) {
            PENDING.remove(player.getUUID());
         }

         due.forEach(Runnable::run);
      }
   }

   public static void clear(UUID playerId) {
      PENDING.remove(playerId);
   }

   private static record Beat(long at, ServerPlayer who, ServerLevel level, Runnable fx) {
   }
}
