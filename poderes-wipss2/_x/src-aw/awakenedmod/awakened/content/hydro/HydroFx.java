package com.awakenedmod.awakened.content.hydro;

import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.content.vfx.VfxEntity;
import com.awakenedmod.awakened.core.Fx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public final class HydroFx {
   private static final int MAX_LIFE = 30;

   private HydroFx() {
   }

   private static int rgb(boolean wet) {
      return HydroPalette.rgb(wet);
   }

   private static float opacity(boolean wet) {
      return HydroPalette.opacity(wet);
   }

   public static void splash(ServerLevel level, Vec3 pos, Vec3 normal, double size, boolean wet) {
      Vfx.effect(VfxEntity.Kind.WATER_SPLASH, pos.add(normal.scale(0.1)))
         .scale(size)
         .along(normal)
         .color(rgb(wet))
         .intensity(opacity(wet))
         .lifetime(life(14 + (int)(size * 4.0)))
         .spawn(level);
      droplets(level, pos.add(normal.scale(0.2)), (int)(8.0 + size * 10.0), size * 0.35, wet);
      sound(level, pos, SoundEvents.PLAYER_SPLASH, 0.6F + (float)size * 0.2F, 1.5F - (float)size * 0.2F);
   }

   public static void impactRing(ServerLevel level, Vec3 pos, Vec3 normal, double radius, boolean wet) {
      Vfx.effect(VfxEntity.Kind.WATER_RING, pos.add(normal.scale(0.05)))
         .scale(radius)
         .along(normal)
         .color(rgb(wet))
         .intensity(opacity(wet))
         .lifetime(life(12))
         .spawn(level);
   }

   public static void puddle(ServerLevel level, Vec3 pos, Vec3 normal, double radius, int ticks, boolean wet) {
      Vfx.effect(VfxEntity.Kind.WATER_PUDDLE, pos.add(normal.scale(0.05)))
         .scale(radius)
         .along(normal)
         .color(rgb(wet))
         .intensity(opacity(wet))
         .lifetime(life(ticks))
         .spawn(level);
   }

   public static void droplets(ServerLevel level, Vec3 pos, int count, double spread, boolean wet) {
      Fx.burst(level, ParticleTypes.SPLASH, pos, count, spread, 0.12);
      if (wet) {
         Fx.burst(level, ParticleTypes.BUBBLE_POP, pos, Math.max(1, count / 4), spread, 0.02);
      } else {
         Fx.burst(level, ParticleTypes.WHITE_SMOKE, pos, Math.max(1, count / 8), spread * 0.6, 0.01);
      }
   }

   public static void ribbon(ServerLevel level, Vec3 from, Vec3 to, boolean wet) {
      ribbon(level, from, to, wet, 10);
   }

   public static void sound(ServerLevel level, Vec3 pos, SoundEvent sound, float volume, float pitch) {
      Fx.sound(level, pos, sound, SoundSource.PLAYERS, volume, pitch);
   }

   private static int life(int ticks) {
      return Math.min(30, Math.max(2, ticks));
   }

   public static void ribbon(ServerLevel level, Vec3 from, Vec3 to, boolean wet, int ticks) {
      Vec3 off = to.subtract(from);
      Vfx.effect(VfxEntity.Kind.WATER_STREAM, from)
         .scale(0.09 + Math.min(0.06, off.length() * 0.006))
         .secondary(off)
         .color(rgb(wet))
         .intensity(opacity(wet))
         .lifetime(life(ticks))
         .spawn(level);
      Fx.burst(level, ParticleTypes.SPLASH, from, 4, 0.15, 0.05);
   }
}
