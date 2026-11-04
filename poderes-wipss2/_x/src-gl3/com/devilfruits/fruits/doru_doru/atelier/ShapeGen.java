package com.devilfruits.fruits.doru_doru.atelier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import javax.annotation.Nullable;
import net.minecraft.world.phys.Vec3;

public final class ShapeGen {
   public static final long MAX_EVAL = 4000000L;

   @Nullable
   public static VoxelSet generate(SculptRequest r) {
      ShapeKind kind = r.shape();
      if (r.points().size() < kind.points()) {
         return null;
      } else {
         VoxelSet out = new VoxelSet();

         for (List<Vec3> pts : symmetric(r.points().subList(0, kind.points()), r)) {
            ShapeGen.Field f = field(kind, pts, r);
            if (f == null || !rasterize(f, r.shell(), out)) {
               return null;
            }
         }

         return out;
      }
   }

   public static List<List<Vec3>> symmetric(List<Vec3> pts, SculptRequest r) {
      List<List<Vec3>> out = new ArrayList<>();
      out.add(pts);
      Vec3 o = r.origin();
      switch (r.symmetry()) {
         case MIRROR_X:
            out.add(map(pts, p -> new Vec3(2.0 * o.x - p.x, p.y, p.z)));
            break;
         case MIRROR_Z:
            out.add(map(pts, p -> new Vec3(p.x, p.y, 2.0 * o.z - p.z)));
            break;
         case MIRROR_XZ:
            out.add(map(pts, p -> new Vec3(2.0 * o.x - p.x, p.y, p.z)));
            out.add(map(pts, p -> new Vec3(p.x, p.y, 2.0 * o.z - p.z)));
            out.add(map(pts, p -> new Vec3(2.0 * o.x - p.x, p.y, 2.0 * o.z - p.z)));
            break;
         case RADIAL:
            int n = Math.max(2, Math.min(12, r.folds()));

            for (int k = 1; k < n; k++) {
               double a = (Math.PI * 2) * (double)k / (double)n;
               double c = Math.cos(a);
               double s = Math.sin(a);
               out.add(map(pts, p -> new Vec3(o.x + (p.x - o.x) * c - (p.z - o.z) * s, p.y, o.z + (p.x - o.x) * s + (p.z - o.z) * c)));
            }
      }

      return out;
   }

   private static List<Vec3> map(List<Vec3> pts, UnaryOperator<Vec3> f) {
      List<Vec3> out = new ArrayList<>(pts.size());

      for (Vec3 p : pts) {
         out.add(f.apply(p));
      }

      return out;
   }

   public static boolean rasterize(ShapeGen.Field f, float shell, VoxelSet out) {
      double[] b = f.bounds();
      int x0 = (int)Math.floor(b[0] * 4.0);
      int y0 = (int)Math.floor(b[1] * 4.0);
      int z0 = (int)Math.floor(b[2] * 4.0);
      int x1 = (int)Math.ceil(b[3] * 4.0);
      int y1 = (int)Math.ceil(b[4] * 4.0);
      int z1 = (int)Math.ceil(b[5] * 4.0);
      long volume = (long)(x1 - x0) * (long)(y1 - y0) * (long)(z1 - z0);
      if (volume <= 4000000L && volume >= 0L) {
         for (int sy = y0; sy < y1; sy++) {
            double y = ((double)sy + 0.5) / 4.0;

            for (int sz = z0; sz < z1; sz++) {
               double z = ((double)sz + 0.5) / 4.0;

               for (int sx = x0; sx < x1; sx++) {
                  double x = ((double)sx + 0.5) / 4.0;
                  if (f.sdf(x, y, z) <= 0.0 && (shell <= 0.0F || f.shell(x, y, z) > (double)(-shell))) {
                     out.add(sx, sy, sz);
                  }
               }
            }
         }

         return true;
      } else {
         return false;
      }
   }

