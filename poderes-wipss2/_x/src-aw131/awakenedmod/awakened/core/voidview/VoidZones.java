package com.awakenedmod.awakened.core.voidview;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

@EventBusSubscriber(
   modid = "awakened"
)
public final class VoidZones {
   private static final Map<Integer, VoidZones.Zone> SERVER = new ConcurrentHashMap<>();
   private static final Map<Integer, VoidZones.Zone> CLIENT = new ConcurrentHashMap<>();

   private VoidZones() {
   }

   private static Map<Integer, VoidZones.Zone> side(boolean client) {
      return client ? CLIENT : SERVER;
   }

   public static void put(boolean client, int key, VoidZones.Zone zone) {
      side(client).put(key, zone);
   }

   public static void remove(boolean client, int key) {
      side(client).remove(key);
   }

   public static void clearClient() {
      CLIENT.clear();
   }

   public static boolean any(Level level) {
      return !side(level.isClientSide()).isEmpty();
   }

   public static boolean inside(Level level, double x, double y, double z) {
      Map<Integer, VoidZones.Zone> zones = side(level.isClientSide());
      if (zones.isEmpty()) {
         return false;
      } else {
         ResourceKey<Level> dim = level.dimension();

         for (VoidZones.Zone zone : zones.values()) {
            if (zone.contains(dim, x, y, z)) {
               return true;
            }
         }

         return false;
      }
   }

   public static boolean overlaps(boolean client, ResourceKey<Level> dim, Vec3 centre, double radius) {
      for (VoidZones.Zone zone : side(client).values()) {
         double reach = zone.radius() + radius;
         if (zone.dimension().equals(dim) && zone.centre().distanceToSqr(centre) < reach * reach) {
            return true;
         }
      }

      return false;
   }

   @Nullable
   public static Vec3 exit(Level level, Vec3 from, Vec3 to) {
      Map<Integer, VoidZones.Zone> zones = side(level.isClientSide());
      ResourceKey<Level> dim = level.dimension();
      Vec3 d = to.subtract(from);
      double a = d.lengthSqr();
      Vec3 at = from;

      for (int pass = 0; pass <= zones.size(); pass++) {
         VoidZones.Zone holding = null;

         for (VoidZones.Zone zone : zones.values()) {
            if (zone.contains(dim, at.x, at.y, at.z)) {
               holding = zone;
               break;
            }
         }

         if (holding == null) {
            return at;
         }

         if (a < 1.0E-12) {
            return null;
         }

         Vec3 f = from.subtract(holding.centre());
         double b = f.dot(d);
         double c = f.lengthSqr() - holding.radius() * holding.radius();
         double t = (-b + Math.sqrt(Math.max(0.0, b * b - a * c))) / a;
         if (t >= 1.0) {
            return null;
         }

         at = from.add(d.scale(Math.min(1.0, t + 1.0E-4)));
      }

      return null;
   }

   public static boolean inside(Level level, BlockPos pos) {
      return inside(level, (double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5);
   }

   public static boolean inside(Entity entity) {
      return inside(entity.level(), entity.getX(), entity.getY(0.5), entity.getZ());
   }

   @SubscribeEvent
   public static void onServerStopped(ServerStoppedEvent event) {
      SERVER.clear();
   }

   public static record Zone(ResourceKey<Level> dimension, Vec3 centre, double radius) {
      public boolean contains(ResourceKey<Level> dim, double x, double y, double z) {
         if (!this.dimension.equals(dim)) {
            return false;
         } else {
            double dx = x - this.centre.x;
            double dy = y - this.centre.y;
            double dz = z - this.centre.z;
            return dx * dx + dy * dy + dz * dz < this.radius * this.radius;
         }
      }
   }
}
