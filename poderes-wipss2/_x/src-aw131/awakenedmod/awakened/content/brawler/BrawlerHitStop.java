package com.awakenedmod.awakened.content.brawler;

import com.awakenedmod.awakened.core.TickFreeze;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class BrawlerHitStop {
   private static final Map<Integer, BrawlerHitStop.Stop> STOPS = new ConcurrentHashMap<>();
   private static final Map<Integer, Long> MOVING = new ConcurrentHashMap<>();
   private static int lastTick = -1;

   private BrawlerHitStop() {
   }

   public static void apply(LivingEntity target, int ticks) {
      if (target != null && !(target instanceof Player) && target.isAlive() && !target.isRemoved() && ticks > 0 && !moving(target)) {
         BrawlerHitStop.Stop stop = STOPS.get(target.getId());
         if (stop == null) {
            stop = new BrawlerHitStop.Stop(target);
            TickFreeze.freeze(target, true);
            STOPS.put(target.getId(), stop);
         }

         stop.ticksLeft = Math.max(stop.ticksLeft, ticks);
      }
   }

   public static boolean stopped(LivingEntity target) {
      return target != null && STOPS.containsKey(target.getId());
   }

   public static void clear(LivingEntity target) {
      BrawlerHitStop.Stop stop = target == null ? null : STOPS.remove(target.getId());
      if (stop != null) {
         TickFreeze.release(stop.target, true);
      }
   }

   public static void moving(LivingEntity target, int ticks) {
      if (target != null) {
         clear(target);
         if (ticks > 0) {
            MOVING.put(target.getId(), target.level().getGameTime() + (long)ticks);
         } else {
            MOVING.remove(target.getId());
         }
      }
   }

   private static boolean moving(LivingEntity target) {
      Long until = MOVING.get(target.getId());
      if (until == null) {
         return false;
      } else if (target.level().getGameTime() >= until) {
         MOVING.remove(target.getId());
         return false;
      } else {
         return true;
      }
   }

   public static void tickAll(MinecraftServer server) {
      int now = server.getTickCount();
      if (now != lastTick) {
         lastTick = now;
         Iterator<Entry<Integer, BrawlerHitStop.Stop>> it = STOPS.entrySet().iterator();

         while (it.hasNext()) {
            BrawlerHitStop.Stop stop = it.next().getValue();
            boolean gone = !stop.target.isAlive() || stop.target.isRemoved();
            if (gone || --stop.ticksLeft <= 0) {
               it.remove();
               TickFreeze.release(stop.target, true);
            }
         }

         MOVING.values().removeIf(until -> server.overworld().getGameTime() >= until);
      }
   }

   public static void clearAll() {
      for (BrawlerHitStop.Stop stop : STOPS.values()) {
         TickFreeze.release(stop.target, true);
      }

      STOPS.clear();
      MOVING.clear();
      lastTick = -1;
   }

   private static final class Stop {
      final LivingEntity target;
      int ticksLeft;

      Stop(LivingEntity target) {
         this.target = target;
      }
   }
}