   @Nullable
   static ShapeGen.Field field(ShapeKind kind, List<Vec3> p, SculptRequest r) {
      float a = r.param(0);
      float b = r.param(1);
      float c = r.param(2);

      return switch (kind) {
         case WALL -> wall(p.get(0), p.get(1), Math.max(0.25, (double)a), Math.max(0.25, (double)b));
         case BOX -> box(p.get(0), p.get(1), Math.max(0.25, (double)a));
         case COLUMN -> loft(p.get(0), p.get(1), Math.max(0.25, (double)a), Math.max(0.25, (double)a));
         case CONE -> loft(p.get(0), p.get(1), Math.max(0.25, (double)a), 0.0);
         case LOFT -> loft(p.get(0), p.get(1), Math.max(0.25, (double)a), (double)Math.max(0.0F, b));
         case SPHERE -> sphere(p.get(0), Math.max(0.5, (double)a), false);
         case DOME -> sphere(p.get(0), Math.max(0.5, (double)a), true);
         case ARCH -> arch(p.get(0), p.get(1), Math.max(0.5, (double)a), Math.max(0.25, (double)b), Math.max(0.25, (double)c));
         case BRIDGE -> bridge(p.get(0), p.get(1), p.get(2), Math.max(0.25, (double)a));
         case RAMP -> ramp(p.get(0), p.get(1), Math.max(0.25, (double)a), Math.max(0.25, (double)b), false);
         case STAIRS -> ramp(p.get(0), p.get(1), Math.max(0.25, (double)a), Math.max(0.25, (double)b), true);
         case SPIRAL -> spiral(p.get(0), p.get(1), (double)Math.max(1.0F, a), Math.max(0.5, (double)b), (double)Math.max(1.0F, c));
         case RING -> ring(p.get(0), Math.max(0.5, (double)a), Math.max(0.25, (double)b));
      };
   }

   private static double[] box(double x0, double y0, double z0, double x1, double y1, double z1, double pad) {
      return new double[]{
         Math.min(x0, x1) - pad, Math.min(y0, y1) - pad, Math.min(z0, z1) - pad, Math.max(x0, x1) + pad, Math.max(y0, y1) + pad, Math.max(z0, z1) + pad
      };
   }

   private static double boxDist(double dx, double dy, double dz, double hx, double hy, double hz) {
      double qx = Math.abs(dx) - hx;
      double qy = Math.abs(dy) - hy;
      double qz = Math.abs(dz) - hz;
      double ox = Math.max(qx, 0.0);
      double oy = Math.max(qy, 0.0);
      double oz = Math.max(qz, 0.0);
      return Math.sqrt(ox * ox + oy * oy + oz * oz) + Math.min(Math.max(qx, Math.max(qy, qz)), 0.0);
   }

   static ShapeGen.Field wall(final Vec3 a, final Vec3 b, final double height, final double thick) {
      double dx = b.x - a.x;
      double dz = b.z - a.z;
      final double len = Math.max(0.001, Math.sqrt(dx * dx + dz * dz));
      final double ux = dx / len;
      final double uz = dz / len;
      return new ShapeGen.Field() {
         @Override
         public double sdf(double x, double y, double z) {
            double px = x - a.x;
            double pz = z - a.z;
            double along = px * ux + pz * uz;
            double across = -px * uz + pz * ux;
            double s = Math.max(0.0, Math.min(1.0, along / len));
            double base = a.y + (b.y - a.y) * s;
            return ShapeGen.boxDist(along - len / 2.0, y - (base + height / 2.0), across, len / 2.0 + 0.25, height / 2.0, thick / 2.0);
         }

         @Override
         public double[] bounds() {
            return ShapeGen.box(a.x, Math.min(a.y, b.y), a.z, b.x, Math.max(a.y, b.y) + height, b.z, thick + 0.5);
         }
      };
   }

