package com.devilfruits.fruits.gomu_gomu.strike;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class StrikePath {
   private static final double SHOULDER_Y = 1.29;
   private static final double SHOULDER_SIDE = 0.31;
   private static final double FEINT = 0.35;
   public static final double ARM = 0.7;
   private static final double COIL_ANGLE = Math.toRadians(165.0);

   public static double distance(StrikeKind k, double t, double end) {
      if (t < (double)k.feint) {
         return 0.35 * smooth(t / (double)k.feint);
      } else {
         int release = k.release();
         if (t < (double)release) {
            double u = (t - (double)k.feint) / (double)k.coil;
            return Mth.lerp(1.0 - Math.pow(1.0 - u, 3.0), 0.35, -k.back);
         } else {
            double shot = -k.back + k.speed * (t - (double)release);
            double tEnd = endTick(k, end);
            if (t < tEnd) {
               return shot;
            } else if (t < tEnd + (double)k.hold) {
               return end;
            } else {
               double u = Math.min(1.0, (t - tEnd - (double)k.hold) / (double)k.retract);
               return end * (1.0 - back(u));
            }
         }
      }
   }

   public static double back(double u) {
      double c = 0.9;
      return 1.0 + (c + 1.0) * Math.pow(u - 1.0, 3.0) + c * Math.pow(u - 1.0, 2.0);
   }

   public static double retracted(double from) {
      if (from <= 0.7) {
         return 0.0;
      } else {
         double want = 1.0 - 0.7 / from;
         double lo = 0.0;
         double hi = 0.53;

         for (int i = 0; i < 30; i++) {
            double mid = (lo + hi) * 0.5;
            if (back(mid) < want) {
               lo = mid;
            } else {
               hi = mid;
            }
         }

         return (lo + hi) * 0.5;
      }
   }

   public static double returnedTick(StrikeKind k, double end) {
      return endTick(k, end) + (double)k.hold + retracted(end) * (double)k.retract;
   }

   public static double endTick(StrikeKind k, double end) {
      return (double)k.release() + (end + k.back) / k.speed;
   }

   public static double doneTick(StrikeKind k, double end) {
      return endTick(k, end) + (double)k.hold + (double)k.retract;
   }

   public static boolean shooting(StrikeKind k, double t, double end) {
      return t >= (double)k.release() && t < endTick(k, end);
   }

   public static Vec3 side(Vec3 aim) {
      Vec3 flat = new Vec3(aim.x, 0.0, aim.z);
      if (flat.lengthSqr() < 1.0E-6) {
         flat = new Vec3(0.0, 0.0, 1.0);
      }

      flat = flat.normalize();
      return new Vec3(-flat.z, 0.0, flat.x);
   }

   public static Vec3 shoulder(Entity entity, Vec3 position, Vec3 aim, int arm) {
      double scale = (double)entity.getBbHeight() / 1.8;
      return position.add(0.0, 1.29 * scale, 0.0).add(side(aim).scale(0.31 * (double)arm * scale));
   }

   public static Vec3 fist(StrikeKind k, Vec3 shoulder, Vec3 aim, int arm, double d) {
      Vec3 side = side(aim).scale((double)arm);
      if (d >= 0.7) {
         Vec3 at = shoulder.add(aim.scale(d));
         return k.twoArms() ? at.subtract(side.scale(Math.min(d, 1.5) * 0.12)) : at;
      } else if (d >= 0.0) {
         return shoulder.add(aim.scale(0.7));
      } else {
         Vec3 flat = new Vec3(aim.x, 0.0, aim.z);
         flat = flat.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
         double a = Math.min(1.0, -d / 0.7);
         double theta = COIL_ANGLE * smooth(a);
         Vec3 level = flat.scale(Math.cos(theta)).add(side.scale(Math.sin(theta)));
         Vec3 dir = level.add(0.0, aim.y * (1.0 - a), 0.0).normalize();
         return shoulder.add(dir.scale(Math.max(0.7, -d)));
      }
   }

   private static double smooth(double x) {
      x = Mth.clamp(x, 0.0, 1.0);
      return x * x * (3.0 - 2.0 * x);
   }

   private StrikePath() {
   }
}
