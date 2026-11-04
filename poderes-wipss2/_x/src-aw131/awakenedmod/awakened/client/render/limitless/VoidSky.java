package com.awakenedmod.awakened.client.render.limitless;

import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class VoidSky {
   private static final float TWO_PI = (float) (Math.PI * 2);
   public static final float INSET = 0.985F;
   private float ox;
   private float oy;
   private float oz;
   private float occ;
   private float radius;
   private float detach;

   public VoidSky set(Vec3 cam, Vec3 centre, double radius) {
      this.ox = (float)(cam.x - centre.x);
      this.oy = (float)(cam.y - centre.y);
      this.oz = (float)(cam.z - centre.z);
      this.radius = (float)radius;
      this.occ = this.ox * this.ox + this.oy * this.oy + this.oz * this.oz - this.radius * this.radius;
      this.detach = 0.0F;
      return this;
   }

   public VoidSky detach(float w) {
      this.detach = Math.max(0.0F, Math.min(1.0F, w));
      return this;
   }

   public boolean inside() {
      return this.occ < 0.0F;
   }

   public float radius() {
      return this.radius;
   }

   public Vector3f offset(Vector3f out) {
      return out.set(this.ox, this.oy, this.oz);
   }

   public float reach(float dx, float dy, float dz) {
      float b = dx * this.ox + dy * this.oy + dz * this.oz;
      return -b + (float)Math.sqrt((double)Math.max(0.0F, b * b - this.occ));
   }

   public Vector3f at(Vector3f dir, float inset) {
      return this.at(dir.x, dir.y, dir.z, inset, new Vector3f());
   }

   public Vector3f at(float dx, float dy, float dz, float inset, Vector3f out) {
      boolean in = this.inside();
      float w = in ? this.detach : 1.0F;
      float wx = 0.0F;
      float wy = 0.0F;
      float wz = 0.0F;
      if (w > 0.0F) {
         float k = this.radius * (in ? inset : 2.0F - inset);
         wx = dx * k - this.ox;
         wy = dy * k - this.oy;
         wz = dz * k - this.oz;
         if (w >= 1.0F) {
            return out.set(wx, wy, wz);
         }
      }

      float t = this.reach(dx, dy, dz) * inset;
      return out.set(dx * t + (wx - dx * t) * w, dy * t + (wy - dy * t) * w, dz * t + (wz - dz * t) * w);
   }

   public float span(Vector3f p) {
      float w = this.inside() ? this.detach : 1.0F;
      float d = p.length();
      return d + (this.radius - d) * w;
   }

   public static Vector3f around(Vector3f dir, Vector3f u, Vector3f v, float angle, float az, Vector3f out) {
      float s = (float)Math.sin((double)angle);
      float c = (float)Math.cos((double)angle);
      float cu = (float)Math.cos((double)az) * s;
      float sv = (float)Math.sin((double)az) * s;
      return out.set(dir.x * c + u.x * cu + v.x * sv, dir.y * c + u.y * cu + v.y * sv, dir.z * c + u.z * cu + v.z * sv);
   }

   public static Vector3f domeDir(int rings, int segs, int i, int j, Vector3f out) {
      float lat = (float)(Math.PI * (double)i / (double)rings - (Math.PI / 2));
      float cl = (float)Math.cos((double)lat);
      float az = (float) (Math.PI * 2) * (float)j / (float)segs;
      return out.set(cl * (float)Math.cos((double)az), (float)Math.sin((double)lat), cl * (float)Math.sin((double)az));
   }

   public void dome(LimitlessMesh m, int rings, int segs, VoidSky.Tint tint, float inset) {
      int[] colors = new int[(rings + 1) * (segs + 1)];
      Vector3f d = new Vector3f();

      for (int i = 0; i <= rings; i++) {
         for (int j = 0; j <= segs; j++) {
            colors[i * (segs + 1) + j] = tint.color(domeDir(rings, segs, i, j, d));
         }
      }

      this.dome(m, rings, segs, colors, 1.0F, inset);
   }

   public void dome(LimitlessMesh m, int rings, int segs, int[] colors, float alpha, float inset) {
      if (!(alpha <= 0.0F)) {
         Vector3f[] prevP = new Vector3f[segs + 1];
         Vector3f[] curP = new Vector3f[segs + 1];
         int[] prevC = new int[segs + 1];
         int[] curC = new int[segs + 1];
         Vector3f d = new Vector3f();

         for (int i = 0; i <= rings; i++) {
            for (int j = 0; j <= segs; j++) {
               curP[j] = this.at(domeDir(rings, segs, i, j, d), inset);
               int c = colors[i * (segs + 1) + j];
               curC[j] = alpha >= 1.0F ? c : LimitlessMesh.fade(c, alpha);
            }

            if (i > 0) {
               for (int j = 0; j < segs; j++) {
                  m.quad(prevP[j], prevC[j], prevP[j + 1], prevC[j + 1], curP[j + 1], curC[j + 1], curP[j], curC[j]);
               }
            }

            Vector3f[] tp = prevP;
            prevP = curP;
            curP = tp;
            int[] tc = prevC;
            prevC = curC;
            curC = tc;
         }
      }
   }

   public static int light(float r, float g, float b) {
      float k = Math.max(r, Math.max(g, b));
      if (k <= 1.0E-4F) {
         return 0;
      } else {
         float a = Math.min(1.0F, k);
         return LimitlessMesh.argb(Math.round(a * 255.0F), Math.round(r / k * 255.0F), Math.round(g / k * 255.0F), Math.round(b / k * 255.0F));
      }
   }

   public void disc(LimitlessMesh m, Vector3f dir, float ang, int segs, int inner, int outer, float inset) {
      Vector3f[] ax = LimitlessMesh.planeAxes(dir);
      Vector3f c = this.at(dir, inset);
      int bands = Math.max(1, (int)Math.ceil((double)(ang / 0.06F)));
      Vector3f d = new Vector3f();
      Vector3f[] prev = new Vector3f[segs + 1];
      Vector3f[] cur = new Vector3f[segs + 1];
      int prevC = inner;

      for (int b = 1; b <= bands; b++) {
         float f = (float)b / (float)bands;
         int cc = LimitlessMesh.mix(inner, outer, f);

         for (int i = 0; i <= segs; i++) {
            cur[i] = this.at(around(dir, ax[0], ax[1], ang * f, (float) (Math.PI * 2) * (float)i / (float)segs, d), inset);
         }

         for (int i = 0; i < segs; i++) {
            if (b == 1) {
               m.tri(c, inner, cur[i], cc, cur[i + 1], cc);
            } else {
               m.quad(prev[i], prevC, prev[i + 1], prevC, cur[i + 1], cc, cur[i], cc);
            }
         }

         Vector3f[] t = prev;
         prev = cur;
         cur = t;
         prevC = cc;
      }
   }

   public void blob(LimitlessMesh m, Vector3f dir, Vector3f u, float angU, float angV, int segs, int inner, int outer, float inset) {
      Vector3f v = new Vector3f(dir).cross(u).normalize();
      Vector3f c = this.at(dir, inset);
      Vector3f prev = null;
      Vector3f d = new Vector3f();

      for (int i = 0; i <= segs; i++) {
         float a = (float) (Math.PI * 2) * (float)i / (float)segs;
         float cu = (float)Math.cos((double)a) * angU;
         float sv = (float)Math.sin((double)a) * angV;
         d.set(dir.x + u.x * cu + v.x * sv, dir.y + u.y * cu + v.y * sv, dir.z + u.z * cu + v.z * sv).normalize();
         Vector3f p = this.at(d, inset);
         if (prev != null) {
            m.tri(c, inner, prev, outer, p, outer);
         }

         prev = p;
      }
   }

   public void ring(LimitlessMesh m, Vector3f dir, float ang, float half, int segs, int mid, int edge, float inset) {
      this.ring(m, dir, ang - half, ang, ang + half, segs, edge, mid, edge, inset);
   }

   public void ring(LimitlessMesh m, Vector3f dir, float a0, float a1, float a2, int segs, int c0, int c1, int c2, float inset) {
      Vector3f[] ax = LimitlessMesh.planeAxes(dir);
      Vector3f d = new Vector3f();
      Vector3f p0 = null;
      Vector3f p1 = null;
      Vector3f p2 = null;
      a0 = Math.max(0.0F, a0);

      for (int i = 0; i <= segs; i++) {
         float az = (float) (Math.PI * 2) * (float)i / (float)segs;
         Vector3f n0 = this.at(around(dir, ax[0], ax[1], a0, az, d), inset);
         Vector3f n1 = this.at(around(dir, ax[0], ax[1], a1, az, d), inset);
         Vector3f n2 = this.at(around(dir, ax[0], ax[1], a2, az, d), inset);
         if (p0 != null) {
            m.quad(p0, c0, n0, c0, n1, c1, p1, c1);
            m.quad(p1, c1, n1, c1, n2, c2, p2, c2);
         }

         p0 = n0;
         p1 = n1;
         p2 = n2;
      }
   }

   public void line(LimitlessMesh m, Vector3f a, Vector3f b, float wa, float wb, int ca, int cb, float inset) {
      Vector3f pa = this.at(a, inset);
      Vector3f pb = this.at(b, inset);
      m.streak(pa, pb, wa * this.span(pa), wb * this.span(pb), ca, cb);
   }

   public void path(LimitlessMesh m, Vector3f[] dirs, float[] widths, int[] colors, int n, float inset, Vector3f[] scratchP, float[] scratchW) {
      for (int i = 0; i < n; i++) {
         this.at(dirs[i].x, dirs[i].y, dirs[i].z, inset, scratchP[i]);
         scratchW[i] = widths[i] * this.span(scratchP[i]);
      }

      m.ribbon(scratchP, scratchW, colors, n);
   }

   @FunctionalInterface
   public interface Tint {
      int color(Vector3f var1);
   }
}
