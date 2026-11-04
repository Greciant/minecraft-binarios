package com.awakenedmod.awakened.content.cryokinetic;

import com.awakenedmod.awakened.core.AimUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

final class GlacierShape {
   static final int PACKED = 0;
   static final int BLUE = 1;
   static final int CLEAR = 2;
   private static final double REACH = 20.0;
   private static final double HALF_ANGLE = Math.toRadians(30.0);
   private static final int SURGE_TICKS = 12;
   private static final double RISE = 0.5;
   private static final double SHOULDER = 3.5;
   private static final int MAX_DEPTH = 12;
   final BlockPos origin;
   final List<GlacierShape.Voxel> voxels = new ArrayList<>();
   final List<GlacierShape.Cell> cells = new ArrayList<>();
   final List<GlacierShape.Shard> shards = new ArrayList<>();
   private final Map<Long, GlacierShape.Voxel> placed = new HashMap<>();
   private final Map<Long, int[]> tops = new HashMap<>();

   private GlacierShape(BlockPos origin) {
      this.origin = origin;
   }

   static GlacierShape burst(Vec3 center, int count, double length, double radius, RandomSource rng) {
      BlockPos origin = BlockPos.containing(center);
      GlacierShape g = new GlacierShape(origin);
      Vec3 rel = center.subtract((double)origin.getX(), (double)origin.getY(), (double)origin.getZ());
      double spin = rng.nextDouble() * Math.PI * 2.0;

      for (int i = 0; i < count; i++) {
         double a = spin + ((double)i + rng.nextDouble() * 0.5) / (double)count * Math.PI * 2.0;
         double tilt = Math.toRadians(25.0 + rng.nextDouble() * 70.0);
         Vec3 dir = new Vec3(Math.cos(a) * Math.sin(tilt), Math.cos(tilt), Math.sin(a) * Math.sin(tilt));
         Vec3 base = rel.subtract(dir.scale(0.3));
         double len = length * (0.7 + rng.nextDouble() * 0.6);
         g.shards
            .add(
               new GlacierShape.Shard(
                  (float)base.x,
                  (float)base.y,
                  (float)base.z,
                  (float)dir.x,
                  (float)dir.y,
                  (float)dir.z,
                  (float)len,
                  (float)(radius * (0.8 + rng.nextDouble() * 0.4)),
                  rng.nextFloat() * 60.0F,
                  i % 3,
                  false
               )
            );
      }

      return g;
   }

   static GlacierShape build(ServerLevel level, BlockPos origin, Vec3 forward, RandomSource rng) {
      GlacierShape g = new GlacierShape(origin);
      g.body(level, forward);
      if (!g.cells.isEmpty()) {
         g.spikes(level, forward, rng);
      }

      return g;
   }

   private void body(ServerLevel level, Vec3 f) {
      Vec3 right = new Vec3(-f.z, 0.0, f.x);
      int span = 21;

      for (int x = -span; x <= span; x++) {
         for (int z = -span; z <= span; z++) {
            double cx = (double)x;
            double cz = (double)z;
            double d = cx * f.x + cz * f.z;
            double lat = cx * right.x + cz * right.z;
            double radial = Math.sqrt(cx * cx + cz * cz);
            if (!(d < 1.0) && !(radial > 20.0) && !(Math.abs(Math.atan2(lat, d)) > HALF_ANGLE)) {
               double angle = Math.abs(Math.atan2(lat, d));
               double edge = 1.0 - 0.35 * angle / HALF_ANGLE;
               double toFlank = (HALF_ANGLE - angle) * radial;
               double shoulder = Mth.clamp(toFlank / 3.5, 0.0, 1.0);
               shoulder = shoulder * shoulder * (3.0 - 2.0 * shoulder);
               double walk = (0.5 + 0.5 * Math.floor(d - 1.0)) * edge * (0.3 + 0.7 * shoulder);
               int topHalves = Math.max(1, (int)Math.floor(walk * 2.0));
               double topY = (double)this.origin.getY() + (double)topHalves / 2.0;
               Vec3 probe = new Vec3((double)this.origin.getX() + 0.5 + (double)x, topY + 2.0, (double)this.origin.getZ() + 0.5 + (double)z);
               Optional<Vec3> floor = AimUtil.groundedPositionIfFound(level, probe);
               if (!floor.isEmpty() && !(floor.get().y >= topY)) {
                  int floorY = Math.max((int)Math.floor(floor.get().y), (int)Math.floor(topY) - 12);
                  int delay = (int)Math.round(radial / 20.0 * 12.0);
                  int fullTop = (int)Math.floor(topY);
                  boolean half = topY - (double)fullTop > 0.25;

                  for (int y = floorY; y < fullTop; y++) {
                     int kind = y != fullTop - 1 && Math.floorMod(y + x * 3 + z * 7, 5) != 0 ? 1 : 0;
                     this.add(x, y - this.origin.getY(), z, kind, false, delay + (y - floorY) / 3);
                  }

                  if (half) {
                     this.add(x, fullTop - this.origin.getY(), z, 0, true, delay + (fullTop - floorY) / 3);
                  }

                  this.cells
                     .add(
                        new GlacierShape.Cell(
                           new Vec3((double)(this.origin.getX() + x) + 0.5, (double)floorY, (double)(this.origin.getZ() + z) + 0.5),
                           (float)(topY - (double)floorY),
                           delay
                        )
                     );
                  this.tops.put(key(x, z), new int[]{topHalves, delay});
               }
            }
         }
      }
   }

