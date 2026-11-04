package com.awakenedmod.awakened.client.render.limitless.red;

import com.awakenedmod.awakened.client.render.limitless.Ease;
import com.awakenedmod.awakened.client.render.limitless.LimitlessMesh;
import com.awakenedmod.awakened.client.render.limitless.LimitlessShapes;
import com.awakenedmod.awakened.client.render.limitless.SpaceBend;
import com.awakenedmod.awakened.core.Lab;
import org.joml.Vector3f;

public final class RedOrb {
   private static final String K = "limitless.red.";
   public static final Lab.Value ORB = Lab.CLIENT.def("limitless.red.orb", 0.24, 0.05, 1.0, 0.01);
   public static final Lab.Value BLOOM = Lab.CLIENT.def("limitless.red.bloom", 1.0, 0.0, 3.0, 0.05);
   public static final Lab.Value WISPS = Lab.CLIENT.def("limitless.red.wisps", 7.0, 0.0, 20.0, 1.0);
   public static final Lab.Value SPARKS = Lab.CLIENT.def("limitless.red.sparks", 9.0, 0.0, 30.0, 1.0);
   public static final Lab.Value SHIMMER = Lab.CLIENT.def("limitless.red.shimmer", 0.08, 0.0, 1.0, 0.01);
   public static final Lab.Value STRETCH = Lab.CLIENT.def("limitless.red.stretch", 2.2, 0.0, 6.0, 0.1);
   public static final int CRIMSON = 13633052;
   public static final int DEEP = 5636104;
   public static final int EMBER = 16726544;
   public static final int GLOW = 16716806;
   public static final int HOT = 16758934;
   public static final int HOT_PAINT = 16772578;
   private static final float TWO_PI = (float) (Math.PI * 2);
   private static final int RIBBON = 12;

   private RedOrb() {
   }

   public static float h(long seed, int salt) {
      return SpaceBend.hash(seed, salt);
   }

   public static int rgb(int a, int b, float t) {
      return LimitlessMesh.mix(LimitlessMesh.rgba(a, 1.0F), LimitlessMesh.rgba(b, 1.0F), t) & 16777215;
   }

   private static Vector3f turn(Vector3f v, Vector3f axis, float ang, Vector3f out) {
      float c = (float)Math.cos((double)ang);
      float s = (float)Math.sin((double)ang);
      float cx = axis.y * v.z - axis.z * v.y;
      float cy = axis.z * v.x - axis.x * v.z;
      float cz = axis.x * v.y - axis.y * v.x;
      return out.set(v.x * c + cx * s, v.y * c + cy * s, v.z * c + cz * s);
   }

   public static Vector3f randomDir(long seed, int salt, Vector3f out) {
      float z = 2.0F * h(seed, salt) - 1.0F;
      float a = (float) (Math.PI * 2) * h(seed, salt + 1);
      float r = (float)Math.sqrt((double)Math.max(0.0F, 1.0F - z * z));
      return out.set(r * (float)Math.cos((double)a), z, r * (float)Math.sin((double)a));
   }

   public static void glow(LimitlessMesh m, Vector3f c, float r, int rgbIn, int rgbOut, float a, float power, float lump, long seed, float age) {
      if (!(a <= 0.004F) && !(r <= 0.001F)) {
         Vector3f[] ax = LimitlessMesh.billboardAxes(c);
         int rings = 4;
         int segs = 20;
         float p1 = (float) (Math.PI * 2) * h(seed, 1);
         float p2 = (float) (Math.PI * 2) * h(seed, 2);
         float p3 = (float) (Math.PI * 2) * h(seed, 3);
         Vector3f[] prev = new Vector3f[segs + 1];
         Vector3f[] cur = new Vector3f[segs + 1];
         float[] cs = new float[segs + 1];
         float[] sn = new float[segs + 1];
         float[] rr = new float[segs + 1];

         for (int j = 0; j <= segs; j++) {
            float th = (float) (Math.PI * 2) * (float)j / (float)segs;
            cs[j] = (float)Math.cos((double)th);
            sn[j] = (float)Math.sin((double)th);
            float n = 0.55F * (float)Math.sin((double)(3.0F * th + p1 + age * 0.15F))
               + 0.3F * (float)Math.sin((double)(5.0F * th + p2 - age * 0.22F))
               + 0.15F * (float)Math.sin((double)(9.0F * th + p3 + age * 0.3F));
            rr[j] = r * (1.0F + lump * n);
            prev[j] = c;
         }

         int prevC = LimitlessMesh.rgba(rgbIn, a);

         for (int k = 1; k <= rings; k++) {
            float t = (float)k / (float)rings;
            int col = LimitlessMesh.rgba(rgb(rgbIn, rgbOut, t), a * (float)Math.pow((double)(1.0F - t), (double)power));

            for (int j = 0; j <= segs; j++) {
               float d = rr[j] * t;
               cur[j] = new Vector3f(c)
                  .add((ax[0].x * cs[j] + ax[1].x * sn[j]) * d, (ax[0].y * cs[j] + ax[1].y * sn[j]) * d, (ax[0].z * cs[j] + ax[1].z * sn[j]) * d);
            }

            for (int j = 0; j < segs; j++) {
               if (k == 1) {
                  m.tri(c, prevC, cur[j], col, cur[j + 1], col);
               } else {
                  m.quad(prev[j], prevC, prev[j + 1], prevC, cur[j + 1], col, cur[j], col);
               }
            }

            Vector3f[] tp = prev;
            prev = cur;
            cur = tp;
            prevC = col;
         }
      }
   }