   static ShapeGen.Field box(Vec3 a, Vec3 b, double thick) {
      double minY = Math.min(a.y, b.y);
      double maxY = Math.max(a.y, b.y);
      if (maxY - minY < 0.26) {
         maxY = minY + thick;
      }

      final double cx = (a.x + b.x) / 2.0;
      final double cy = (minY + maxY) / 2.0;
      final double cz = (a.z + b.z) / 2.0;
      final double hx = Math.abs(b.x - a.x) / 2.0 + 0.25;
      final double hy = (maxY - minY) / 2.0;
      final double hz = Math.abs(b.z - a.z) / 2.0 + 0.25;
      final double top = maxY;
      return new ShapeGen.Field() {
         @Override
         public double sdf(double x, double y, double z) {
            return ShapeGen.boxDist(x - cx, y - cy, z - cz, hx, hy, hz);
         }

         @Override
         public double[] bounds() {
            return ShapeGen.box(cx - hx, cy - hy, cz - hz, cx + hx, top, cz + hz, 0.25);
         }
      };
   }

   static ShapeGen.Field loft(final Vec3 a, Vec3 b, final double ra, final double rb) {
      final double bax = b.x - a.x;
      final double bay = b.y - a.y;
      final double baz = b.z - a.z;
      final double baba = bax * bax + bay * bay + baz * baz;
      if (baba < 1.0E-4) {
         bay = 0.25;
         baba = bay * bay;
      }

      final double len = Math.sqrt(baba);
      final double rba = rb - ra;
      final double slope = Math.sqrt(1.0 + rba * rba / baba);
      final double r = Math.max(ra, rb);
      return new ShapeGen.Field() {
         @Override
         public double sdf(double x, double y, double z) {
            double pax = x - a.x;
            double pay = y - a.y;
            double paz = z - a.z;
            double papa = pax * pax + pay * pay + paz * paz;
            double paba = (pax * bax + pay * bay + paz * baz) / baba;
            double xx = Math.sqrt(Math.max(0.0, papa - paba * paba * baba));
            double cax = Math.max(0.0, xx - (paba < 0.5 ? ra : rb));
            double cay = Math.abs(paba - 0.5) - 0.5;
            double k = rba * rba + baba;
            double f = Math.max(0.0, Math.min(1.0, (rba * (xx - ra) + paba * baba) / k));
            double cbx = xx - ra - f * rba;
            double cby = paba - f;
            double s = cbx < 0.0 && cay < 0.0 ? -1.0 : 1.0;
            return s * Math.sqrt(Math.min(cax * cax + cay * cay * baba, cbx * cbx + cby * cby * baba));
         }

         @Override
         public double shell(double x, double y, double z) {
            double pax = x - a.x;
            double pay = y - a.y;
            double paz = z - a.z;
            double paba = (pax * bax + pay * bay + paz * baz) / baba;
            double xx = Math.sqrt(Math.max(0.0, pax * pax + pay * pay + paz * paz - paba * paba * baba));
            double rt = ra + rba * Math.max(0.0, Math.min(1.0, paba));
            return (xx - rt) / slope;
         }

         @Override
         public double[] bounds() {
            return ShapeGen.box(a.x, a.y, a.z, a.x + bax, a.y + bay, a.z + baz, r + 0.5 + (len < 0.3 ? 0.25 : 0.0));
         }
      };
   }

   static ShapeGen.Field sphere(final Vec3 c, final double radius, final boolean dome) {
      return new ShapeGen.Field() {
         @Override
         public double sdf(double x, double y, double z) {
            double d = Math.sqrt((x - c.x) * (x - c.x) + (y - c.y) * (y - c.y) + (z - c.z) * (z - c.z)) - radius;
            return dome ? Math.max(d, c.y - y) : d;
         }

         @Override
         public double shell(double x, double y, double z) {
            return Math.sqrt((x - c.x) * (x - c.x) + (y - c.y) * (y - c.y) + (z - c.z) * (z - c.z)) - radius;
         }

         @Override
         public double[] bounds() {
            return new double[]{
               c.x - radius - 0.5, dome ? c.y : c.y - radius - 0.5, c.z - radius - 0.5, c.x + radius + 0.5, c.y + radius + 0.5, c.z + radius + 0.5
            };
         }
      };
   }

