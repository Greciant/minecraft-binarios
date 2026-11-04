package com.awakenedmod.awakened.client.render.limitless;

import org.joml.Vector3f;

public record SpaceBend(Vector3f axis, Vector3f start, float radius, float turns, float lift, float tighten) {
   private static final float TWO_PI = (float) (Math.PI * 2);

   public Vector3f at(float s, Vector3f out) {
      s = Math.max(0.0F, Math.min(1.0F, s));
      float k = 1.0F - s;
      float r = this.radius * (float)Math.pow((double)k, (double)this.tighten);
      float ang = this.turns * (float) (Math.PI * 2) * (1.0F - (float)Math.sqrt((double)k));
      float c = (float)Math.cos((double)ang);
      float sn = (float)Math.sin((double)ang);
      float bx = this.axis.y * this.start.z - this.axis.z * this.start.y;
      float by = this.axis.z * this.start.x - this.axis.x * this.start.z;
      float bz = this.axis.x * this.start.y - this.axis.y * this.start.x;
      float h = this.lift * r;
      return out.set(
         (this.start.x * c + bx * sn) * r + this.axis.x * h,
         (this.start.y * c + by * sn) * r + this.axis.y * h,
         (this.start.z * c + bz * sn) * r + this.axis.z * h
      );
   }

   public static SpaceBend random(long seed, float rMin, float rMax, float turnsMin, float turnsMax, float lift, float tighten) {
      Vector3f axis = new Vector3f(hash(seed, 1) * 2.0F - 1.0F, hash(seed, 2) * 2.0F - 1.0F, hash(seed, 3) * 2.0F - 1.0F);
      if (axis.lengthSquared() < 1.0E-4F) {
         axis.set(0.0F, 1.0F, 0.0F);
      }

      axis.normalize();
      Vector3f[] plane = LimitlessMesh.planeAxes(axis);
      float a = hash(seed, 4) * (float) (Math.PI * 2);
      Vector3f start = new Vector3f(plane[0]).mul((float)Math.cos((double)a)).add(new Vector3f(plane[1]).mul((float)Math.sin((double)a)));
      float sign = hash(seed, 7) < 0.5F ? -1.0F : 1.0F;
      return new SpaceBend(
         axis,
         start.normalize(),
         rMin + (rMax - rMin) * hash(seed, 5),
         sign * (turnsMin + (turnsMax - turnsMin) * hash(seed, 6)),
         lift * (hash(seed, 8) * 2.0F - 1.0F),
         tighten
      );
   }

   public static float fall(float u, float power) {
      u = Math.max(0.0F, Math.min(1.0F, u));
      return (float)Math.pow((double)u, (double)power);
   }

   public static float hash(long seed, int salt) {
      long h = seed * -7046029254386353131L + (long)salt * -4417276706812531889L;
      h ^= h >>> 33;
      h *= -49064778989728563L;
      h ^= h >>> 33;
      h *= -4265267296055464877L;
      h ^= h >>> 33;
      return (float)(h >>> 40) / 1.6777216E7F;
   }
}
