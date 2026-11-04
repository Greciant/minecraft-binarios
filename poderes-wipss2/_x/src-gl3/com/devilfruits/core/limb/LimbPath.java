package com.devilfruits.core.limb;

import javax.annotation.Nullable;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public final class LimbPath {
   public static final int MAX_POINTS = 16;
   private static final int STEPS = 16;
   private final float[] p = new float[48];
   private final float[] out = new float[48];
   private final float[] in = new float[48];
   private final float[] acc = new float[241];
   private int count;
   private boolean start;
   private boolean end;
   private final float[] dirs = new float[6];
   private final Vector3f scratchA = new Vector3f();
   private final Vector3f scratchB = new Vector3f();
   private float length;

   public LimbPath reset() {
      this.count = 0;
      this.start = false;
      this.end = false;
      this.length = 0.0F;
      return this;
   }

   public LimbPath add(float x, float y, float z) {
      if (this.count < 16) {
         this.set(this.count++, x, y, z);
      }

      return this;
   }

   public LimbPath add(Vector3fc v) {
      return this.add(v.x(), v.y(), v.z());
   }

   public LimbPath set(int i, float x, float y, float z) {
      this.p[i * 3] = x;
      this.p[i * 3 + 1] = y;
      this.p[i * 3 + 2] = z;
      return this;
   }

   public int count() {
      return this.count;
   }

   public Vector3f point(int i, Vector3f dest) {
      return dest.set(this.p[i * 3], this.p[i * 3 + 1], this.p[i * 3 + 2]);
   }

   public LimbPath startDirection(float x, float y, float z) {
      return this.direction(0, x, y, z);
   }

   public LimbPath endDirection(float x, float y, float z) {
      return this.direction(3, x, y, z);
   }

   private LimbPath direction(int at, float x, float y, float z) {
      float l = (float)Math.sqrt((double)(x * x + y * y + z * z));
      if (l > 1.0E-6F) {
         this.dirs[at] = x / l;
         this.dirs[at + 1] = y / l;
         this.dirs[at + 2] = z / l;
         if (at == 0) {
            this.start = true;
         } else {
            this.end = true;
         }
      }

      return this;
   }

   public LimbPath build() {
      this.length = 0.0F;
      this.acc[0] = 0.0F;
      if (this.count < 2) {
         return this;
      } else {
         for (int i = 0; i < this.count - 1; i++) {
            float chord = this.dist(i, i + 1);
            this.tangent(i, chord, this.out, i * 3);
            this.tangent(i + 1, chord, this.in, (i + 1) * 3);
         }

         int k = 0;

         for (int seg = 0; seg < this.count - 1; seg++) {
            this.evalInto(seg, 0.0F, this.scratchA, null);

            for (int j = 1; j <= 16; j++) {
               this.evalInto(seg, (float)j / 16.0F, this.scratchB, null);
               this.length = this.length + this.scratchA.distance(this.scratchB);
               k++;
               this.acc[k] = this.length;
               this.scratchA.set(this.scratchB);
            }
         }

         return this;
      }
   }

   public float length() {
      return this.length;
   }

   public void sample(float s, Vector3f pos, @Nullable Vector3f tangent) {
      if (this.count < 2) {
         pos.set(this.p[0], this.p[1], this.p[2]);
         if (tangent != null) {
            tangent.set(0.0F, 1.0F, 0.0F);
         }
      } else {
         int last = (this.count - 1) * 16;
         s = Math.max(0.0F, Math.min(s, this.length));
         int lo = 0;
         int hi = last;

         while (hi - lo > 1) {
            int mid = lo + hi >>> 1;
            if (this.acc[mid] <= s) {
               lo = mid;
            } else {
               hi = mid;
            }
         }

         float span = this.acc[hi] - this.acc[lo];
         float f = span > 1.0E-6F ? (s - this.acc[lo]) / span : 0.0F;
         int seg = Math.min(lo / 16, this.count - 2);
         this.evalInto(seg, ((float)(lo - seg * 16) + f) / 16.0F, pos, tangent);
      }
   }

   private void evalInto(int seg, float t, Vector3f pos, @Nullable Vector3f tangent) {
      float t2 = t * t;
      float t3 = t2 * t;
      float h00 = 2.0F * t3 - 3.0F * t2 + 1.0F;
      float h10 = t3 - 2.0F * t2 + t;
      float h01 = -2.0F * t3 + 3.0F * t2;
      float h11 = t3 - t2;
      int i = seg * 3;
      int j = i + 3;
      pos.set(
         h00 * this.p[i] + h10 * this.out[i] + h01 * this.p[j] + h11 * this.in[j],
         h00 * this.p[i + 1] + h10 * this.out[i + 1] + h01 * this.p[j + 1] + h11 * this.in[j + 1],
         h00 * this.p[i + 2] + h10 * this.out[i + 2] + h01 * this.p[j + 2] + h11 * this.in[j + 2]
      );
      if (tangent != null) {
         float d00 = 6.0F * t2 - 6.0F * t;
         float d10 = 3.0F * t2 - 4.0F * t + 1.0F;
         float d01 = -6.0F * t2 + 6.0F * t;
         float d11 = 3.0F * t2 - 2.0F * t;
         tangent.set(
            d00 * this.p[i] + d10 * this.out[i] + d01 * this.p[j] + d11 * this.in[j],
            d00 * this.p[i + 1] + d10 * this.out[i + 1] + d01 * this.p[j + 1] + d11 * this.in[j + 1],
            d00 * this.p[i + 2] + d10 * this.out[i + 2] + d01 * this.p[j + 2] + d11 * this.in[j + 2]
         );
         float l = tangent.length();
         if (l > 1.0E-6F) {
            tangent.div(l);
         } else {
            tangent.set(this.p[j] - this.p[i], this.p[j + 1] - this.p[i + 1], this.p[j + 2] - this.p[i + 2]);
            if (tangent.lengthSquared() > 1.0E-12F) {
               tangent.normalize();
            } else {
               tangent.set(0.0F, 1.0F, 0.0F);
            }
         }
      }
   }

   private void tangent(int i, float chord, float[] dest, int at) {
      float dx;
      float dy;
      float dz;
      if (i == 0 && this.start) {
         dx = this.dirs[0];
         dy = this.dirs[1];
         dz = this.dirs[2];
      } else if (i == this.count - 1 && this.end) {
         dx = this.dirs[3];
         dy = this.dirs[4];
         dz = this.dirs[5];
      } else {
         int a = Math.max(i - 1, 0);
         int b = Math.min(i + 1, this.count - 1);
         dx = this.p[b * 3] - this.p[a * 3];
         dy = this.p[b * 3 + 1] - this.p[a * 3 + 1];
         dz = this.p[b * 3 + 2] - this.p[a * 3 + 2];
         float l = (float)Math.sqrt((double)(dx * dx + dy * dy + dz * dz));
         if (l > 1.0E-6F) {
            dx /= l;
            dy /= l;
            dz /= l;
         }
      }

      dest[at] = dx * chord;
      dest[at + 1] = dy * chord;
      dest[at + 2] = dz * chord;
   }

   private float dist(int a, int b) {
      float dx = this.p[b * 3] - this.p[a * 3];
      float dy = this.p[b * 3 + 1] - this.p[a * 3 + 1];
      float dz = this.p[b * 3 + 2] - this.p[a * 3 + 2];
      return (float)Math.sqrt((double)(dx * dx + dy * dy + dz * dz));
   }

   public static void transport(Vector3fc p0, Vector3fc t0, Vector3fc r0, Vector3fc p1, Vector3fc t1, Vector3f rOut) {
      float v1x = p1.x() - p0.x();
      float v1y = p1.y() - p0.y();
      float v1z = p1.z() - p0.z();
      float c1 = v1x * v1x + v1y * v1y + v1z * v1z;
      float rx = r0.x();
      float ry = r0.y();
      float rz = r0.z();
      float tx = t0.x();
      float ty = t0.y();
      float tz = t0.z();
      if (c1 > 1.0E-10F) {
         float kr = 2.0F / c1 * (v1x * rx + v1y * ry + v1z * rz);
         float kt = 2.0F / c1 * (v1x * tx + v1y * ty + v1z * tz);
         rx -= kr * v1x;
         ry -= kr * v1y;
         rz -= kr * v1z;
         tx -= kt * v1x;
         ty -= kt * v1y;
         tz -= kt * v1z;
      }

      float v2x = t1.x() - tx;
      float v2y = t1.y() - ty;
      float v2z = t1.z() - tz;
      float c2 = v2x * v2x + v2y * v2y + v2z * v2z;
      if (c2 > 1.0E-10F) {
         float k = 2.0F / c2 * (v2x * rx + v2y * ry + v2z * rz);
         rx -= k * v2x;
         ry -= k * v2y;
         rz -= k * v2z;
      }

      float d = rx * t1.x() + ry * t1.y() + rz * t1.z();
      rOut.set(rx - d * t1.x(), ry - d * t1.y(), rz - d * t1.z());
      if (rOut.lengthSquared() > 1.0E-12F) {
         rOut.normalize();
      } else {
         rOut.set(r0);
      }
   }
}
