package com.devilfruits.core.combat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class AbilityProjectiles {
   private static final Map<ServerLevel, List<AbilityProjectiles.Flight>> FLIGHTS = new WeakHashMap<>();

   public static <T extends AbilityProjectiles.Flight> T track(ServerLevel level, T flight) {
      List<AbilityProjectiles.Flight> list = FLIGHTS.computeIfAbsent(level, l -> new ArrayList<>());
      list.removeIf(f -> f.done || f.broken);
      list.add(flight);
      return flight;
   }

   public static int shatter(ServerLevel level, Entity by, Predicate<Vec3> within, Vec3 push) {
      List<AbilityProjectiles.Flight> list = FLIGHTS.get(level);
      if (list == null) {
         return 0;
      } else {
         int n = 0;

         for (AbilityProjectiles.Flight f : List.copyOf(list)) {
            if (!f.done && !f.broken && f.ownerId != by.getId()) {
               Vec3 to = by.position().subtract(f.position);
               Vec3 edge = to.lengthSqr() < 1.0E-6 ? f.position : f.position.add(to.normalize().scale(f.radius));
               if (within.test(f.position) || within.test(edge)) {
                  f.broken = true;
                  f.shatter(level, by, push);
                  n++;
               }
            }
         }

         list.removeIf(fx -> fx.done || fx.broken);
         return n;
      }
   }

   public static List<AbilityProjectiles.Flight> active(ServerLevel level) {
      List<AbilityProjectiles.Flight> list = FLIGHTS.get(level);
      return list == null ? List.of() : list.stream().filter(f -> !f.done && !f.broken).toList();
   }

   private AbilityProjectiles() {
   }

   public abstract static class Flight {
      private final int ownerId;
      private Vec3 position;
      private final double radius;
      private boolean broken;
      private boolean done;

      protected Flight(@Nullable Entity owner, Vec3 position, double radius) {
         this.ownerId = owner != null ? owner.getId() : -1;
         this.position = position;
         this.radius = radius;
      }

      public Vec3 position() {
         return this.position;
      }

      public double radius() {
         return this.radius;
      }

      public void moveTo(Vec3 at) {
         this.position = at;
      }

      public void end() {
         this.done = true;
      }

      public boolean broken() {
         return this.broken;
      }

      protected abstract void shatter(ServerLevel var1, Entity var2, Vec3 var3);
   }
}
