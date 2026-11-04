package com.awakenedmod.awakened.content.entity;

import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Fx;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SiegeHazards {
   private static final int HAZARD_COLOR = 16756768;
   private static final int DAMAGE_INTERVAL = 10;

   private SiegeHazards() {
   }

   public static void tick(ServerLevel level, Mob source, List<SiegeHazards.Zone> zones, long age) {
      Iterator<SiegeHazards.Zone> it = zones.iterator();

      while (it.hasNext()) {
         SiegeHazards.Zone z = it.next();
         if (z.warmup > 0) {
            z.warmup--;
            Telegraphs.groundMarker(level, z.center, z.radius, 16756768, z.warmup, z.warmupTotal);
            if (z.warmup == 3) {
               Telegraphs.dangerClose(level, z.center, z.radius);
            }

            if (z.warmup == 0) {
               erupt(level, z);
            }
         } else {
            if (z.active > 0) {
               z.active--;
               Vfx.ring(level, z.center.add(0.0, 0.05, 0.0), z.radius, 16756768, 3);
               Fx.burst(level, ParticleTypes.FLAME, z.center.add(0.0, 0.1, 0.0), (int)(z.radius * 4.0), z.radius * 0.7, 0.02);
               Fx.burst(level, ParticleTypes.LARGE_SMOKE, z.center.add(0.0, 0.2, 0.0), 2, z.radius * 0.5, 0.01);
               if (age % 10L == 0L) {
                  damageInside(level, source, z);
               }
            }

            if (z.done()) {
               it.remove();
            }
         }
      }
   }

   private static void erupt(ServerLevel level, SiegeHazards.Zone z) {
      Fx.burst(level, ParticleTypes.EXPLOSION, z.center.add(0.0, 0.5, 0.0), 6, z.radius * 0.4, 0.08);
      Fx.burst(level, ParticleTypes.LAVA, z.center.add(0.0, 0.3, 0.0), 10, z.radius * 0.6, 0.05);
      Fx.sound(level, z.center, (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.0F, 0.7F);
   }

   private static void damageInside(ServerLevel level, Mob source, SiegeHazards.Zone z) {
      AABB area = new AABB(z.center, z.center).inflate(z.radius, 2.0, z.radius);

      for (Player player : level.getEntitiesOfClass(
         Player.class,
         area,
         p -> p.isAlive() && !p.isCreative() && !p.isSpectator() && p.position().distanceToSqr(z.center.x, p.getY(), z.center.z) <= z.radius * z.radius
      )) {
         Dmg.hurt(player, level, source.damageSources().mobAttack(source), z.damage);
         player.setRemainingFireTicks(40);
      }
   }

   public static final class Zone {
      private final Vec3 center;
      private final double radius;
      private final float damage;
      private final int warmupTotal;
      private int warmup;
      private int active;

      public Zone(Vec3 center, double radius, float damage, int warmupTicks, int activeTicks) {
         this.center = center;
         this.radius = radius;
         this.damage = damage;
         this.warmup = warmupTicks;
         this.warmupTotal = warmupTicks;
         this.active = activeTicks;
      }

      private boolean done() {
         return this.warmup <= 0 && this.active <= 0;
      }
   }
}
