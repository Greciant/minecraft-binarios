package com.awakenedmod.awakened.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class HitStops {
   private final Map<Integer, HitStops.Stop> stops = new LinkedHashMap<>();

   public void hold(LivingEntity body, int ticks, @Nullable Consumer<LivingEntity> then) {
      if (!(body instanceof Player) && ticks > 0) {
         HitStops.Stop stop = this.stops.get(body.getId());
         if (stop == null) {
            stop = new HitStops.Stop(body);
            TickFreeze.freeze(body, true);
            this.stops.put(body.getId(), stop);
         }

         stop.left = Math.max(stop.left, ticks);
         if (then != null) {
            stop.then = then;
         }
      } else {
         if (then != null) {
            then.accept(body);
         }
      }
   }

   public boolean holds(LivingEntity body) {
      return this.stops.containsKey(body.getId());
   }

   public boolean isEmpty() {
      return this.stops.isEmpty();
   }

   public void tick() {
      List<HitStops.Stop> done = null;
      Iterator<HitStops.Stop> it = this.stops.values().iterator();

      while (it.hasNext()) {
         HitStops.Stop stop = it.next();
         boolean gone = !stop.body.isAlive() || stop.body.isRemoved();
         if (gone || --stop.left <= 0) {
            it.remove();
            TickFreeze.release(stop.body, true);
            if (!gone && stop.then != null) {
               (done == null ? (done = new ArrayList<>()) : done).add(stop);
            }
         }
      }

      if (done != null) {
         for (HitStops.Stop stop : done) {
            stop.then.accept(stop.body);
         }
      }
   }

   public void releaseAll() {
      for (HitStops.Stop stop : this.stops.values()) {
         TickFreeze.release(stop.body, true);
      }

      this.stops.clear();
   }

   private static final class Stop {
      final LivingEntity body;
      int left;
      @Nullable
      Consumer<LivingEntity> then;

      Stop(LivingEntity body) {
         this.body = body;
      }
   }
}
