package com.awakenedmod.awakened.client.render.limitless;

import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class LimitlessShapes {
   private static final float TWO_PI = (float) (Math.PI * 2);
   private static final int[][] CHUNK_FACES = new int[][]{{0, 2, 6, 4}, {1, 3, 7, 5}, {0, 1, 5, 4}, {2, 3, 7, 6}, {0, 1, 3, 2}, {4, 5, 7, 6}};
   private static final float[][] CHUNK_NORMALS = new float[][]{
      {-1.0F, 0.0F, 0.0F}, {1.0F, 0.0F, 0.0F}, {0.0F, -1.0F, 0.0F}, {0.0F, 1.0F, 0.0F}, {0.0F, 0.0F, -1.0F}, {0.0F, 0.0F, 1.0F}
   };

   private LimitlessShapes() {
   }

   public static void blob(
      LimitlessMesh m,
      Vector3f center,
      Vector3f pole,
      float maxPolar,
      int rings,
      int segs,
      boolean frontOnly,
      LimitlessShapes.Radius radius,
      LimitlessMesh.Shade shade
   ) {
      Vector3f n0 = new Vector3f(pole).normalize();
      Vector3f[] ax = LimitlessMesh.planeAxes(n0);
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
            Vector3f p = new Vector3f(n).mul(radius.at(n)).add(center);
            float facing = -(n.x * p.x + n.y * p.y + n.z * p.z) / Math.max(1.0E-4F, p.length());
            curP[j] = p;
            curC[j] = frontOnly && facing < 0.0F ? 0 : shade.color(n, facing);
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

   public static void spindle(
      LimitlessMesh m, Vector3f tail, Vector3f head, float width, float waist, float roll, int tailCol, int body, int bodyDark, int headCol
   ) {
      Vector3f axis = new Vector3f(head).sub(tail);
      float len = axis.length();
      if (!(len < 1.0E-4F)) {
         axis.div(len);
         Vector3f[] ax = LimitlessMesh.planeAxes(axis);
         Vector3f mid = new Vector3f(axis).mul(len * waist).add(tail);
         Vector3f[] w = new Vector3f[4];

         for (int k = 0; k < 4; k++) {
            float a = roll + (float)k * (float) Math.PI / 2.0F;
            w[k] = new Vector3f(ax[0])
               .mul((float)Math.cos((double)a) * width * 0.5F)
               .add(new Vector3f(ax[1]).mul((float)Math.sin((double)a) * width * 0.5F))
               .add(mid);
         }

         for (int k = 0; k < 4; k++) {
            Vector3f p = w[k];
            Vector3f q = w[k + 1 & 3];
            int col = (k & 1) == 0 ? body : bodyDark;
            face(m, mid, tail, tailCol, p, q, col);
            face(m, mid, head, headCol, p, q, col);
         }
      }
   }

   private static void face(LimitlessMesh m, Vector3f mid, Vector3f apex, int apexCol, Vector3f p, Vector3f q, int col) {
      float cx = (apex.x + p.x + q.x) / 3.0F;
      float cy = (apex.y + p.y + q.y) / 3.0F;
      float cz = (apex.z + p.z + q.z) / 3.0F;
      float ox = cx - mid.x;
      float oy = cy - mid.y;
      float oz = cz - mid.z;
      float ux = p.x - apex.x;
      float uy = p.y - apex.y;
      float uz = p.z - apex.z;
      float vx = q.x - apex.x;
      float vy = q.y - apex.y;
      float vz = q.z - apex.z;
      float nx = uy * vz - uz * vy;
      float ny = uz * vx - ux * vz;
      float nz = ux * vy - uy * vx;
      if (nx * ox + ny * oy + nz * oz < 0.0F) {
         nx = -nx;
         ny = -ny;
         nz = -nz;
      }

      if (-(nx * cx + ny * cy + nz * cz) > 0.0F) {
         m.tri(apex, apexCol, p, col, q, col);
      }
   }

   public static void bolt(Vector3f a, Vector3f b, float amp, float age, long seed, Vector3f[] out, int n) {
      Vector3f d = new Vector3f(b).sub(a);
      Vector3f dir = d.lengthSquared() < 1.0E-8F ? new Vector3f(0.0F, 1.0F, 0.0F) : new Vector3f(d).normalize();
      Vector3f[] ax = LimitlessMesh.planeAxes(dir);

      for (int i = 0; i < n; i++) {
         float t = (float)i / (float)(n - 1);
         out[i].set(d).mul(t).add(a);
         if (i != 0 && i != n - 1) {
            float env = (float)Math.sin(Math.PI * (double)t) * (0.55F + 0.45F * SpaceBend.hash(seed, i * 5));
            float w1 = 0.5F + 0.9F * SpaceBend.hash(seed, i * 5 + 1);
            float w2 = 0.5F + 0.9F * SpaceBend.hash(seed, i * 5 + 2);
            float x = amp * env * (float)Math.sin((double)(age * w1 + (float) (Math.PI * 2) * SpaceBend.hash(seed, i * 5 + 3)));
            float y = amp * env * (float)Math.sin((double)(age * w2 + (float) (Math.PI * 2) * SpaceBend.hash(seed, i * 5 + 4)));
            out[i].add(ax[0].x * x + ax[1].x * y, ax[0].y * x + ax[1].y * y, ax[0].z * x + ax[1].z * y);
         }
      }
   }

   public static Vector3f coneDir(Vector3f fwd, Vector3f[] ax, float ang, float off, Vector3f out) {
      float so = (float)Math.sin((double)off);
      float co = (float)Math.cos((double)off);
      float ca = (float)Math.cos((double)ang) * so;
      float sa = (float)Math.sin((double)ang) * so;
      return out.set(fwd.x * co + ax[0].x * ca + ax[1].x * sa, fwd.y * co + ax[0].y * ca + ax[1].y * sa, fwd.z * co + ax[0].z * ca + ax[1].z * sa).normalize();
   }

   public static float patches(Vector3f n, float scale, float phase) {
      float x = n.x * scale;
      float y = n.y * scale;
      float z = n.z * scale;
      float v = (float)(
         Math.sin((double)(x * 1.7F + phase)) * Math.sin((double)(y * 2.3F - phase * 0.7F))
            + Math.sin((double)(z * 1.9F + x * 0.8F + phase * 0.5F)) * Math.sin((double)(y * 1.1F - z * 1.3F))
            + 0.5 * Math.sin((double)(x * 3.1F - y * 2.9F + z * 2.3F))
      );
      return Math.max(0.0F, Math.min(1.0F, 0.5F + v * 0.3F));
   }

   public static float dissolve(float patch, float left) {
      float threshold = 1.2F * (1.0F - left) - 0.1F;
      return Ease.smooth((patch - threshold) / 0.1F);
   }

   public static Vector3f fibonacci(int i, int n, Vector3f out) {
      float y = 1.0F - 2.0F * ((float)i + 0.5F) / (float)n;
      float r = (float)Math.sqrt((double)Math.max(0.0F, 1.0F - y * y));
      float a = (float)i * 2.3999631F;
      return out.set((float)Math.cos((double)a) * r, y, (float)Math.sin((double)a) * r);
   }

   public static void chunk(LimitlessMesh m, Vector3f center, float size, Quaternionf rot, long seed, int color) {
      float h = size * 0.5F;
      Vector3f[] c = new Vector3f[8];

      for (int i = 0; i < 8; i++) {
         float jx = 0.7F + 0.55F * SpaceBend.hash(seed, i * 3);
         float jy = 0.7F + 0.55F * SpaceBend.hash(seed, i * 3 + 1);
         float jz = 0.7F + 0.55F * SpaceBend.hash(seed, i * 3 + 2);
         c[i] = new Vector3f(((i & 1) == 0 ? -h : h) * jx, ((i >> 1 & 1) == 0 ? -h * 0.8F : h * 0.8F) * jy, ((i >> 2 & 1) == 0 ? -h : h) * jz)
            .rotate(rot)
            .add(center);
      }

      Vector3f n = new Vector3f();
      Vector3f mid = new Vector3f();

      for (int f = 0; f < 6; f++) {
         int[] q = CHUNK_FACES[f];
         mid.set(c[q[0]]).add(c[q[1]]).add(c[q[2]]).add(c[q[3]]).mul(0.25F);
         n.set(CHUNK_NORMALS[f][0], CHUNK_NORMALS[f][1], CHUNK_NORMALS[f][2]).rotate(rot);
         if (!(n.dot(mid) >= 0.0F)) {
            float shade = 0.5F + 0.32F * (n.y + 1.0F) * 0.5F + 0.18F * Math.max(0.0F, n.y);
            int col = LimitlessMesh.argb(
               color >>> 24,
               Math.round((float)(color >> 16 & 0xFF) * shade),
               Math.round((float)(color >> 8 & 0xFF) * shade),
               Math.round((float)(color & 0xFF) * shade)
            );
            m.quad(c[q[0]], col, c[q[1]], col, c[q[2]], col, c[q[3]], col);
         }
      }
   }

   @FunctionalInterface
   public interface Radius {
      float at(Vector3f var1);
   }
}