   private void spikes(ServerLevel level, Vec3 f, RandomSource rng) {
      for (int deg = -20; deg <= 20; deg += 8) {
         double t = (double)Math.abs(deg) / 20.0;
         Vec3 dir = f.yRot((float)Math.toRadians((double)deg * 1.2));
         double length = (21.0 - 6.0 * t) * (0.85 + rng.nextDouble() * 0.3);
         this.spike(f.yRot((float)Math.toRadians((double)deg)).scale(14.5), length, 1.6, dir, 8.0 + rng.nextDouble() * 10.0, rng, false);
      }

      for (int i = 0; i < 12; i++) {
         double a = Math.toRadians((rng.nextDouble() * 2.0 - 1.0) * 25.0);
         Vec3 dir = f.yRot((float)(a * 1.3));
         this.spike(
            f.yRot((float)a).scale(11.0 + rng.nextDouble() * 8.0),
            8.0 + rng.nextDouble() * 7.0,
            0.7 + rng.nextDouble() * 0.4,
            dir,
            12.0 + rng.nextDouble() * 22.0,
            rng,
            false
         );
      }

      for (int deg = -28; deg <= 28; deg += 4) {
         double t = (double)Math.abs(deg) / 28.0;
         Vec3 at = f.yRot((float)Math.toRadians((double)deg)).scale(18.5 - rng.nextDouble());
         Vec3 dir = f.yRot((float)Math.toRadians((double)deg * 1.4));
         double length = (13.0 - 5.0 * t) * (0.7 + rng.nextDouble() * 0.6);
         this.spike(at, length, 1.1 + rng.nextDouble() * 0.4, dir, 30.0 + rng.nextDouble() * 20.0, rng, false);
      }

      for (double deg = -28.0; deg <= 28.0; deg += 3.5) {
         Vec3 dir = f.yRot((float)Math.toRadians(deg + (rng.nextDouble() - 0.5) * 3.0));
         this.groundSpike(
            level,
            dir.scale(20.3 + rng.nextDouble() * 1.2),
            3.0 + rng.nextDouble() * 4.0,
            0.7 + rng.nextDouble() * 0.5,
            dir,
            18.0 + rng.nextDouble() * 22.0,
            rng
         );
      }

      for (int sign = -1; sign <= 1; sign += 2) {
         Vec3 edge = f.yRot((float)((double)sign * (HALF_ANGLE + Math.toRadians(2.0))));
         Vec3 out = f.yRot((float)((double)sign * Math.toRadians(75.0)));

         for (double r = 3.5; r <= 20.0; r += 1.6 + rng.nextDouble()) {
            this.groundSpike(
               level, edge.scale(r), 2.0 + r * 0.25 + rng.nextDouble() * 2.5, 0.55 + rng.nextDouble() * 0.4, out, 25.0 + rng.nextDouble() * 25.0, rng
            );
         }
      }

      for (double deg = -26.0; deg <= 26.0; deg += 5.0) {
         Vec3 dir = f.yRot((float)Math.toRadians(deg + (rng.nextDouble() - 0.5) * 3.0));
         this.groundSpike(
            level,
            dir.scale(20.1 + rng.nextDouble() * 0.5),
            7.0 + rng.nextDouble() * 5.0,
            0.9 + rng.nextDouble() * 0.5,
            dir,
            8.0 + rng.nextDouble() * 14.0,
            rng
         );
      }

      for (double height : new double[]{0.3, 0.6}) {
         for (double deg = -24.0 + height * 5.0; deg <= 24.0; deg += 6.0) {
            Vec3 dir = f.yRot((float)Math.toRadians(deg + (rng.nextDouble() - 0.5) * 3.0));
            this.faceSpike(dir.scale(19.2), height, 4.0 + rng.nextDouble() * 4.0, 0.8 + rng.nextDouble() * 0.5, dir, 50.0 + rng.nextDouble() * 22.0, rng);
         }
      }

      for (int sign = -1; sign <= 1; sign += 2) {
         for (double r : new double[]{6.0, 10.5, 15.0}) {
            Vec3 at = f.yRot((float)((double)sign * (HALF_ANGLE - Math.toRadians(5.0)))).scale(r);
            Vec3 out = f.yRot((float)((double)sign * Math.toRadians(70.0)));
            this.spike(at, 5.0 + rng.nextDouble() * 4.0, 0.9, out, 45.0 + rng.nextDouble() * 15.0, rng, false);
         }
      }

      for (int i = 0; i < 16; i++) {
         double a = Math.toRadians((double)(rng.nextBoolean() ? 1 : -1) * (16.0 + rng.nextDouble() * 12.0));
         Vec3 dir = f.yRot((float)a);
         this.spike(
            dir.scale(4.0 + rng.nextDouble() * 14.0),
            2.0 + rng.nextDouble() * 2.5,
            0.35 + rng.nextDouble() * 0.2,
            dir,
            15.0 + rng.nextDouble() * 30.0,
            rng,
            true
         );
      }
   }

