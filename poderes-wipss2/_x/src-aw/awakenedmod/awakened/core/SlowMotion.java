package com.awakenedmod.awakened.core;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent.Unload;

@EventBusSubscriber(
   modid = "awakened"
)
public final class SlowMotion {
   private static final long SWEEP_INTERVAL = 200L;
   private static final long STALE_AFTER = 40L;
   private static final Map<Integer, SlowMotion.Body> BODIES = new ConcurrentHashMap<>();
   private static volatile long lastSweep;

   private SlowMotion() {
   }

   public static boolean beginClientTick(Entity entity) {
      int divisor = tickDivisor(entity);
      if (divisor < 2) {
         if (BODIES.remove(entity.getId()) != null) {
            restoreInterpolation(entity);
         }

         return true;
      } else {
         long now = entity.level().getGameTime();
         sweep(now);
         SlowMotion.Body body = BODIES.computeIfAbsent(entity.getId(), id -> new SlowMotion.Body());
         body.seen = now;
         if (body.divisor != divisor) {
            body.divisor = divisor;
            body.phase = 0;
            body.skipping = false;
            shortenInterpolation(entity);
            return true;
         } else {
            body.phase = (body.phase + 1) % divisor;
            body.skipping = body.phase != 0;
            return !holdingBack(entity);
         }
      }
   }

   public static boolean skipping(Entity entity) {
      SlowMotion.Body body = BODIES.get(entity.getId());
      return body != null && body.skipping && !entity.isPassenger();
   }

   public static boolean holdingBack(Entity entity) {
      return skipping(entity) && !owned(entity);
   }

   public static boolean smoothed(Entity entity) {
      return tickDivisor(entity) > 1 && !owned(entity);
   }

   private static int tickDivisor(Entity entity) {
      return entity instanceof Player ? 1 : TickFreeze.divisorOf(entity);
   }

   private static boolean owned(Entity entity) {
      if (!entity.isVehicle()) {
         return false;
      } else {
         for (Entity rider : entity.getIndirectPassengers()) {
            if (rider instanceof Player player && player.isLocalPlayer()) {
               return true;
            }
         }

         return false;
      }
   }

   public static float partialTick(Entity entity, float partialTick) {
      SlowMotion.Body body = BODIES.get(entity.getId());
      if (body != null && body.divisor >= 2 && !entity.isPassenger() && !owned(entity)) {
         float stretched = ((float)body.phase + partialTick) / (float)body.divisor;
         return stretched < 0.0F ? 0.0F : Math.min(stretched, 1.0F);
      } else {
         return partialTick;
      }
   }

   private static void shortenInterpolation(Entity entity) {
      if (!owned(entity)) {
         ;
      }
   }

   private static void restoreInterpolation(Entity entity) {
   }

   private static void sweep(long now) {
      if (now - lastSweep >= 200L) {
         lastSweep = now;
         Iterator<Entry<Integer, SlowMotion.Body>> it = BODIES.entrySet().iterator();

         while (it.hasNext()) {
            if (now - it.next().getValue().seen > 40L) {
               it.remove();
            }
         }
      }
   }

   @SubscribeEvent
   public static void onLevelUnload(Unload event) {
      if (event.getLevel().isClientSide()) {
         BODIES.clear();
         lastSweep = 0L;
      }
   }

   private static final class Body {
      int divisor;
      int phase;
      boolean skipping;
      long seen;
   }
}
