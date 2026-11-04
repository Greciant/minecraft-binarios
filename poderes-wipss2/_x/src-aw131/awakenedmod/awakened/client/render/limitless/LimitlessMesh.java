package com.awakenedmod.awakened.client.render.limitless;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class LimitlessMesh {
   private static final float TWO_PI = (float) (Math.PI * 2);
   private final Pose pose;
   private final VertexConsumer vc;
   public static final float RIBBON_NEAR = 1.6F;

   LimitlessMesh(Pose pose, VertexConsumer vc) {
      this.pose = pose;
      this.vc = vc;
   }

   public static int argb(int a, int r, int g, int b) {
      return clamp(a) << 24 | clamp(r) << 16 | clamp(g) << 8 | clamp(b);
   }

   public static int rgba(int rgb, float a) {
      return clamp(Math.round(a * 255.0F)) << 24 | rgb & 16777215;
   }

   public static int fade(int argb, float f) {
      return clamp(Math.round((float)(argb >>> 24) * f)) << 24 | argb & 16777215;
   }

   public static int mix(int a, int b, float t) {
      t = Math.max(0.0F, Math.min(1.0F, t));
      int aa = a >>> 24;
      int ar = a >> 16 & 0xFF;
      int ag = a >> 8 & 0xFF;
      int ab = a & 0xFF;
      int ba = b >>> 24;
      int br = b >> 16 & 0xFF;
      int bg = b >> 8 & 0xFF;
      int bb = b & 0xFF;
      return argb(
         Math.round((float)aa + (float)(ba - aa) * t),
         Math.round((float)ar + (float)(br - ar) * t),
         Math.round((float)ag + (float)(bg - ag) * t),
         Math.round((float)ab + (float)(bb - ab) * t)
      );
   }

   private static int clamp(int v) {
      return v < 0 ? 0 : Math.min(255, v);
   }

   public void tri(Vector3f a, int ca, Vector3f b, int cb, Vector3f c, int cc) {
      if (ca >>> 24 != 0 || cb >>> 24 != 0 || cc >>> 24 != 0) {
         float ux = b.x - a.x;
         float uy = b.y - a.y;
         float uz = b.z - a.z;
         float vx = c.x - a.x;
         float vy = c.y - a.y;
         float vz = c.z - a.z;
         float nx = uy * vz - uz * vy;
         float ny = uz * vx - ux * vz;
         float nz = ux * vy - uy * vx;
         if (-(nx * a.x + ny * a.y + nz * a.z) >= 0.0F) {
            this.vertex(a, ca);
            this.vertex(b, cb);
            this.vertex(c, cc);
         } else {
            this.vertex(a, ca);
            this.vertex(c, cc);
            this.vertex(b, cb);
         }
      }
   }

   public void quad(Vector3f a, int ca, Vector3f b, int cb, Vector3f c, int cc, Vector3f d, int cd) {
      this.tri(a, ca, b, cb, c, cc);
      this.tri(a, ca, c, cc, d, cd);
   }

   private void vertex(Vector3f p, int argb) {
      this.vc.addVertex(this.pose, p.x, p.y, p.z).setColor(argb);
   }

   public static Vector3f[] billboardAxes(Vector3f center) {
      Vector3f n = new Vector3f(center).negate();
      if (n.lengthSquared() < 1.0E-8F) {
         n.set(0.0F, 0.0F, 1.0F);
      }

      n.normalize();
      return planeAxes(n);
   }

   public static Vector3f[] planeAxes(Vector3f n) {
      Vector3f ref = Math.abs(n.y) < 0.95F ? new Vector3f(0.0F, 1.0F, 0.0F) : new Vector3f(1.0F, 0.0F, 0.0F);
      Vector3f u = new Vector3f(ref).cross(n).normalize();
      Vector3f v = new Vector3f(n).cross(u).normalize();
      return new Vector3f[]{u, v};
   }

   public void disc(Vector3f center, float radius, int segs, int inner, int outer) {
      Vector3f[] ax = billboardAxes(center);
      Vector3f prev = null;

      for (int i = 0; i <= segs; i++) {
         float a = (float) (Math.PI * 2) * (float)i / (float)segs;
         Vector3f p = onCircle(center, ax[0], ax[1], radius, a);
         if (prev != null) {
            this.tri(center, inner, prev, outer, p, outer);
         }

         prev = p;
      }
   }

   public void ring(Vector3f center, float r, float half, int segs, int mid, int edge) {
      Vector3f[] ax = billboardAxes(center);
      this.ringIn(center, ax[0], ax[1], r, half, segs, mid, edge, 0.0F);
   }

   public void ringFlat(Vector3f center, Vector3f normal, float r, float half, int segs, int mid, int edge, float spin) {
      Vector3f[] ax = planeAxes(normal);
      this.ringIn(center, ax[0], ax[1], r, half, segs, mid, edge, spin);
   }

   private void ringIn(Vector3f c, Vector3f u, Vector3f v, float r, float half, int segs, int mid, int edge, float spin) {
      float rIn = Math.max(0.0F, r - half);
      float rOut = r + half;
      Vector3f pi = null;
      Vector3f pm = null;
      Vector3f po = null;

      for (int i = 0; i <= segs; i++) {
         float a = spin + (float) (Math.PI * 2) * (float)i / (float)segs;
         Vector3f ni = onCircle(c, u, v, rIn, a);
         Vector3f nm = onCircle(c, u, v, r, a);
         Vector3f no = onCircle(c, u, v, rOut, a);
         if (pi != null) {
            this.quad(pi, edge, ni, edge, nm, mid, pm, mid);
            this.quad(pm, mid, nm, mid, no, edge, po, edge);
         }

         pi = ni;
         pm = nm;
         po = no;
      }
   }

   private static Vector3f onCircle(Vector3f c, Vector3f u, Vector3f v, float r, float a) {
      float cs = (float)Math.cos((double)a) * r;
      float sn = (float)Math.sin((double)a) * r;
      return new Vector3f(c.x + u.x * cs + v.x * sn, c.y + u.y * cs + v.y * sn, c.z + u.z * cs + v.z * sn);
   }

   public void sphere(Vector3f center, float radius, Vector3f pole, float maxPolar, int rings, int segs, boolean frontOnly, LimitlessMesh.Shade shade) {
      Vector3f[] ax = planeAxes(new Vector3f(pole).normalize());
      Vector3f n0 = new Vector3f(pole).normalize();
      Vector3f[] prevP = new Vector3f[segs + 1];
      int[] prevC = new int[segs + 1];
      Vector3f[] curP = new Vector3f[segs + 1];
      int[] curC = new int[segs + 1];

      for (int i = 0; i <= rings; i++) {
         float polar = maxPolar * (float)i / (float)rings;
         float sp = (float)Math.sin((double)polar);
         float cp = (float)Math.cos((double)polar);

         for (int j = 0; j <= segs; j++) {
            float az = (float) (Math.PI * 2) * (float)j / (float)segs;
            float ca = (float)Math.cos((double)az) * sp;
            float sa = (float)Math.sin((double)az) * sp;
            Vector3f n = new Vector3f(n0.x * cp + ax[0].x * ca + ax[1].x * sa, n0.y * cp + ax[0].y * ca + ax[1].y * sa, n0.z * cp + ax[0].z * ca + ax[1].z * sa);
            Vector3f p = new Vector3f(n).mul(radius).add(center);
            float facing = -(n.x * p.x + n.y * p.y + n.z * p.z) / Math.max(1.0E-4F, p.length());
            curP[j] = p;
            curC[j] = frontOnly && facing < 0.0F ? 0 : shade.color(n, facing);
         }

         if (i > 0) {
            for (int j = 0; j < segs; j++) {
               this.quad(prevP[j], prevC[j], prevP[j + 1], prevC[j + 1], curP[j + 1], curC[j + 1], curP[j], curC[j]);
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

   public void sphere(Vector3f center, float radius, int rings, int segs, LimitlessMesh.Shade shade) {
      this.sphere(center, radius, new Vector3f(0.0F, 1.0F, 0.0F), (float) Math.PI, rings, segs, true, shade);
   }

   public void ribbon(Vector3f[] pts, float[] width, int[] color, int n) {
      if (n >= 2) {
         Vector3f prevL = null;
         Vector3f prevR = null;
         Vector3f t = new Vector3f();
         Vector3f side = new Vector3f();
         int prevC = 0;

         for (int i = 0; i < n; i++) {
            float near = Math.min(1.0F, Math.max(0.0F, (pts[i].length() - 0.8F) / 0.8F));
            int ci = near >= 1.0F ? color[i] : fade(color[i], near);
            Vector3f a = pts[Math.max(0, i - 1)];
            Vector3f b = pts[Math.min(n - 1, i + 1)];
            t.set(b).sub(a);
            side.set(t).cross(pts[i]);
            if (side.lengthSquared() < 1.0E-10F) {
               side.set(1.0F, 0.0F, 0.0F);
            }

            side.normalize().mul(width[i]);
            Vector3f l = new Vector3f(pts[i]).add(side);
            Vector3f r = new Vector3f(pts[i]).sub(side);
            if (prevL != null) {
               this.quad(prevL, prevC, l, ci, r, ci, prevR, prevC);
            }

            prevL = l;
            prevR = r;
            prevC = ci;
         }
      }
   }

   public void streak(Vector3f a, Vector3f b, float wa, float wb, int ca, int cb) {
      this.ribbon(new Vector3f[]{a, b}, new float[]{wa, wb}, new int[]{ca, cb}, 2);
   }

   public void shard(Vector3f center, float size, Quaternionf rot, int color, int shadow) {
      float s = size * 0.5F;
      Vector3f a = new Vector3f(s, s, s).rotate(rot).add(center);
      Vector3f b = new Vector3f(-s, -s, s).rotate(rot).add(center);
      Vector3f c = new Vector3f(-s, s, -s).rotate(rot).add(center);
      Vector3f d = new Vector3f(s * 1.4F, -s, -s).rotate(rot).add(center);
      this.tri(a, color, b, color, c, color);
      this.tri(a, color, c, shadow, d, shadow);
      this.tri(a, color, d, shadow, b, color);
      this.tri(b, shadow, d, shadow, c, shadow);
   }

   @FunctionalInterface
   public interface Shade {
      int color(Vector3f var1, float var2);
   }
}
