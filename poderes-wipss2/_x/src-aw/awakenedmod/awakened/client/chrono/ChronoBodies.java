package com.awakenedmod.awakened.client.chrono;

import com.awakenedmod.awakened.content.chronokinetic.ChronoHeld;
import com.awakenedmod.awakened.core.net.ChronoFxPayload;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class ChronoBodies {
   private static final int NO_FLICKER = Integer.MAX_VALUE;
   private static final Map<Integer, ChronoBodies.Timed> TIMED = new HashMap<>();

   private ChronoBodies() {
   }

   public static void negative(int entityId, int ticks) {
      if (ticks > 0) {
         TIMED.put(entityId, new ChronoBodies.Timed(ChronoBodies.Mode.NEGATIVE, ChronoFxClient.now(), 0, ticks, Integer.MAX_VALUE));
      }
   }

   public static void negative(int entityId, int ticks, int flicker) {
      if (ticks > 0) {
         TIMED.put(entityId, new ChronoBodies.Timed(ChronoBodies.Mode.NEGATIVE, ChronoFxClient.now(), 0, ticks + Math.max(0, flicker), ticks));
      }
   }

   public static void flicker(int entityId, int ticks) {
      if (ticks > 0) {
         TIMED.put(entityId, new ChronoBodies.Timed(ChronoBodies.Mode.NEGATIVE, ChronoFxClient.now(), 0, ticks, 0));
      }
   }

   private static boolean flickerOn(float sinceFlicker) {
      int phase = (int)sinceFlicker;
      return phase == 1 || phase == 3;
   }

   public static void develop(int entityId, int ticks, int delay) {
      ChronoBodies.Timed running = TIMED.get(entityId);
      if (ticks > 0 && (running == null || running.mode != ChronoBodies.Mode.NEGATIVE)) {
         TIMED.put(entityId, new ChronoBodies.Timed(ChronoBodies.Mode.DEVELOP, ChronoFxClient.now(), Math.max(0, delay), ticks, Integer.MAX_VALUE));
      }
   }

   public static ChronoBodies.Look look(Entity entity) {
      int id = entity.getId();
      ChronoBodies.Timed timed = TIMED.isEmpty() ? null : TIMED.get(id);
      if (timed != null) {
         float t = (float)(ChronoFxClient.now() - timed.start - timed.delay) + ChronoFxClient.partialTick();
         if (t >= 0.0F && t < (float)timed.length) {
            if (timed.mode == ChronoBodies.Mode.NEGATIVE) {
               if (!(t < (float)timed.flickerFrom) && !flickerOn(t - (float)timed.flickerFrom)) {
                  return ChronoHeld.isHeld(entity) ? new ChronoBodies.Look(ChronoBodies.Mode.NEGATIVE, 1.0F, id) : null;
               }

               return new ChronoBodies.Look(ChronoBodies.Mode.NEGATIVE, 1.0F, id);
            }

            if (!ChronoHeld.isHeld(entity)) {
               return new ChronoBodies.Look(ChronoBodies.Mode.DEVELOP, 1.0F - Mth.clamp(t / (float)timed.length, 0.0F, 1.0F), id);
            }
         }
      }

      return ChronoHeld.isHeld(entity) && !coveredByField(entity) ? new ChronoBodies.Look(ChronoBodies.Mode.NEGATIVE, 1.0F, id) : null;
   }

   private static boolean coveredByField(Entity entity) {
      Vec3 middle = entity.position().add(0.0, (double)entity.getBbHeight() * 0.5, 0.0);
      float partialTick = ChronoFxClient.partialTick();

      for (ChronoFxClient.Effect e : ChronoFxClient.effects(ChronoFxPayload.Kind.TIME_STOP)) {
         if (ChronoNegative.covers(e, middle, e.age(partialTick))) {
            return true;
         }
      }

      return false;
   }

   static void tick() {
      int now = ChronoFxClient.now();
      Iterator<ChronoBodies.Timed> it = TIMED.values().iterator();

      while (it.hasNext()) {
         ChronoBodies.Timed t = it.next();
         if (now - t.start - t.delay > t.length) {
            it.remove();
         }
      }
   }

   static void clear() {
      TIMED.clear();
   }

   public static record Look(ChronoBodies.Mode mode, float amount, int seed) {
   }

   public static enum Mode {
      NEGATIVE,
      DEVELOP;
   }

   private static record Timed(ChronoBodies.Mode mode, int start, int delay, int length, int flickerFrom) {
   }
}
