package com.awakenedmod.awakened.content.kineticcharger;

import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.content.vfx.VfxEntity;
import com.awakenedmod.awakened.content.vfx.VoxelSprites;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class KineticVoxel {
   private static final int WAKE_LIFE_MIN = 6;
   private static final int WAKE_LIFE_SPAN = 3;
   private static final int BURST_LIFE = 24;

   private KineticVoxel() {
   }

   public static VoxelSprites.Look lookFor(ServerPlayer caster, float dim) {
      return KineticTone.LOOK.forOwner(caster.getId(), dim);
   }

   public static void stream(ServerLevel level, Vec3 origin, Vec3 dir, double length, double width) {
      VoxelSprites.stream(level, origin, dir, length, width, KineticTone.LOOK);
   }

   public static void trail(ServerLevel level, Vec3 pos, Vec3 velocity) {
      VoxelSprites.trail(level, pos, velocity, KineticTone.LOOK);
   }

   public static void wake(ServerLevel level, Vec3 pos, Vec3 velocity, int interval, double width) {
      Vec3 back = velocity.scale((double)(-interval));
      if (!(back.lengthSqr() < 0.01)) {
         int life = 6 + level.random.nextInt(3);
         Vfx.effect(VfxEntity.Kind.KINETIC_WAKE, pos).scale(width).secondary(back).color(8072383, 13073919).lifetime(life).spawn(level);
      }
   }

   public static void burst(ServerLevel level, Vec3 center, double radius) {
      VoxelSprites.burst(level, center, radius, KineticTone.LOOK);
   }

   public static void burst(ServerLevel level, Vec3 center, double radius, Vec3 normal) {
      VoxelSprites.burst(level, center, radius, KineticTone.LOOK, normal);
   }

   public static void burstDelayed(ServerLevel level, Vec3 center, double radius, Vec3 normal, int delay) {
      VoxelSprites.burstShell(level, center, radius, 24, KineticTone.LOOK, normal, delay);
   }

   public static void inrush(ServerLevel level, Vec3 center, double reach, int tick, float progress) {
      VoxelSprites.inrush(level, center, reach, tick, progress, KineticTone.LOOK);
   }

   public static void gather(ServerLevel level, Vec3 from, Vec3 center) {
      VoxelSprites.gather(level, from, center, KineticTone.LOOK);
   }

   public static void gather(ServerLevel level, Vec3 from, Vec3 center, double scale, int life, VoxelSprites.Look look) {
      VoxelSprites.gather(level, from, center, scale, life, look);
   }

   public static void flare(ServerLevel level, Vec3 pos) {
      VoxelSprites.impactFlare(level, pos, KineticTone.LOOK);
   }

   public static void knot(ServerLevel level, Vec3 pos, double scale) {
      knot(level, pos, scale, KineticTone.LOOK);
   }

   public static void knot(ServerLevel level, Vec3 pos, double scale, VoxelSprites.Look look) {
      VoxelSprites.knot(level, pos, scale, look);
   }

   public static void airRipple(
      ServerLevel level, Vec3 pos, Vec3 dir, double radius, double band, double depth, float intensity, int delay, int pop, int hold, int fade
   ) {
      Vfx.effect(VfxEntity.Kind.KINETIC_AIR_TORUS, pos)
         .scale(radius)
         .secondary(new Vec3(band, (double)VfxEntity.packAirTorusClock(pop, hold), depth))
         .along(dir)
         .color(16053496, 16777215)
         .lifetime(Math.max(0, hold) + Math.max(1, fade))
         .intensity(intensity)
         .delay(delay)
         .spawn(level);
   }
}
