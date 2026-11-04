package com.devilfruits.core.physics;

import java.util.List;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public interface ImpactShape {
   AABB bounds();

   double power(Vec3 var1);

   Vec3 center();

   static ImpactShape sphere(final Vec3 c, double r) {
      final double rr = Math.max(0.5, r);
      final AABB box = new AABB(c, c).inflate(rr);
      return new ImpactShape() {
         @Override
         public AABB bounds() {
            return box;
         }

         @Override
         public Vec3 center() {
            return c;
         }

         @Override
         public double power(Vec3 p) {
            double d = p.distanceTo(c);
            return d > rr ? 0.0 : 1.0 - 0.5 * d / rr;
         }
      };
   }

   static ImpactShape cone(final Vec3 apex, Vec3 dir, final double length, double halfAngle) {
      final Vec3 d = dir.lengthSqr() < 1.0E-8 ? new Vec3(0.0, 0.0, 1.0) : dir.normalize();
      final double cos = Math.cos(halfAngle);
      double spread = length * Math.tan(Math.min(halfAngle, 1.2)) + 1.0;
      final AABB box = new AABB(apex, apex.add(d.scale(length))).inflate(spread);
      final Vec3 mid = apex.add(d.scale(length * 0.5));
      return new ImpactShape() {
         @Override
         public AABB bounds() {
            return box;
         }

         @Override
         public Vec3 center() {
            return mid;
         }

         @Override
         public double power(Vec3 p) {
            Vec3 to = p.subtract(apex);
            double dist = to.length();
            if (dist < 1.0) {
               return 1.0;
            } else {
               double along = to.dot(d);
               return !(along <= 0.0) && !(along > length) && !(along / dist < cos) ? 1.0 - 0.5 * along / length : 0.0;
            }
         }
      };
   }

   static ImpactShape capsule(final Vec3 a, Vec3 b, final double r) {
      final Vec3 ab = b.subtract(a);
      final double len2 = Math.max(1.0E-6, ab.lengthSqr());
      final AABB box = new AABB(a, b).inflate(r);
      final Vec3 mid = a.add(ab.scale(0.5));
      return new ImpactShape() {
         @Override
         public AABB bounds() {
            return box;
         }

         @Override
         public Vec3 center() {
            return mid;
         }

         @Override
         public double power(Vec3 p) {
            double t = Math.max(0.0, Math.min(1.0, p.subtract(a).dot(ab) / len2));
            double d = p.distanceTo(a.add(ab.scale(t)));
            return d > r ? 0.0 : 1.0 - 0.4 * d / r;
         }
      };
   }

   static ImpactShape blade(final Vec3 a, final Vec3 b, final double halfWidth, final double below, final double above) {
      final Vec3 ab = new Vec3(b.x - a.x, 0.0, b.z - a.z);
      final double len2 = ab.lengthSqr();
      final AABB box = new AABB(a, b).inflate(halfWidth, 0.0, halfWidth).expandTowards(0.0, above, 0.0).expandTowards(0.0, -below, 0.0);
      final Vec3 mid = a.add(b).scale(0.5);
      return new ImpactShape() {
         @Override
         public AABB bounds() {
            return box;
         }

         @Override
         public Vec3 center() {
            return mid;
         }

         @Override
         public double power(Vec3 p) {
            double t = len2 < 1.0E-6 ? 0.0 : Math.max(0.0, Math.min(1.0, ((p.x - a.x) * ab.x + (p.z - a.z) * ab.z) / len2));
            double cx = a.x + ab.x * t;
            double cz = a.z + ab.z * t;
            double cy = a.y + (b.y - a.y) * t;
            double dx = p.x - cx;
            double dz = p.z - cz;
            return dx * dx + dz * dz <= halfWidth * halfWidth && p.y >= cy - below && p.y <= cy + above ? 1.0 : 0.0;
         }
      };
   }

   static ImpactShape ring(final Vec3 c, final double inner, final double outer, final double halfHeight) {
      final AABB box = new AABB(c.x - outer, c.y - halfHeight, c.z - outer, c.x + outer, c.y + halfHeight, c.z + outer);
      final double midR = (inner + outer) * 0.5;
      final double half = Math.max(0.5, (outer - inner) * 0.5);
      return new ImpactShape() {
         @Override
         public AABB bounds() {
            return box;
         }

         @Override
         public Vec3 center() {
            return c;
         }

         @Override
         public double power(Vec3 p) {
            if (Math.abs(p.y - c.y) > halfHeight) {
               return 0.0;
            } else {
               double dx = p.x - c.x;
               double dz = p.z - c.z;
               double d = Math.sqrt(dx * dx + dz * dz);
               return !(d < inner) && !(d > outer) ? 1.0 - 0.5 * Math.min(1.0, Math.abs(d - midR) / half) : 0.0;
            }
         }
      };
   }

   static ImpactShape cylinder(final Vec3 base, final double r, final double height) {
      final AABB box = new AABB(base.x - r, base.y, base.z - r, base.x + r, base.y + height, base.z + r);
      final Vec3 mid = base.add(0.0, height * 0.5, 0.0);
      return new ImpactShape() {
         @Override
         public AABB bounds() {
            return box;
         }

         @Override
         public Vec3 center() {
            return mid;
         }

         @Override
         public double power(Vec3 p) {
            if (!(p.y < base.y) && !(p.y > base.y + height)) {
               double dx = p.x - base.x;
               double dz = p.z - base.z;
               double d = Math.sqrt(dx * dx + dz * dz);
               return d > r ? 0.0 : 1.0 - 0.5 * d / r;
            } else {
               return 0.0;
            }
         }
      };
   }

   static ImpactShape union(final List<ImpactShape> parts) {
      if (parts.size() == 1) {
         return parts.get(0);
      } else {
         final AABB box = parts.get(0).bounds();
         Vec3 sum = Vec3.ZERO;

         for (ImpactShape s : parts) {
            box = box.minmax(s.bounds());
            sum = sum.add(s.center());
         }

         final Vec3 mid = sum.scale(1.0 / (double)parts.size());
         return new ImpactShape() {
            @Override
            public AABB bounds() {
               return box;
            }

            @Override
            public Vec3 center() {
               return mid;
            }

            @Override
            public double power(Vec3 p) {
               double best = 0.0;

               for (ImpactShape s : parts) {
                  best = Math.max(best, s.power(p));
               }

               return best;
            }
         };
      }
   }
}
