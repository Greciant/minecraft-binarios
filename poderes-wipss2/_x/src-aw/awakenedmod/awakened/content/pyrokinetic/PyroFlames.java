package com.awakenedmod.awakened.content.pyrokinetic;

import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.caster.Caster;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public final class PyroFlames {
   public static final int WAKE_INTERVAL = 2;

   private PyroFlames() {
   }

   public static void stream(ServerLevel level, Vec3 origin, Vec3 dir, double length, double width, FlameTone tone) {
      VoxelFlame.stream(level, origin, dir, length, width, tone);
   }

   public static void trail(ServerLevel level, Vec3 pos, Vec3 velocity, FlameTone tone) {
      VoxelFlame.trail(level, pos, velocity, tone);
   }

   public static void wake(ServerLevel level, Vec3 pos, Vec3 velocity, FlameTone tone, int interval) {
      VoxelFlame.wake(level, pos, velocity, tone, interval);
   }

   public static void burst(ServerLevel level, Vec3 center, double radius, FlameTone tone) {
      VoxelFlame.burst(level, center, radius, tone);
      burstSound(level, center, radius, tone);
   }

   public static void burst(ServerLevel level, Vec3 center, double radius, FlameTone tone, Vec3 normal) {
      VoxelFlame.burst(level, center, radius, tone, normal);
      burstSound(level, center, radius, tone);
   }

   public static void burstSilent(ServerLevel level, Vec3 center, double radius, FlameTone tone, Vec3 normal) {
      VoxelFlame.burst(level, center, radius, tone, normal);
   }

   private static void burstSound(ServerLevel level, Vec3 center, double radius, FlameTone tone) {
      sound(level, center, (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), 0.9F + (float)radius * 0.1F, 0.95F, tone);
      sound(level, center, SoundEvents.FIRECHARGE_USE, 1.0F, 0.8F, tone);
      if (radius >= 4.0) {
         sound(level, center, (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), 1.6F, 0.5F, tone);
      }
   }

   public static void impactFlare(ServerLevel level, Vec3 pos, FlameTone tone) {
      VoxelFlame.impactFlare(level, pos, tone);
   }

   public static void inrush(ServerLevel level, Vec3 center, double reach, int tick, float progress, FlameTone tone) {
      VoxelFlame.inrush(level, center, reach, tick, progress, tone);
   }

   public static void gather(ServerLevel level, Vec3 from, Vec3 center, FlameTone tone) {
      VoxelFlame.gather(level, from, center, tone);
   }

   public static void knot(ServerLevel level, Vec3 pos, double scale, FlameTone tone) {
      VoxelFlame.knot(level, pos, scale, tone);
   }

   public static void sound(ServerLevel level, Vec3 pos, SoundEvent sound, float volume, float pitch, FlameTone tone) {
      soundExcept(level, null, pos, sound, volume, pitch, tone);
   }

   public static void sound(Caster caster, Vec3 pos, SoundEvent sound, float volume, float pitch, FlameTone tone) {
      Fx.soundExcept(caster.level(), null, pos, sound, caster.soundSource(), volume, pitch * tone.soundPitch());
   }

   public static void soundExcept(ServerLevel level, ServerPlayer except, Vec3 pos, SoundEvent sound, float volume, float pitch, FlameTone tone) {
      Fx.soundExcept(level, except, pos, sound, SoundSource.PLAYERS, volume, pitch * tone.soundPitch());
   }
}