   static ShapeGen.Field arch(final Vec3 a, final Vec3 b, final double rise, final double thick, final double depth) {
      double dx = b.x - a.x;
      double dz = b.z - a.z;
      double len = Math.max(0.5, Math.sqrt(dx * dx + dz * dz));
      final double ux = dx / len;
      final double uz = dz / len;
      final double mx = (a.x + b.x) / 2.0;
      final double my = Math.min(a.y, b.y);
      final double mz = (a.z + b.z) / 2.0;
      final double ha = len / 2.0;
      return new ShapeGen.Field() {
         @Override
         public double sdf(double x, double y, double z) {
            double px = x - mx;
            double pz = z - mz;
            double py = y - my;
            double u = px * ux + pz * uz;
            double w = -px * uz + pz * ux;
            double fa = u / ha;
            double fb = py / rise;
            double f = fa * fa + fb * fb - 1.0;
            double gx = 2.0 * u / (ha * ha);
            double gy = 2.0 * py / (rise * rise);
            double g = Math.sqrt(gx * gx + gy * gy);
            double d = g < 1.0E-6 ? Math.min(ha, rise) : f / g;
            return Math.max(Math.max(Math.abs(d) - thick / 2.0, Math.abs(w) - depth / 2.0), -py);
         }

         @Override
         public double[] bounds() {
            double pad = thick + depth + 0.5;
            return ShapeGen.box(a.x, my, a.z, b.x, my + rise + thick, b.z, pad);
         }
      };
   }

   static ShapeGen.Field bridge(Vec3 a, Vec3 b, Vec3 c, double thick) {
      final int n = 40;
      final double[] xs = new double[n + 1];
      final double[] ys = new double[n + 1];
      final double[] zs = new double[n + 1];
      double qx = 2.0 * c.x - (a.x + b.x) / 2.0;
      double qy = 2.0 * c.y - (a.y + b.y) / 2.0;
      double qz = 2.0 * c.z - (a.z + b.z) / 2.0;
      double minX = Double.MAX_VALUE;
      double minY = minX;
      double minZ = minX;
      double maxX = -minX;
      double maxY = maxX;
      double maxZ = maxX;

      for (int i = 0; i <= n; i++) {
         double t = (double)i / (double)n;
         double u = 1.0 - t;
         xs[i] = u * u * a.x + 2.0 * u * t * qx + t * t * b.x;
         ys[i] = u * u * a.y + 2.0 * u * t * qy + t * t * b.y;
         zs[i] = u * u * a.z + 2.0 * u * t * qz + t * t * b.z;
         minX = Math.min(minX, xs[i]);
         minY = Math.min(minY, ys[i]);
         minZ = Math.min(minZ, zs[i]);
         maxX = Math.max(maxX, xs[i]);
         maxY = Math.max(maxY, ys[i]);
         maxZ = Math.max(maxZ, zs[i]);
      }

      final double r = thick / 2.0;
      final double[] bb = new double[]{minX - r - 0.5, minY - r - 0.5, minZ - r - 0.5, maxX + r + 0.5, maxY + r + 0.5, maxZ + r + 0.5};
      return new ShapeGen.Field() {
         @Override
         public double sdf(double x, double y, double z) {
            double best = Double.MAX_VALUE;

            for (int i = 0; i < n; i++) {
               double sx = xs[i + 1] - xs[i];
               double sy = ys[i + 1] - ys[i];
               double sz = zs[i + 1] - zs[i];
               double px = x - xs[i];
               double py = y - ys[i];
               double pz = z - zs[i];
               double ss = sx * sx + sy * sy + sz * sz;
               double t = ss < 1.0E-9 ? 0.0 : Math.max(0.0, Math.min(1.0, (px * sx + py * sy + pz * sz) / ss));
               double ex = px - sx * t;
               double ey = py - sy * t;
               double ez = pz - sz * t;
               best = Math.min(best, ex * ex + ey * ey + ez * ez);
            }

            return Math.sqrt(best) - r;
         }

         @Override
         public double[] bounds() {
            return bb;
         }
      };
   }

