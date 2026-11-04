package com.awakenedmod.awakened.content.hydro;

import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.content.vfx.VfxEntity;
import com.awakenedmod.awakened.core.Fx;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class HydroJetFx {
   public static final int DISSOLVE_MAX = 10;
   public static final int COLLAPSE_TICKS = 8;
   public static final double COLLAPSE_MARK = -1.0;
   public static final int TENDRIL_TICKS = 15;
   public static final double TENDRIL_RADIUS = 0.17;
   public static final double TENDRIL_OFF_FACE = 0.1;

   private HydroJetFx() {
   }

   public static double packNormal(Vec3 n) {
      return (double)(1L + Math.round(n.x) + 1L + 3L * (Math.round(n.y) + 1L) + 9L * (Math.round(n.z) + 1L));
   }

   public static Vec3 unpackNormal(double packed) {
      int c = (int)Math.round(packed) - 1;
      if (c >= 0 && c <= 26) {
         Vec3 n = new Vec3((double)(c % 3 - 1), (double)(c / 3 % 3 - 1), (double)(c / 9 - 1));
         return n.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 1.0, 0.0) : n.normalize();
      } else {
         return new Vec3(0.0, 1.0, 0.0);
      }
   }

   static VfxEntity geyser(ServerLevel level, Vec3 base, Entity rider, int fuse, int erupt, double power, boolean wet) {
      return Vfx.effect(VfxEntity.Kind.WATER_GEYSER, base.add(0.0, 0.06, 0.0))
         .scale(power)
         .secondary(new Vec3(VfxEntity.idToSecondary(rider.getId()), (double)fuse, (double)erupt))
         .color(HydroPalette.bodyRgb(wet))
         .intensity(HydroPalette.bodyOpacity(wet))
         .lifetime(fuse + erupt + 1)
         .spawnTracked(level);
   }

   static void rumbleTick(ServerLevel level, Vec3 base, int step, int total) {
      float u = (float)step / (float)Math.max(1, total);
      Vec3 foot = base.add(0.0, 0.15, 0.0);
      Fx.burst(level, ParticleTypes.BUBBLE_POP, foot, 3 + step, 0.35 + (double)u * 0.4, 0.02);
      if (step % 2 == 1) {
         Fx.burst(level, ParticleTypes.SPLASH, foot, 4 + step, 0.4 + (double)u * 0.3, 0.05);
      }

      Fx.Floor floor = Fx.floorState(level, base.x, base.y, base.z);
      if (floor != null) {
         double spread = 0.3 + (double)u * 0.35;
         level.sendParticles(
            new BlockParticleOption(ParticleTypes.BLOCK, floor.state()), base.x, floor.topY() + 0.1, base.z, 2 + step / 2, spread, 0.05, spread, 0.08
         );
      }
   }

   static void eruptTick(ServerLevel level, Vec3 base, double topY, int age, double power, boolean wet, boolean self) {
      Vec3 crown = new Vec3(base.x, topY, base.z);
      if (self) {
         double out = 1.2 * (0.85 + 0.25 * power) + 0.5;
         Vec3 low = crown.add(0.0, -1.5, 0.0);

         for (int i = 0; i < 6; i++) {
            double a = (double)i * (Math.PI / 3) + (double)age * 0.4;
            Fx.burst(level, ParticleTypes.SPLASH, low.add(Math.cos(a) * out, 0.0, Math.sin(a) * out), 2, 0.15, 0.1);
         }
      } else {
         Fx.burst(level, ParticleTypes.SPLASH, crown, 6, 0.45, 0.12);
         if (!wet && age % 4 == 0) {
            Fx.burst(level, ParticleTypes.WHITE_SMOKE, crown.add(0.0, 0.3, 0.0), 2, 0.5, 0.01);
         }
      }

      if (age % 3 == 0) {
         Fx.burst(level, ParticleTypes.FALLING_WATER, crown.add(0.0, -0.5, 0.0), 2, 0.7, 0.0);
         Fx.burst(level, ParticleTypes.SPLASH, base.add(0.0, 0.2, 0.0), 6, 0.6, 0.06);
      }
   }

   static void collapse(ServerLevel level, Vec3 base, Entity rider, double height, double power, boolean wet) {
      Vfx.effect(VfxEntity.Kind.WATER_GEYSER, base.add(0.0, 0.06, 0.0))
         .scale(power)
         .secondary(new Vec3(Math.min(30.0, height), -1.0, VfxEntity.idToSecondary(rider.getId())))
         .color(HydroPalette.bodyRgb(wet))
         .intensity(HydroPalette.bodyOpacity(wet))
         .lifetime(11)
         .spawnTracked(level);
   }

   static void burst(ServerLevel level, Vec3 top, double power, boolean self) {
      double r = 1.0 + 0.4 * power;
      if (self) {
         Vec3 low = top.add(0.0, -1.2, 0.0);

         for (int i = 0; i < 6; i++) {
            double a = (double)i * (Math.PI / 3);
            Fx.burst(level, ParticleTypes.BUBBLE_POP, low.add(Math.cos(a) * r * 1.3, 0.0, Math.sin(a) * r * 1.3), 3, 0.2, 0.02);
         }
      } else {
         Fx.burst(level, ParticleTypes.BUBBLE_POP, top, 14, r * 0.8, 0.02);
         Fx.burst(level, ParticleTypes.SPLASH, top, 5, r * 0.6, 0.05);
      }

      Fx.sound(level, top, SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS, 0.8F, 1.25F);
      Fx.sound(level, top, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 0.5F, 1.4F);
   }

   static void tendril(ServerLevel level, Entity caster, Vec3 latch, Vec3 normal, boolean wet) {
      Vfx.effect(VfxEntity.Kind.WATER_TENDRIL, latch.add(normal.scale(0.1)))
         .scale(0.17)
         .secondary(new Vec3(VfxEntity.idToSecondary(caster.getId()), packNormal(normal), 0.0))
         .color(HydroPalette.rgb(wet))
         .intensity(HydroPalette.lineOpacity(wet))
         .lifetime(15)
         .spawn(level);
   }

   static void latch(ServerLevel level, Vec3 at, Vec3 normal, boolean wet) {
      HydroFx.splash(level, at, normal, 0.55, wet);
      Fx.burst(level, ParticleTypes.FALLING_WATER, at.add(normal.scale(0.15)), 3, 0.2, 0.0);
   }

   static void whip(ServerLevel level, Vec3 from, Vec3 to, boolean wet) {
      Vfx.effect(VfxEntity.Kind.WATER_STREAM, from)
         .scale(0.13)
         .secondary(to.subtract(from))
         .color(HydroPalette.rgb(wet))
         .intensity(HydroPalette.lineOpacity(wet))
         .lifetime(6)
         .spawn(level);
   }
}
