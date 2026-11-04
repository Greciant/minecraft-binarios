package com.awakenedmod.awakened.content.aero;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.NeoForge;

public final class AeroAmbience {
   private static final List<AeroAmbience.TickFx> ACTIVE = new ArrayList<>();
   private static final List<AeroAmbience.Beat> BEATS = new ArrayList<>();
   private static boolean registered;

   private AeroAmbience() {
   }

   public static void add(AeroAmbience.TickFx fx) {
      ensureRegistered();
      ACTIVE.add(fx);
   }

   public static void schedule(ServerLevel level, int delayTicks, Runnable beat) {
      ensureRegistered();
      BEATS.add(new AeroAmbience.Beat(level.getGameTime() + (long)delayTicks, level, beat));
   }

   private static synchronized void ensureRegistered() {
      if (!registered) {
         registered = true;
         NeoForge.EVENT_BUS.addListener(e -> onTick());
         NeoForge.EVENT_BUS.addListener(e -> {
            ACTIVE.clear();
            BEATS.clear();
         });
      }
   }

   private static void onTick() {
      if (!BEATS.isEmpty()) {
         List<AeroAmbience.Beat> due = new ArrayList<>();
         Iterator<AeroAmbience.Beat> it = BEATS.iterator();

         while (it.hasNext()) {
            AeroAmbience.Beat beat = it.next();
            if (beat.level().getGameTime() >= beat.atTick()) {
               it.remove();
               due.add(beat);
            }
         }

         for (AeroAmbience.Beat beat : due) {
            beat.run().run();
         }
      }

      if (!ACTIVE.isEmpty()) {
         for (AeroAmbience.TickFx fx : new ArrayList<>(ACTIVE)) {
            if (!fx.tick()) {
               ACTIVE.remove(fx);
            }
         }
      }
   }

   private static record Beat(long atTick, ServerLevel level, Runnable run) {
   }

   @FunctionalInterface
   public interface TickFx {
      boolean tick();
   }
}
