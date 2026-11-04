package com.awakenedmod.awakened.content.spider;

import com.awakenedmod.awakened.core.PowerService;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

public final class SpiderAnim {
   private static final Map<UUID, Boolean> nextLeft = new ConcurrentHashMap<>();
   private static final Map<UUID, Long> lastThrow = new ConcurrentHashMap<>();
   private static final long RESET_TICKS = 40L;

   private SpiderAnim() {
   }

   public static boolean playThrow(ServerPlayer player) {
      UUID id = player.getUUID();
      long now = player.serverLevel().getGameTime();
      Long last = lastThrow.get(id);
      boolean left = last != null && now - last <= 40L && Boolean.TRUE.equals(nextLeft.get(id));
      nextLeft.put(id, !left);
      lastThrow.put(id, now);
      PowerService.playAnimation(player, left ? "web_shot_left" : "web_shot");
      return left;
   }

   public static void clear(ServerPlayer player) {
      nextLeft.remove(player.getUUID());
      lastThrow.remove(player.getUUID());
   }
}