   private void spike(Vec3 at, double length, double radius, Vec3 leanDir, double tiltDeg, RandomSource rng, boolean clear) {
      int bx = Mth.floor(at.x + 0.5);
      int bz = Mth.floor(at.z + 0.5);
      int[] top = this.tops.get(key(bx, bz));
      if (top != null) {
         this.shard(new Vec3((double)bx + 0.5, (double)top[0] / 2.0 - 1.0, (double)bz + 0.5), top[1] + 4, length, radius, leanDir, tiltDeg, rng, clear);
      }
   }

   private void faceSpike(Vec3 at, double height, double length, double radius, Vec3 leanDir, double tiltDeg, RandomSource rng) {
      int bx = Mth.floor(at.x + 0.5);
      int bz = Mth.floor(at.z + 0.5);
      int[] top = this.tops.get(key(bx, bz));
      if (top != null) {
         Vec3 flat = new Vec3(leanDir.x, 0.0, leanDir.z).normalize();
         this.shard(
            new Vec3((double)bx + 0.5 + flat.x * 0.2, (double)top[0] / 2.0 * height, (double)bz + 0.5 + flat.z * 0.2),
            top[1] + 3,
            length,
            radius,
            leanDir,
            tiltDeg,
            rng,
            false
         );
      }
   }

   private void groundSpike(ServerLevel level, Vec3 at, double length, double radius, Vec3 leanDir, double tiltDeg, RandomSource rng) {
      Vec3 probe = new Vec3((double)this.origin.getX() + 0.5 + at.x, (double)(this.origin.getY() + 4), (double)this.origin.getZ() + 0.5 + at.z);
      Optional<Vec3> floor = AimUtil.groundedPositionIfFound(level, probe);
      if (!floor.isEmpty() && !(Math.abs(floor.get().y - (double)this.origin.getY()) > 6.0)) {
         this.shard(new Vec3(at.x + 0.5, floor.get().y - (double)this.origin.getY() - 0.6, at.z + 0.5), 13, length, radius, leanDir, tiltDeg, rng, false);
      }
   }

   private void shard(Vec3 base, int delay, double length, double radius, Vec3 leanDir, double tiltDeg, RandomSource rng, boolean clear) {
      double tilt = Math.toRadians(tiltDeg);
      Vec3 flat = new Vec3(leanDir.x, 0.0, leanDir.z).normalize();
      Vec3 dir = flat.scale(Math.sin(tilt)).add(0.0, Math.cos(tilt), 0.0).normalize();
      this.shards
         .add(
            new GlacierShape.Shard(
               (float)base.x,
               (float)base.y,
               (float)base.z,
               (float)dir.x,
               (float)dir.y,
               (float)dir.z,
               (float)length,
               (float)radius,
               45.0F + (rng.nextFloat() - 0.5F) * 40.0F,
               delay,
               clear
            )
         );
   }

   private void add(int x, int y, int z, int kind, boolean half, int delay) {
      long k = key(x, z) * 1024L + (long)(y + 512);
      GlacierShape.Voxel had = this.placed.get(k);
      if (had == null || had.half() && !half) {
         GlacierShape.Voxel v = new GlacierShape.Voxel(x, y, z, kind, half, had != null ? had.delay() : delay);
         if (had != null) {
            this.voxels.set(this.voxels.indexOf(had), v);
         } else {
            this.voxels.add(v);
         }

         this.placed.put(k, v);
      }
   }

   private static long key(int x, int z) {
      return (long)(x + 512) << 20 | (long)(z + 512);
   }

   static record Cell(Vec3 feet, float height, int delay) {
   }

   static record Shard(float x, float y, float z, float dx, float dy, float dz, float length, float radius, float roll, int delay, boolean clear) {
   }

   static record Voxel(int x, int y, int z, int kind, boolean half, int delay) {
   }
}