   public static void shimmer(LimitlessMesh m, Vector3f c, float r, float half, int rgb, float a, long seed, float age) {
      if (!(a <= 0.004F)) {
         Vector3f[] ax = LimitlessMesh.billboardAxes(c);
         int segs = 40;
         float p1 = (float) (Math.PI * 2) * h(seed, 4);
         float p2 = (float) (Math.PI * 2) * h(seed, 5);
         Vector3f pi = null;
         Vector3f pm = null;
         Vector3f po = null;
         int mid = LimitlessMesh.rgba(rgb, a);
         int edge = LimitlessMesh.rgba(rgb, 0.0F);

         for (int j = 0; j <= segs; j++) {
            float th = (float) (Math.PI * 2) * (float)j / (float)segs;
            float rr = r
               * (1.0F + 0.09F * (float)Math.sin((double)(4.0F * th + p1 + age * 0.4F)) + 0.05F * (float)Math.sin((double)(7.0F * th + p2 - age * 0.6F)));
            float cx = (float)Math.cos((double)th);
            float sx = (float)Math.sin((double)th);
            Vector3f u = new Vector3f(ax[0]).mul(cx).add(ax[1].x * sx, ax[1].y * sx, ax[1].z * sx);
            Vector3f ni = new Vector3f(u).mul(rr - half).add(c);
            Vector3f nm = new Vector3f(u).mul(rr).add(c);
            Vector3f no = new Vector3f(u).mul(rr + half).add(c);
            if (pi != null) {
               m.quad(pi, edge, ni, edge, nm, mid, pm, mid);
               m.quad(pm, mid, nm, mid, no, edge, po, edge);
            }

            pi = ni;
            pm = nm;
            po = no;
         }
      }
   }

   public static void orb(LimitlessMesh m, boolean body, Vector3f c, Vector3f fwd, float age, float grow, float pre, float fly, float vis, long seed) {
      if (!(vis <= 0.01F)) {
         float r = ORB.getF() * (0.3F + 0.7F * grow) * (1.0F - 0.15F * pre) * (1.0F + 0.2F * fly);
         float heat = Math.max(pre, fly);
         float calm = vis * (1.0F - fly);
         if (body) {
            glow(m, c, r * (2.4F + 0.6F * heat), 13633052, 5636104, 0.55F * vis, 1.6F, 0.15F, seed * 3L + 1L, age);
            wisps(m, true, c, r, age, pre, calm, seed);
            orbBody(m, c, r, fwd, STRETCH.getF() * fly, age, vis);
            m.disc(c, r * (0.5F + 0.15F * heat), 16, LimitlessMesh.rgba(16772578, 0.95F * vis), LimitlessMesh.rgba(16726544, 0.0F));
            sparks(m, true, c, r, age, calm, seed);
         } else {
            float bloom = BLOOM.getF() * vis;
            glow(m, c, r * (7.0F + 2.0F * heat), 16716806, 16716806, 0.28F * bloom, 2.2F, 0.0F, seed, age);
            glow(m, c, r * 3.0F, 16716806, 16726544, 0.5F * bloom, 1.4F, 0.0F, seed, age);
            m.disc(c, r * (0.85F + 0.3F * heat), 16, LimitlessMesh.rgba(16758934, (0.35F + 0.3F * heat) * vis), LimitlessMesh.rgba(16716806, 0.0F));
            wisps(m, false, c, r, age, pre, calm, seed);
            sparks(m, false, c, r, age, calm, seed);
            float sh = SHIMMER.getF() * calm * grow;

            for (int k = 0; k < 2; k++) {
               float u = Ease.frac(age / 9.0F + (float)k * 0.5F);
               shimmer(m, c, r * (2.0F + 4.0F * u) * (1.0F - 0.3F * pre), r * 0.25F, 16716806, sh * (float)Math.sin(Math.PI * (double)u), seed + (long)k, age);
            }
         }
      }
   }

