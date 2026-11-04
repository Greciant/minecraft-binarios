package com.devilfruits.fruits.gomu_gomu.strike;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class GatlingPath {
   public static final int PER_TICK = 2;
   public static final int OUT = 2;
   public static final int BACK = 2;
   public static final double REACH = 4.0;
   public static final double CONE = Math.toRadians(3.0);
   private static final double SPREAD_SIDE = 0.95;
   private static final double SPREAD_UP = 0.85;
   private static final double SPREAD_DOWN = 0.55;

   public static int arm(int index) {
      return (index & 1) == 0 ? 1 : -1;
   }

   private static double rand(long seed, int index, int salt) {
      long h = seed * -7046029254386353131L + (long)index * -4417276706812531889L + (long)salt * 1609587929392839161L;
      h ^= h >>> 31;
      h *= -4658895280553007687L;
      h ^= h >>> 29;
      return (double)(h >>> 11) * 1.110223E-16F;
   }

   public static Vec3 origin(long seed, int index, Vec3 centre, Vec3 aim) {
      Vec3 flat = new Vec3(aim.x, 0.0, aim.z);
      flat = flat.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
      double angle = Math.toRadians((rand(seed, index, 1) - 0.5) * 200.0);
      double radius = 0.35 + 0.65 * Math.sqrt(rand(seed, index, 2));
      double up = Math.sin(angle) * radius;
      Vec3 side = StrikePath.side(aim).scale((double)arm(index) * Math.cos(angle) * radius * 0.95);
      double back = 0.1 + rand(seed, index, 3) * 0.25;
      return centre.add(side).add(0.0, up * (up > 0.0 ? 0.85 : 0.55), 0.0).subtract(flat.scale(back));
   }

   public static Vec3 centre(Entity entity, Vec3 position) {
      return position.add(0.0, 1.29 * (double)entity.getBbHeight() / 1.8, 0.0);
   }

   public static Vec3 target(long seed, int index, Vec3 from, Vec3 aim) {
      double r = Math.sqrt(rand(seed, index, 4)) * Math.tan(CONE);
      double a = rand(seed, index, 5) * Math.PI * 2.0;
      Vec3 side = StrikePath.side(aim);
      Vec3 up = side.cross(aim).normalize();
      Vec3 dir = aim.add(side.scale(r * Math.cos(a))).add(up.scale(r * Math.sin(a))).normalize();
      return from.add(dir.scale(4.0));
   }

   public static double bow(long seed, int index) {
      return 0.25 + rand(seed, index, 6) * 0.3;
   }

   public static Vec3 along(long seed, int index, Vec3 from, Vec3 to, Vec3 aim, double u) {
      Vec3 off = from.subtract(to);
      Vec3 out = off.subtract(aim.scale(off.dot(aim)));
      out = out.lengthSqr() < 1.0E-6 ? StrikePath.side(aim).scale((double)arm(index)) : out.normalize();
      Vec3 mid = from.lerp(to, 0.5).add(out.scale(bow(seed, index)));
      double v = 1.0 - u;
      return from.scale(v * v).add(mid.scale(2.0 * u * v)).add(to.scale(u * u));
   }

   public static double extension(double age) {
      if (age < 0.0 || age >= 4.0) {
         return -1.0;
      } else if (age < 2.0) {
         double u = age / 2.0;
         return 1.0 - (1.0 - u) * (1.0 - u);
      } else {
         double u = (age - 2.0) / 2.0;
         return 1.0 - u * u * (3.0 - 2.0 * u);
      }
   }

   public static int first(int tick) {
      return tick * 2;
   }

   public static double clampAge(double age) {
      return Mth.clamp(age, 0.0, 4.0);
   }

   private GatlingPath() {
   }
}
