package com.devilfruits.core.util;

import com.devilfruits.DevilFruits;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent.Post;

@EventBusSubscriber(
   modid = "devilfruits"
)
public final class ServerScheduler {
   private static final List<ServerScheduler.Entry> TASKS = new ArrayList<>();
   private static final List<ServerScheduler.Entry> PENDING = new ArrayList<>();

   public static void schedule(ServerLevel level, int delay, ServerScheduler.Task task) {
      PENDING.add(new ServerScheduler.Entry(level, task, delay));
   }

   public static void later(ServerLevel level, int delay, Runnable action) {
      schedule(level, delay, age -> {
         action.run();
         return true;
      });
   }

   @SubscribeEvent
   public static void onServerTick(Post event) {
      if (!PENDING.isEmpty()) {
         TASKS.addAll(PENDING);
         PENDING.clear();
      }

      TASKS.removeIf(entry -> {
         if (entry.level.getServer() != event.getServer()) {
            return true;
         } else if (entry.delay > 0) {
            entry.delay--;
            return false;
         } else {
            try {
               return entry.task.tick(entry.age++);
            } catch (RuntimeException var3) {
               DevilFruits.LOGGER.error("Scheduled ability task failed", var3);
               return true;
            }
         }
      });
   }

   @SubscribeEvent
   public static void onServerStopped(ServerStoppedEvent event) {
      TASKS.clear();
      PENDING.clear();
   }

   private ServerScheduler() {
   }

   private static final class Entry {
      final ServerLevel level;
      final ServerScheduler.Task task;
      int delay;
      int age;

      Entry(ServerLevel level, ServerScheduler.Task task, int delay) {
         this.level = level;
         this.task = task;
         this.delay = delay;
      }
   }

   @FunctionalInterface
   public interface Task {
      boolean tick(int var1);
   }
}
