package com.awakenedmod.awakened.content.ruin;

import com.awakenedmod.awakened.Awakened;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent.Post;

final class RuinFxClock {
   private static final int GATE_FORGET_TICKS = 1200;
   private static final int GATE_PRUNE_EVERY = 200;
   private static final List<RuinFxClock.Job> JOBS = new ArrayList<>();
   private static final Map<String, Long> GATES = new HashMap<>();
   private static boolean registered;
   private static long lastPrune;

   private RuinFxClock() {
   }

   static void every(ServerLevel level, int ticks, IntConsumer step) {
      if (ticks > 0) {
         ensureRegistered();
         JOBS.add(new RuinFxClock.Job(level, ticks, step));
      }
   }

   static void after(ServerLevel level, int ticks, Runnable action) {
      if (ticks <= 0) {
         action.run();
      } else {
         every(level, ticks, t -> {
            if (t == ticks) {
               action.run();
            }
         });
      }
   }

   static boolean once(ServerLevel level, String key, int ticks) {
      long now = level.getGameTime();
      Long last = GATES.get(key);
      if (last != null && now - last < (long)ticks) {
         return false;
      } else {
         GATES.put(key, now);
         return true;
      }
   }

   private static synchronized void ensureRegistered() {
      if (!registered) {
         registered = true;
         NeoForge.EVENT_BUS.addListener(RuinFxClock::onServerTick);
         NeoForge.EVENT_BUS.addListener(RuinFxClock::onServerStopped);
      }
   }

   private static void onServerTick(Post event) {
      long now = event.getServer().overworld().getGameTime();
      if (now - lastPrune >= 200L && !GATES.isEmpty()) {
         lastPrune = now;
         GATES.values().removeIf(at -> now - at > 1200L);
      }

      if (!JOBS.isEmpty()) {
         for (RuinFxClock.Job job : new ArrayList<>(JOBS)) {
            if (job.level.getServer() != event.getServer()) {
               JOBS.remove(job);
            } else {
               job.t++;

               try {
                  job.step.accept(job.t);
               } catch (RuntimeException var6) {
                  Awakened.LOGGER.error("Awakened: a Ruin visual step threw", var6);
                  job.t = job.ticks;
               }

               if (job.t >= job.ticks) {
                  JOBS.remove(job);
               }
            }
         }
      }
   }

   private static void onServerStopped(ServerStoppedEvent event) {
      JOBS.clear();
      GATES.clear();
      lastPrune = 0L;
   }

   private static final class Job {
      final ServerLevel level;
      final int ticks;
      final IntConsumer step;
      int t;

      Job(ServerLevel level, int ticks, IntConsumer step) {
         this.level = level;
         this.ticks = ticks;
         this.step = step;
      }
   }
}