   private static void orbBody(LimitlessMesh m, Vector3f c, float r, Vector3f fwd, float stretch, float age, float vis) {
      Vector3f[] ax = LimitlessMesh.planeAxes(fwd);
      LimitlessShapes.blob(m, c, fwd, (float) Math.PI, 10, 18, true, n -> {
         float along = n.dot(fwd);
         float wob = 1.0F + 0.1F * (float)Math.sin((double)(n.x * 7.0F + age * 1.3F)) * (float)Math.sin((double)(n.y * 6.0F - age * 1.1F));
         float s = along < 0.0F ? stretch * along * along : 0.15F * stretch * along * along;
         return r * wob * (1.0F + s);
      }, (n, facing) -> {
         float az = (float)Math.atan2((double)n.dot(ax[1]), (double)n.dot(ax[0]));
         float polar = (float)Math.acos((double)Math.max(-1.0F, Math.min(1.0F, n.dot(fwd))));
         float churn = 0.5F + 0.5F * (float)Math.sin((double)(az * 3.0F + polar * 5.0F - age * 1.1F));
         float f = Math.max(0.0F, facing);
         int col = LimitlessMesh.mix(LimitlessMesh.rgba(5636104, 1.0F), LimitlessMesh.rgba(13633052, 1.0F), 0.15F + 0.85F * (float)Math.pow((double)f, 0.6));
         col = LimitlessMesh.mix(col, LimitlessMesh.rgba(16726544, 1.0F), churn * f * 0.55F);
         return LimitlessMesh.fade(col, 0.97F * vis);
      });
   }

   private static void wisps(LimitlessMesh m, boolean body, Vector3f c, float r, float age, float tight, float vis, long seed) {
      if (!(vis <= 0.01F)) {
         Vector3f[] pts = new Vector3f[12];
         float[] w = new float[12];
         int[] col = new int[12];
         Vector3f d0 = new Vector3f();
         Vector3f axis = new Vector3f();
         Vector3f tmp = new Vector3f();
         int n = WISPS.getI();

         for (int k = 0; k < n; k++) {
            long sk = seed * 131L + (long)k;
            float period = 6.0F + 4.0F * h(sk, 1);
            float clock = age * (1.0F + 0.6F * tight) + h(sk, 2) * period;
            long cyc = (long)Math.floor((double)(clock / period));
            float local = clock - (float)cyc * period;
            float u = local / period;
            float env = Ease.envelope(local, 1.5F, period, 2.5F) * vis;
            if (!(env <= 0.01F)) {
               long sc = sk * 7919L + cyc;
               randomDir(sc, 3, d0);
               randomDir(sc, 6, tmp);
               axis.set(d0).cross(tmp);
               if (axis.lengthSquared() < 1.0E-4F) {
                  axis.set(0.0F, 1.0F, 0.0F).cross(d0);
               }

               axis.normalize();
               float sweep = (1.4F + 1.2F * h(sc, 8)) * (h(sc, 9) > 0.5F ? 1.0F : -1.0F);
               float reach = r * (1.2F + 1.6F * h(sc, 10)) * (1.0F - 0.45F * tight);

               for (int i = 0; i < 12; i++) {
                  float s = (float)i / 11.0F;
                  float ang = sweep * (s * 0.8F + u * 0.9F);
                  float rho = r * 1.02F + reach * s * s * (0.4F + 0.6F * u);
                  pts[i] = turn(d0, axis, ang, new Vector3f()).mul(rho).add(c);
                  float taper = (1.0F - s) * (0.7F + 0.3F * (1.0F - u));
                  if (body) {
                     w[i] = r * 0.3F * taper;
                     col[i] = LimitlessMesh.rgba(rgb(13633052, 5636104, s), env * 0.95F * (1.0F - 0.4F * s));
                  } else {
                     w[i] = r * 0.09F * taper;
                     col[i] = LimitlessMesh.rgba(16726544, env * 0.85F * (1.0F - s));
                  }
               }

               m.ribbon(pts, w, col, 12);
            }
         }
      }
   }

   private static void sparks(LimitlessMesh m, boolean body, Vector3f c, float r, float age, float vis, long seed) {
      if (!(vis <= 0.01F)) {
         Vector3f d = new Vector3f();
         int n = SPARKS.getI();

         for (int k = 0; k < n; k++) {
            long sk = seed * 977L + (long)k;
            float period = 5.0F + 4.0F * h(sk, 1);
            float clock = age + h(sk, 2) * period;
            long cyc = (long)Math.floor((double)(clock / period));
            float local = clock - (float)cyc * period;
            float u = local / period;
            float env = Ease.envelope(local, 1.0F, period, 2.0F) * vis;
            if (!(env <= 0.01F)) {
               randomDir(sk * 31L + cyc, 3, d);
               float d0 = r * (1.1F + 3.5F * Ease.out(u, 1.6F));
               float d1 = d0 + r * (0.8F + 0.8F * h(sk, 5)) * (1.0F - 0.5F * u);
               Vector3f a = new Vector3f(d).mul(d0).add(c);
               Vector3f b = new Vector3f(d).mul(d1).add(c);
               if (body) {
                  m.streak(a, b, 0.004F, 0.028F, LimitlessMesh.rgba(5636104, 0.0F), LimitlessMesh.rgba(13633052, env * 0.9F));
               } else {
                  m.streak(a, b, 0.002F, 0.012F, LimitlessMesh.rgba(16726544, 0.0F), LimitlessMesh.rgba(16758934, env * 0.8F));
               }
            }
         }
      }
   }
}
