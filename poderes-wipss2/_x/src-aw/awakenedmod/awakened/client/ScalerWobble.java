package com.awakenedmod.awakened.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderLivingEvent.Pre;

public final class ScalerWobble {
   private static final float TRIGGER_REL_DELTA = 0.03F;
   private static final float KICK = 0.22F;
   private static final float MAX_AMPLITUDE = 0.13F;
   private static final float DAMPING = 5.5F;
   private static final float FREQUENCY = 18.0F;
   private static final float MIN_AMPLITUDE = 0.004F;
   private static final long STALE_MS = 10000L;
   private static final long PRUNE_INTERVAL_MS = 5000L;
   private static final Map<Integer, ScalerWobble.Entry> ENTRIES = new HashMap<>();
   private static long lastPruneMs;

   private ScalerWobble() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(ScalerWobble::onRenderLiving);
   }

   private static void onRenderLiving(Pre<?, ?> event) {
      float w = observe(event.getEntity());
      if (Math.abs(w - 1.0F) > 0.001F) {
         event.getPoseStack().scale(w, w, w);
      }
   }

   public static float observe(LivingEntity entity) {
      long now = Util.getMillis();
      prune(now);
      float scale = entity.getScale();
      ScalerWobble.Entry e = ENTRIES.get(entity.getId());
      if (e == null) {
         e = new ScalerWobble.Entry(scale, now);
         ENTRIES.put(entity.getId(), e);
         return 1.0F;
      } else {
         e.seenMs = now;
         float reference = Math.max(scale, e.lastScale);
         if (reference > 1.0E-4F) {
            float rel = (scale - e.lastScale) / reference;
            if (Math.abs(rel) > 0.03F) {
               e.amplitude = Mth.clamp(value(e, now) + rel * 0.22F, -0.13F, 0.13F);
               e.startMs = now;
               e.lastScale = scale;
            }
         }

         return 1.0F + value(e, now);
      }
   }

   private static float value(ScalerWobble.Entry e, long now) {
      if (Math.abs(e.amplitude) < 0.004F) {
         return 0.0F;
      } else {
         float t = (float)(now - e.startMs) / 1000.0F;
         float decay = (float)Math.exp((double)(-5.5F * t));
         if (Math.abs(e.amplitude) * decay < 0.004F) {
            e.amplitude = 0.0F;
            return 0.0F;
         } else {
            return e.amplitude * decay * Mth.cos(18.0F * t);
         }
      }
   }

   private static void prune(long now) {
      if (now - lastPruneMs >= 5000L) {
         lastPruneMs = now;
         Iterator<ScalerWobble.Entry> it = ENTRIES.values().iterator();

         while (it.hasNext()) {
            if (now - it.next().seenMs > 10000L) {
               it.remove();
            }
         }
      }
   }

   private static final class Entry {
      float lastScale;
      float amplitude;
      long startMs;
      long seenMs;

      Entry(float scale, long now) {
         this.lastScale = scale;
         this.seenMs = now;
      }
   }
}
