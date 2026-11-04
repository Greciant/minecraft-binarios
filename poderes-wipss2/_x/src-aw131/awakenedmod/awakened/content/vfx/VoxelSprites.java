package com.awakenedmod.awakened.content.vfx;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public final class VoxelSprites {
   private static final float[] CLUSTER_STATIONS = new float[]{0.25F, 0.62F};
   private static final double[] CLUSTER_SCALES = new double[]{0.55, 0.42};
   private static final double[] CLUSTER_CLIMB = new double[]{0.45, 0.36};
   public static final int STREAM_CLUSTER_LIFE = 6;
   private static final double BASE_WIDTH = 0.9;
   private static final int BURST_LIFE = 24;
   private static final int BURST_LIFE_BIG = 32;
   private static final float BURST_INTENSITY = 1.1F;
   public static final double BIG_RADIUS = 4.0;
   private static final int FRONT_RINGS = 4;
   private static final double FRONT_REACH_MUL = 1.5;
   private static final double FRONT_SPEED = 1.0;
   private static final int FRONT_RING_BASE = 2;
   private static final int FRONT_RING_PER_RING = 3;
   private static final double SATELLITE_SCALE = 0.275;
   private static final int SATELLITE_LIFE = 22;
   private static final double TRAIL_SCALE = 0.18;
   private static final double TRAIL_CLIMB = 0.7;
   private static final int TRAIL_LIFE = 6;
   private static final double FLARE_SCALE = 0.35;
   private static final double FLARE_CLIMB = 0.6;
   private static final int FLARE_LIFE = 6;
   private static final float FLARE_INTENSITY = 1.15F;
   public static final int WAKE_INTERVAL = 2;
   private static final double WAKE_BRIDGE = 1.0;
   private static final float WAKE_SKIP_CHANCE = 0.15F;
   private static final double WAKE_SCALE_MIN = 0.7;
   private static final double WAKE_SCALE_SPAN = 0.8;
   private static final float WAKE_FLARE_CHANCE = 0.07F;
   private static final int WAKE_LIFE_MIN = 5;
   private static final int WAKE_LIFE_SPAN = 4;
   private static final int INRUSH_SPOKES = 8;
   private static final double INRUSH_SCALE = 1.0;
   private static final double INRUSH_SCALE_JITTER = 0.4;
   private static final double INRUSH_RING_MIN = 0.7;
   private static final int INRUSH_LIFE_SLOW = 14;
   private static final int INRUSH_LIFE_FAST = 6;
   private static final double INRUSH_HEIGHT = 0.5;
   private static final double GOLDEN = 2.399963;
   private static final double GATHER_SCALE = 0.6;
   private static final int GATHER_LIFE = 10;
   private static final double KNOT_CLIMB = 0.6;
   private static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);

   private VoxelSprites() {
   }

   public static void stream(ServerLevel level, Vec3 origin, Vec3 dir, double length, double width, VoxelSprites.Look look) {
      Vec3 d = dir.normalize();
      double w = width / 0.9;

      for (int i = 0; i < CLUSTER_STATIONS.length; i++) {
         cluster(level, origin.add(d.scale(length * (double)CLUSTER_STATIONS[i])), CLUSTER_SCALES[i] * w, d.scale(length * CLUSTER_CLIMB[i]), 6, 1.0F, look);
      }
   }

   public static void trail(ServerLevel level, Vec3 pos, Vec3 velocity, VoxelSprites.Look look) {
      Vec3 back = velocity.lengthSqr() < 1.0E-6 ? Vec3.ZERO : velocity.scale(-1.0).normalize();
      Vec3 climb = back.scale(0.5).add(0.0, 0.8, 0.0).normalize().scale(0.7);
      cluster(level, pos, 0.18, climb, 6, 1.0F, look);
   }

   public static void wake(ServerLevel level, Vec3 pos, Vec3 velocity, VoxelSprites.Look look, int interval) {
      RandomSource random = level.random;
      if (!(random.nextFloat() < 0.15F)) {
         double speed = velocity.length();
         Vec3 back = speed < 0.001 ? new Vec3(0.0, 1.0, 0.0) : velocity.scale(-1.0 / speed);
         double bridge = Math.max(0.7, speed * (double)interval * 1.0);
         Vec3 climb = back.scale(bridge).add(0.0, 0.25, 0.0);
         double scale = 0.18 * (0.7 + random.nextDouble() * 0.8);
         if (random.nextFloat() < 0.07F) {
            scale *= 2.0;
         }

         int life = 5 + random.nextInt(4);
         cluster(level, pos, scale, climb, life, 1.0F, look);
      }
   }

   public static void burst(ServerLevel level, Vec3 center, double radius, VoxelSprites.Look look) {
      burst(level, center, radius, look, UP);
   }

   public static void burst(ServerLevel level, Vec3 center, double radius, VoxelSprites.Look look, Vec3 normal) {
      boolean big = radius >= 4.0;
      int life = big ? 32 : 24;
      burstShell(level, center, radius, life, look, normal, 0);
      if (big) {
         detonationFront(level, center, radius, look, normal);
      }
   }

   private static void detonationFront(ServerLevel level, Vec3 center, double radius, VoxelSprites.Look look, Vec3 normal) {
      RandomSource random = level.random;
      double reach = radius * 1.5;
      double satellite = radius * 0.275;

      for (int k = 1; k <= 4; k++) {
         double dist = reach * (double)k / 4.0;
         int delay = (int)Math.round(dist / 1.0);
         int count = 2 + 3 * k;
         double phase = random.nextDouble() * Math.PI * 2.0;

         for (int i = 0; i < count; i++) {
            double ang = phase + (Math.PI * 2) * (double)i / (double)count + (random.nextDouble() - 0.5) * 0.5;
            double r = dist * (0.9 + random.nextDouble() * 0.2);
            Vec3 off = new Vec3(Math.cos(ang) * r, 0.0, Math.sin(ang) * r);
            burstShell(level, center.add(off), satellite, 22, look, normal, delay + random.nextInt(2));
         }
      }
   }

   public static void impactFlare(ServerLevel level, Vec3 pos, VoxelSprites.Look look) {
      cluster(level, pos, 0.35, new Vec3(0.0, 0.6, 0.0), 6, 1.15F, look);
   }

   public static void inrush(ServerLevel level, Vec3 center, double reach, int tick, float progress, VoxelSprites.Look look) {
      RandomSource random = level.random;
      int life = Math.round(Mth.lerp(Mth.clamp(progress, 0.0F, 1.0F), 14.0F, 6.0F));
      double base = (double)tick * 2.399963;

      for (int i = 0; i < 8; i++) {
         double ang = base + (Math.PI * 2) * (double)i / 8.0 + (random.nextDouble() - 0.5) * 0.5;
         double r = reach * (0.7 + random.nextDouble() * 0.30000000000000004);
         double y = -0.8 + random.nextDouble() * reach * 0.5;
         Vec3 pos = center.add(Math.cos(ang) * r, y, Math.sin(ang) * r);
         double scale = 1.0 * (0.8 + random.nextDouble() * 0.4);
         inrushShell(level, pos, scale, center.subtract(pos), life, look);
      }
   }

   public static void gather(ServerLevel level, Vec3 from, Vec3 center, VoxelSprites.Look look) {
      gather(level, from, center, 0.6, 10, look);
   }

   public static void gather(ServerLevel level, Vec3 from, Vec3 center, double scale, int life, VoxelSprites.Look look) {
      inrushShell(level, from, scale, center.subtract(from), life, look);
   }

   public static void knot(ServerLevel level, Vec3 pos, double scale, VoxelSprites.Look look) {
      cluster(level, pos, scale, new Vec3(0.0, 0.6 * Math.max(1.0, scale), 0.0), 6, 1.15F, look);
   }

   public static void inrushShell(ServerLevel level, Vec3 pos, double scale, Vec3 toCenter, int lifetime, VoxelSprites.Look look) {
      Vfx.effect(look.inrush(), pos)
         .scale(scale)
         .secondary(toCenter)
         .color(look.color(), look.color2())
         .lifetime(lifetime)
         .intensity(1.0F)
         .ownerView(look.ownerId(), look.ownerDim())
         .spawn(level);
   }

   public static void cluster(ServerLevel level, Vec3 pos, double scale, Vec3 climb, int lifetime, float intensity, VoxelSprites.Look look) {
      Vfx.effect(look.sprite(), pos)
         .scale(scale)
         .secondary(climb)
         .color(look.color(), look.color2())
         .lifetime(lifetime)
         .intensity(intensity)
         .ownerView(look.ownerId(), look.ownerDim())
         .spawn(level);
   }

   public static void burstShell(ServerLevel level, Vec3 pos, double radius, int lifetime, VoxelSprites.Look look, Vec3 normal, int delay) {
      Vfx.effect(look.sprite(), pos)
         .scale(-radius)
         .secondary(normal)
         .color(look.color(), look.color2())
         .lifetime(lifetime)
         .intensity(1.1F)
         .delay(delay)
         .ownerView(look.ownerId(), look.ownerDim())
         .spawn(level);
   }

   public static record Look(VfxEntity.Kind sprite, VfxEntity.Kind inrush, int color, int color2, int ownerId, float ownerDim) {
      public Look(VfxEntity.Kind sprite, VfxEntity.Kind inrush, int color, int color2) {
         this(sprite, inrush, color, color2, -1, 1.0F);
      }

      public VoxelSprites.Look forOwner(int ownerId, float dim) {
         return new VoxelSprites.Look(this.sprite, this.inrush, this.color, this.color2, ownerId, dim);
      }
   }
}
