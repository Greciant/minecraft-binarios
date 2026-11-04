package com.awakenedmod.awakened.content.pyrokinetic;

import com.awakenedmod.awakened.content.vfx.VfxEntity;
import com.awakenedmod.awakened.content.vfx.VoxelSprites;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public final class VoxelFlame {
   static final double BIG_RADIUS = 4.0;
   static final int WAKE_INTERVAL = 2;

   private VoxelFlame() {
   }

   private static VoxelSprites.Look look(FlameTone tone) {
      return new VoxelSprites.Look(VfxEntity.Kind.FIRE_VOXEL_SPRITE, VfxEntity.Kind.FIRE_VOXEL_INRUSH, tone.wireBody(), tone.wireColor2());
   }

   static void stream(ServerLevel level, Vec3 origin, Vec3 dir, double length, double width, FlameTone tone) {
      VoxelSprites.stream(level, origin, dir, length, width, look(tone));
   }

   static void trail(ServerLevel level, Vec3 pos, Vec3 velocity, FlameTone tone) {
      VoxelSprites.trail(level, pos, velocity, look(tone));
   }

   static void wake(ServerLevel level, Vec3 pos, Vec3 velocity, FlameTone tone, int interval) {
      VoxelSprites.wake(level, pos, velocity, look(tone), interval);
   }

   static void burst(ServerLevel level, Vec3 center, double radius, FlameTone tone) {
      VoxelSprites.burst(level, center, radius, look(tone));
   }

   static void burst(ServerLevel level, Vec3 center, double radius, FlameTone tone, Vec3 normal) {
      VoxelSprites.burst(level, center, radius, look(tone), normal);
   }

   static void impactFlare(ServerLevel level, Vec3 pos, FlameTone tone) {
      VoxelSprites.impactFlare(level, pos, look(tone));
   }

   static void inrush(ServerLevel level, Vec3 center, double reach, int tick, float progress, FlameTone tone) {
      VoxelSprites.inrush(level, center, reach, tick, progress, look(tone));
   }

   static void gather(ServerLevel level, Vec3 from, Vec3 center, FlameTone tone) {
      VoxelSprites.gather(level, from, center, look(tone));
   }

   static void knot(ServerLevel level, Vec3 pos, double scale, FlameTone tone) {
      VoxelSprites.knot(level, pos, scale, look(tone));
   }
}
