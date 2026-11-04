package com.devilfruits.fruits.doru_doru.atelier;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public final class TechniqueGen {
   public static final double PLATE_TOP = 3.0;
   public static final double RING_RADIUS = 2.35;
   public static final double RING_CANDLE = 0.4;
   public static final double RING_HEIGHT = 2.25;
   public static final double CENTRE_CANDLE = 0.6;
   public static final double CENTRE_HEIGHT = 3.0;

   public static VoxelSet wall(final Vec3 base, Vec3 facing, double width, final double height, final double thick, final double bow) {
      double fx = facing.x;
      double fz = facing.z;
      double len = Math.sqrt(fx * fx + fz * fz);
      if (len < 1.0E-4) {
         fx = 0.0;
         fz = 1.0;
         len = 1.0;
      }

      final double nx = fx / len;
      final double nz = fz / len;
      final double ux = -nz;
      final double uz = nx;
      final double half = width / 2.0;
      ShapeGen.Field f = new ShapeGen.Field() {
         @Override
         public double sdf(double x, double y, double z) {
            double px = x - base.x;
            double pz = z - base.z;
            double py = y - base.y;
            double u = px * ux + pz * uz;
            double w = px * nx + pz * nz;
            double k = Math.min(1.0, Math.abs(u) / half);
            double centre = bow * (1.0 - k * k);
            double rib = Math.abs((u + half) / 1.5 - Math.rint((u + half) / 1.5)) < 0.17 ? 0.3 : 0.0;
            double top = height + 0.25 * Math.sin(u * 2.1) * (1.0 - k * 0.5);
            double dw = w - centre;
            double across = dw > 0.0 ? dw - (thick / 2.0 + rib) : -dw - thick / 2.0;
            double along = Math.abs(u) - half;
            double vert = Math.max(-py, py - top);
            return Math.max(Math.max(across, along), vert);
         }

         @Override
         public double[] bounds() {
            double r = half + bow + thick + 1.0;
            return new double[]{base.x - r, base.y - 0.5, base.z - r, base.x + r, base.y + height + 1.0, base.z + r};
         }
      };
      VoxelSet out = new VoxelSet();
      ShapeGen.rasterize(f, 0.0F, out);
      return out;
   }

   public static VoxelSet dome(Vec3 centre, double radius, double shell) {
      VoxelSet out = new VoxelSet();
      ShapeGen.rasterize(ShapeGen.sphere(centre, radius, true), (float)shell, out);
      return out;
   }

   public static VoxelSet peg(Vec3 at, Vec3 dir, double length, double radius) {
      Vec3 d = dir.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : dir.normalize();
      Vec3 a = at.add(d.scale(0.4));
      Vec3 b = at.subtract(d.scale(length));
      VoxelSet out = new VoxelSet();
      ShapeGen.rasterize(ShapeGen.loft(a, b, radius, radius * 0.75), 0.0F, out);
      return out;
   }

   public static TechniqueGen.Service service(Vec3 base, int ring, long seed) {
      RandomSource rnd = RandomSource.create(seed);
      double cx = base.x;
      double cz = base.z;
      double by = base.y;
      int drips = 14;
      double[] dripAngle = new double[drips];
      double[] dripLength = new double[drips];

      for (int i = 0; i < drips; i++) {
         dripAngle[i] = ((double)i + rnd.nextDouble() * 0.6) * (float) (Math.PI * 2) / (double)drips;
         dripLength[i] = 0.25 + rnd.nextDouble() * 0.9;
      }

      double wobblePhase = rnd.nextDouble() * (float) (Math.PI * 2);
      VoxelSet stand = new VoxelSet();
      double plate = 3.4;
      int x0 = Mth.floor((cx - plate - 0.3) * 4.0);
      int x1 = Mth.floor((cx + plate + 0.3) * 4.0);
      int z0 = Mth.floor((cz - plate - 0.3) * 4.0);
      int z1 = Mth.floor((cz + plate + 0.3) * 4.0);
      int y0 = Mth.floor(by * 4.0);
      int y1 = Mth.floor((by + 3.0 + 0.25) * 4.0);

      for (int sy = y0; sy < y1; sy++) {
         double dy = ((double)sy + 0.5) / 4.0 - by;

         for (int sx = x0; sx <= x1; sx++) {
            for (int sz = z0; sz <= z1; sz++) {
               double dx = ((double)sx + 0.5) / 4.0 - cx;
               double dz = ((double)sz + 0.5) / 4.0 - cz;
               double r = Math.sqrt(dx * dx + dz * dz);
               double a = Math.atan2(dz, dx);
               double edge = plate + 0.08 * Math.sin(a * 6.0 + wobblePhase);
               boolean in;
               if (dy < 0.5) {
                  in = r < 1.75 - dy * 0.7;
               } else if (dy < 2.5) {
                  in = r < 0.6 + Math.max(0.0, dy - 1.9) * 0.9;
               } else if (dy < 3.0) {
                  in = r < edge;
               } else {
                  in = r < edge && r > edge - 0.35;
               }

               if (!in && dy < 2.5 && r < edge && r > edge - 0.3) {
                  for (int i = 0; i < drips; i++) {
                     double da = Math.abs(Mth.wrapDegrees((a - dripAngle[i]) * 180.0F / (float)Math.PI)) * (float) (Math.PI / 180.0);
                     if (da * r < 0.2 && dy > 2.5 - dripLength[i]) {
                        in = true;
                        break;
                     }
                  }
               }

               if (in) {
                  stand.add(sx, sy, sz);
               }
            }
         }
      }

      List<TechniqueGen.Candle> candles = new ArrayList<>();
      int foot = Mth.floor((by + 3.0) * 4.0);

      for (int ix = 0; ix < ring; ix++) {
         double ax = (double)((float)ix * (float) (Math.PI * 2) / (float)ring);
         candles.add(new TechniqueGen.Candle(cx + Math.cos(ax) * 2.35, cz + Math.sin(ax) * 2.35, 0.4, foot, foot + (int)Math.round(9.0) - 1));
      }

      candles.add(new TechniqueGen.Candle(cx, cz, 0.6, foot, foot + (int)Math.round(12.0) - 1));
      VoxelSet candleDrips = new VoxelSet();

      for (TechniqueGen.Candle c : candles) {
         for (int k = 0; k < 2; k++) {
            double ax = rnd.nextDouble() * (float) (Math.PI * 2);
            double rx = c.radius() + 0.1;
            int sx = Mth.floor((c.x() + Math.cos(ax) * rx) * 4.0);
            int sz = Mth.floor((c.z() + Math.sin(ax) * rx) * 4.0);
            int len = 2 + rnd.nextInt(4);

            for (int y = c.top() - 2 - len; y <= c.top() - 2; y++) {
               candleDrips.add(sx, y, sz);
            }
         }
      }

      return new TechniqueGen.Service(stand, candleDrips, candles);
   }

   private TechniqueGen() {
   }

   public static record Candle(double x, double z, double radius, int foot, int top) {
      public int height() {
         return this.top - this.foot + 1;
      }

      public void layer(int y, VoxelSet out) {
         double r = this.radius + 0.2;
         int x0 = Mth.floor((this.x - r) * 4.0);
         int x1 = Mth.floor((this.x + r) * 4.0);
         int z0 = Mth.floor((this.z - r) * 4.0);
         int z1 = Mth.floor((this.z + r) * 4.0);

         for (int sx = x0; sx <= x1; sx++) {
            for (int sz = z0; sz <= z1; sz++) {
               double dx = ((double)sx + 0.5) / 4.0 - this.x;
               double dz = ((double)sz + 0.5) / 4.0 - this.z;
               if (dx * dx + dz * dz <= r * r) {
                  out.add(sx, y, sz);
               }
            }
         }
      }
   }

   public static record Service(VoxelSet stand, VoxelSet drips, List<TechniqueGen.Candle> candles) {
      public VoxelSet all() {
         VoxelSet out = new VoxelSet();
         out.addAll(this.stand);
         out.addAll(this.drips);

         for (TechniqueGen.Candle c : this.candles) {
            for (int y = c.foot(); y <= c.top(); y++) {
               c.layer(y, out);
            }
         }

         return out;
      }
   }
}
