package com.awakenedmod.awakened.content.entity;

import java.util.Iterator;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

public final class TelegraphedAoe {
   private TelegraphedAoe() {
   }

   public static void tick(ServerLevel level, Mob source, List<TelegraphedAoe.Zone> zones) {
      Iterator<TelegraphedAoe.Zone> it = zones.iterator();

      while (it.hasNext()) {
         TelegraphedAoe.Zone z = it.next();
         z.warmup--;
         Telegraphs.groundMarker(level, z.center, z.radius, z.color, Math.max(0, z.warmup), z.warmupTotal);
         if (z.warmup == 3) {
            Telegraphs.dangerClose(level, z.center, z.radius);
         }

         if (z.warmup <= 0) {
            z.onDetonate.detonate(level, source, z.center);
            it.remove();
         }
      }
   }

   @FunctionalInterface
   public interface Detonation {
      void detonate(ServerLevel var1, Mob var2, Vec3 var3);
   }

   public static final class Zone {
      private final Vec3 center;
      private final double radius;
      private final int color;
      private final int warmupTotal;
      private final TelegraphedAoe.Detonation onDetonate;
      private int warmup;

      public Zone(Vec3 center, double radius, int warmupTicks, int color, TelegraphedAoe.Detonation onDetonate) {
         this.center = center;
         this.radius = radius;
         this.warmup = warmupTicks;
         this.warmupTotal = warmupTicks;
         this.color = color;
         this.onDetonate = onDetonate;
      }
   }
}
