package com.awakenedmod.awakened.content.aero;

import com.awakenedmod.awakened.core.Harm;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;

public final class AirRules {
   private static final List<AirRules.Zone> ZONES = new CopyOnWriteArrayList<>();
   private static boolean registered;

   private AirRules() {
   }

   public static void register(AirRules.Zone zone) {
      ensureRegistered();
      ZONES.add(zone);
   }

   public static void unregister(AirRules.Zone zone) {
      ZONES.remove(zone);
   }

   public static boolean isSuppressed(Entity entity) {
      if (entity != null && !ZONES.isEmpty()) {
         for (AirRules.Zone zone : ZONES) {
            if (zone.containsEntity(entity) && Harm.allowed(zone.responsible(), entity)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public static boolean withinCylinder(Entity entity, ServerLevel level, Vec3 center, double radius, double height) {
      return withinCylinder(entity, level, center, radius, height, 0.5);
   }

   public static boolean withinCylinder(Entity entity, ServerLevel level, Vec3 center, double radius, double height, double below) {
      if (entity.level() != level) {
         return false;
      } else {
         Vec3 pos = entity.position();
         double dx = pos.x - center.x;
         double dz = pos.z - center.z;
         if (dx * dx + dz * dz > radius * radius) {
            return false;
         } else {
            double dy = pos.y - center.y;
            return dy >= -below && dy <= height + 0.5;
         }
      }
   }

   private static synchronized void ensureRegistered() {
      if (!registered) {
         registered = true;
         NeoForge.EVENT_BUS.addListener(e -> ZONES.clear());
      }
   }

   public interface Zone {
      boolean containsEntity(Entity var1);

      @Nullable
      Entity responsible();
   }
}