   static ShapeGen.Field ramp(final Vec3 a, final Vec3 b, final double width, final double thick, final boolean stairs) {
      double dx = b.x - a.x;
      double dz = b.z - a.z;
      final double len = Math.max(0.5, Math.sqrt(dx * dx + dz * dz));
      final double ux = dx / len;
      final double uz = dz / len;
      final double rise = b.y - a.y;
      final int steps = Math.max(1, (int)Math.ceil(Math.abs(rise)));
      return new ShapeGen.Field() {
         @Override
         public double sdf(double x, double y, double z) {
            double px = x - a.x;
            double pz = z - a.z;
            double along = px * ux + pz * uz;
            double across = -px * uz + pz * ux;
            double s = Math.max(0.0, Math.min(1.0, along / len));
            double top;
            if (stairs) {
               int k = Math.min(steps - 1, (int)Math.floor(s * (double)steps));
               top = a.y + rise * (double)(k + 1) / (double)steps;
            } else {
               top = a.y + rise * s;
            }

            double out = Math.max(Math.max(-along, along - len), Math.abs(across) - width / 2.0);
            double vert = Math.max(y - top, top - thick - y);
            return Math.max(out, vert);
         }

         @Override
         public double[] bounds() {
            return ShapeGen.box(a.x, Math.min(a.y, b.y) - thick, a.z, b.x, Math.max(a.y, b.y), b.z, width / 2.0 + 0.5);
         }
      };
   }

   static ShapeGen.Field spiral(final Vec3 a, Vec3 b, final double radius, double width, final double pitch) {
      final double height = Math.max(1.0, b.y - a.y);
      final double inner = Math.max(0.0, radius - width);
      final double tread = 0.5;
      return new ShapeGen.Field() {
         @Override
         public double sdf(double x, double y, double z) {
            double px = x - a.x;
            double pz = z - a.z;
            double py = y - a.y;
            double r = Math.sqrt(px * px + pz * pz);
            double turn = (Math.atan2(pz, px) / (Math.PI * 2) + 1.0) % 1.0;
            double k = (double)Math.round(py / pitch - turn);
            double surface = (turn + k) * pitch;
            double radial = Math.max(r - radius, inner - r);
            double vert = Math.max(py - surface, surface - tread - py);
            double clip = Math.max(-py - tread, py - height);
            return Math.max(Math.max(radial, vert), clip);
         }

         @Override
         public double[] bounds() {
            return new double[]{a.x - radius - 0.5, a.y - tread - 0.5, a.z - radius - 0.5, a.x + radius + 0.5, a.y + height + 0.5, a.z + radius + 0.5};
         }
      };
   }

   static ShapeGen.Field ring(final Vec3 c, final double radius, final double thick) {
      final double r = thick / 2.0;
      return new ShapeGen.Field() {
         @Override
         public double sdf(double x, double y, double z) {
            double q = Math.sqrt((x - c.x) * (x - c.x) + (z - c.z) * (z - c.z)) - radius;
            double h = y - (c.y + r);
            return Math.sqrt(q * q + h * h) - r;
         }

         @Override
         public double[] bounds() {
            return new double[]{c.x - radius - r - 0.5, c.y - 0.5, c.z - radius - r - 0.5, c.x + radius + r + 0.5, c.y + thick + 0.5, c.z + radius + r + 0.5};
         }
      };
   }

   private ShapeGen() {
   }

   public interface Field {
      double sdf(double var1, double var3, double var5);

      default double shell(double x, double y, double z) {
         return this.sdf(x, y, z);
      }

      double[] bounds();
   }
}
