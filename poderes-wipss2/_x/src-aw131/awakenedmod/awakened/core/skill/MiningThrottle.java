package com.awakenedmod.awakened.core.skill;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

public final class MiningThrottle {
   private final int maxPerTick;
   private final Map<UUID, long[]> state = new ConcurrentHashMap<>();

   public MiningThrottle(int maxPerTick) {
      this.maxPerTick = maxPerTick;
   }

   public boolean accept(ServerPlayer player) {
      long tick = player.serverLevel().getGameTime();
      long[] s = this.state.computeIfAbsent(player.getUUID(), k -> new long[]{Long.MIN_VALUE, 0L});
      if (s[0] != tick) {
         s[0] = tick;
         s[1] = 0L;
      }

      if (s[1] >= (long)this.maxPerTick) {
         return false;
      } else {
         s[1]++;
         return true;
      }
   }

   public void clear(UUID id) {
      this.state.remove(id);
   }
}
