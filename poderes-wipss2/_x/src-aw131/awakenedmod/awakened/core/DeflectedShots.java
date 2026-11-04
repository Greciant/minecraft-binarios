package com.awakenedmod.awakened.core;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent.Post;

@EventBusSubscriber(
   modid = "awakened"
)
public final class DeflectedShots {
   public static final int MEMORY_TICKS = 100;
   private static final int SWEEP_INTERVAL = 20;
   private static final Map<Integer, DeflectedShots.Turn> TURNED = new ConcurrentHashMap<>();

   private DeflectedShots() {
   }

   public static void mark(Entity shot, UUID by) {
      mark(shot, by, 100);
   }

   public static void mark(Entity shot, UUID by, int memoryTicks) {
      long now = shot.level().getGameTime();
      TURNED.put(shot.getId(), new DeflectedShots.Turn(by, now, now + (long)memoryTicks));
   }

   public static boolean turned(Entity shot) {
      return live(shot) != null;
   }

   public static boolean turnedBy(Entity shot, UUID by) {
      DeflectedShots.Turn turn = live(shot);
      return turn != null && turn.by.equals(by);
   }

   public static boolean turnedThisTick(Entity shot) {
      DeflectedShots.Turn turn = live(shot);
      return turn != null && turn.tick == shot.level().getGameTime();
   }

   private static DeflectedShots.Turn live(Entity shot) {
      DeflectedShots.Turn turn = TURNED.get(shot.getId());
      if (turn == null) {
         return null;
      } else if (shot.level().getGameTime() > turn.until) {
         TURNED.remove(shot.getId());
         return null;
      } else {
         return turn;
      }
   }

   @SubscribeEvent
   public static void onServerTick(Post event) {
      if (!TURNED.isEmpty()) {
         long now = event.getServer().overworld().getGameTime();
         if (now % 20L == 0L) {
            TURNED.values().removeIf(turn -> now > turn.until);
         }
      }
   }

   @SubscribeEvent
   public static void onServerStopped(ServerStoppedEvent event) {
      TURNED.clear();
   }

   public static void clearAll() {
      TURNED.clear();
   }

   private static record Turn(UUID by, long tick, long until) {
   }
}
