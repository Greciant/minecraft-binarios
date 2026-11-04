package com.devilfruits.fruits.suna_suna.client.vfx;

import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.render.MeshTex;
import com.devilfruits.client.vfx.render.MeshWriter;
import com.devilfruits.client.vfx.sprite.VfxSprites;
import com.mojang.blaze3d.vertex.BufferBuilder;
import java.util.Arrays;
import javax.annotation.Nullable;
import net.minecraft.util.Mth;

public final class SandDraw {
   private static final int MAX = 256;
   private static final float[] XS = new float[256];
   private static final float[] YS = new float[256];
   private static final float[] ZS = new float[256];
   private static final float[] HEAT = new float[256];
   private static final float LX = -0.32F;
   private static final float LY = 0.86F;
   private static final float LZ = 0.39F;
   public static final int PLAIN = 3;

   public static float shade(float nx, float ny, float nz) {
      float d = nx * -0.32F + ny * 0.86F + nz * 0.39F;
      return 0.66F + 0.3F * Math.max(0.0F, d) + 0.04F * ny;
   }

   public static void stream(VfxFrame f, int n, double[] px, double[] py, double[] pz, float[] halfWidth, float[] alpha, float u0, float du, int rgb) {
      stream(f, MeshTex.SAND_STREAM, n, px, py, pz, halfWidth, alpha, u0, du, rgb);
   }

   public static void stream(
      VfxFrame f, MeshTex tex, int n, double[] px, double[] py, double[] pz, float[] halfWidth, float[] alpha, float u0, float du, int rgb
   ) {
      n = Math.min(n, 256);
      if (n >= 2 && f.allowMesh()) {
         for (int i = 0; i < n; i++) {
            XS[i] = f.rx(px[i]);
            YS[i] = f.ry(py[i]);
            ZS[i] = f.rz(pz[i]);
         }

         MeshWriter.strip(f.mesh(tex), n, XS, YS, ZS, halfWidth, HEAT, alpha, u0, du, rgb, 0, 0.0F);
      }
   }

   public static void band(
      VfxFrame f,
      MeshTex tex,
      int n,
      double[] px,
      double[] py,
      double[] pz,
      float[] ax,
      float[] ay,
      float[] az,
      float[] halfWidth,
      float[] alpha,
      float u0,
      float du,
      int rgb,
      boolean shaded
   ) {
      band(f, tex, n, px, py, pz, ax, ay, az, halfWidth, alpha, null, u0, du, rgb, shaded);
   }

   public static void band(
      VfxFrame f,
      MeshTex tex,
      int n,
      double[] px,
      double[] py,
      double[] pz,
      float[] ax,
      float[] ay,
      float[] az,
      float[] halfWidth,
      float[] alpha,
      @Nullable float[] bright,
      float u0,
      float du,
      int rgb,
      boolean shaded
   ) {
      if (n >= 2 && f.allowMesh()) {
         BufferBuilder b = f.mesh(tex);

         for (int i = 0; i + 1 < n; i++) {
            int c = bright == null ? rgb : SandCinematic.scale(rgb, (bright[i] + bright[i + 1]) * 0.5F);
            band(
               b,
               f,
               px[i],
               py[i],
               pz[i],
               ax[i],
               ay[i],
               az[i],
               halfWidth[i],
               alpha[i],
               px[i + 1],
               py[i + 1],
               pz[i + 1],
               ax[i + 1],
               ay[i + 1],
               az[i + 1],
               halfWidth[i + 1],
               alpha[i + 1],
               u0 + (float)i * du,
               u0 + (float)(i + 1) * du,
               c,
               shaded
            );
         }
      }
   }

   private static void band(
      BufferBuilder b,
      VfxFrame f,
      double x0,
      double y0,
      double z0,
      float ax0,
      float ay0,
      float az0,
      float w0,
      float a0,
      double x1,
      double y1,
      double z1,
      float ax1,
      float ay1,
      float az1,
      float w1,
      float a1,
      float ua,
      float ub,
      int rgb,
      boolean shaded
   ) {
      float rx0 = f.rx(x0);
      float ry0 = f.ry(y0);
      float rz0 = f.rz(z0);
      float rx1 = f.rx(x1);
      float ry1 = f.ry(y1);
      float rz1 = f.rz(z1);
      float tx = rx1 - rx0;
      float ty = ry1 - ry0;
      float tz = rz1 - rz0;
      float nx = ty * az0 - tz * ay0;
      float ny = tz * ax0 - tx * az0;
      float nz = tx * ay0 - ty * ax0;
      float nl = Mth.sqrt(nx * nx + ny * ny + nz * nz);
      if (nl < 1.0E-6F) {
         nx = 0.0F;
         ny = 1.0F;
         nz = 0.0F;
      } else {
         nx /= nl;
         ny /= nl;
         nz /= nl;
      }

      if (nx * -rx0 + ny * -ry0 + nz * -rz0 < 0.0F) {
         nx = -nx;
         ny = -ny;
         nz = -nz;
      }

      int c = shaded ? SandCinematic.scale(rgb, shade(nx, ny, nz)) : rgb;
      int c0 = MeshWriter.argb(c, a0);
      int c1 = MeshWriter.argb(c, a1);
      MeshWriter.vertex(b, rx0 - ax0 * w0, ry0 - ay0 * w0, rz0 - az0 * w0, c0, ua, 1.0F, -1.0F, 0, 0.0F, 0.0F, 0.0F, nx, ny, nz);
      MeshWriter.vertex(b, rx0 + ax0 * w0, ry0 + ay0 * w0, rz0 + az0 * w0, c0, ua, 0.0F, -1.0F, 0, 0.0F, 0.0F, 0.0F, nx, ny, nz);
      MeshWriter.vertex(b, rx1 + ax1 * w1, ry1 + ay1 * w1, rz1 + az1 * w1, c1, ub, 0.0F, -1.0F, 0, 0.0F, 0.0F, 0.0F, nx, ny, nz);
      MeshWriter.vertex(b, rx1 - ax1 * w1, ry1 - ay1 * w1, rz1 - az1 * w1, c1, ub, 1.0F, -1.0F, 0, 0.0F, 0.0F, 0.0F, nx, ny, nz);
   }

   public static void quad(
      BufferBuilder b,
      VfxFrame f,
      double[] a,
      double[] bb,
      double[] c,
      double[] d,
      float u0,
      float v0,
      float u1,
      float v1,
      int rgb,
      float alphaAB,
      float alphaCD
   ) {
      float ax = f.rx(a[0]);
      float ay = f.ry(a[1]);
      float az = f.rz(a[2]);
      float bx = f.rx(bb[0]);
      float by = f.ry(bb[1]);
      float bz = f.rz(bb[2]);
      float cx = f.rx(c[0]);
      float cy = f.ry(c[1]);
      float cz = f.rz(c[2]);
      float dx = f.rx(d[0]);
      float dy = f.ry(d[1]);
      float dz = f.rz(d[2]);
      float ux = cx - ax;
      float uy = cy - ay;
      float uz = cz - az;
      float vx = dx - bx;
      float vy = dy - by;
      float vz = dz - bz;
      float nx = uy * vz - uz * vy;
      float ny = uz * vx - ux * vz;
      float nz = ux * vy - uy * vx;
      float nl = Mth.sqrt(nx * nx + ny * ny + nz * nz);
      if (!(nl < 1.0E-6F)) {
         nx /= nl;
         ny /= nl;
         nz /= nl;
         if (nx * -ax + ny * -ay + nz * -az < 0.0F) {
            nx = -nx;
            ny = -ny;
            nz = -nz;
         }

         int col = SandCinematic.scale(rgb, shade(nx, ny, nz));
         int cAB = MeshWriter.argb(col, alphaAB);
         int cCD = MeshWriter.argb(col, alphaCD);
         MeshWriter.vertex(b, ax, ay, az, cAB, u0, v0, -1.0F, 0, 0.0F, 0.0F, 0.0F, nx, ny, nz);
         MeshWriter.vertex(b, bx, by, bz, cAB, u1, v0, -1.0F, 0, 0.0F, 0.0F, 0.0F, nx, ny, nz);
         MeshWriter.vertex(b, cx, cy, cz, cCD, u1, v1, -1.0F, 0, 0.0F, 0.0F, 0.0F, nx, ny, nz);
         MeshWriter.vertex(b, dx, dy, dz, cCD, u0, v1, -1.0F, 0, 0.0F, 0.0F, 0.0F, nx, ny, nz);
      }
   }

   public static void quadD(
      BufferBuilder b,
      VfxFrame f,
      double[] a,
      double[] bb,
      double[] c,
      double[] d,
      float u0,
      float v0,
      float u1,
      float v1,
      int rgb,
      float alphaAB,
      float alphaCD,
      float da,
      float db,
      float dc,
      float dd
   ) {
      float ax = f.rx(a[0]);
      float ay = f.ry(a[1]);
      float az = f.rz(a[2]);
      float bx = f.rx(bb[0]);
      float by = f.ry(bb[1]);
      float bz = f.rz(bb[2]);
      float cx = f.rx(c[0]);
      float cy = f.ry(c[1]);
      float cz = f.rz(c[2]);
      float dx = f.rx(d[0]);
      float dy = f.ry(d[1]);
      float dz = f.rz(d[2]);
      float ux = cx - ax;
      float uy = cy - ay;
      float uz = cz - az;
      float vx = dx - bx;
      float vy = dy - by;
      float vz = dz - bz;
      float nx = uy * vz - uz * vy;
      float ny = uz * vx - ux * vz;
      float nz = ux * vy - uy * vx;
      float nl = Mth.sqrt(nx * nx + ny * ny + nz * nz);
      if (!(nl < 1.0E-6F)) {
         nx /= nl;
         ny /= nl;
         nz /= nl;
         if (nx * -ax + ny * -ay + nz * -az < 0.0F) {
            nx = -nx;
            ny = -ny;
            nz = -nz;
         }

         int col = SandCinematic.scale(rgb, shade(nx, ny, nz));
         int cAB = MeshWriter.argb(col, alphaAB);
         int cCD = MeshWriter.argb(col, alphaCD);
         MeshWriter.vertex(b, ax, ay, az, cAB, u0, v0, -1.0F, 3, 0.0F, da, 0.0F, nx, ny, nz);
         MeshWriter.vertex(b, bx, by, bz, cAB, u1, v0, -1.0F, 3, 0.0F, db, 0.0F, nx, ny, nz);
         MeshWriter.vertex(b, cx, cy, cz, cCD, u1, v1, -1.0F, 3, 0.0F, dc, 0.0F, nx, ny, nz);
         MeshWriter.vertex(b, dx, dy, dz, cCD, u0, v1, -1.0F, 3, 0.0F, dd, 0.0F, nx, ny, nz);
      }
   }

   public static void billboard(
      VfxFrame f, VfxSprites.Sprite sprite, float frame, double x, double y, double z, float size, float rotation, int rgb, float alpha
   ) {
      if (!(alpha <= 0.01F) && f.allowMesh()) {
         BufferBuilder b = f.mesh(MeshTex.ATLAS);
         float cx = f.rx(x);
         float cy = f.ry(y);
         float cz = f.rz(z);
         float h = size * 0.5F;
         float c = Mth.cos(rotation) * h;
         float s = Mth.sin(rotation) * h;
         float rx = -f.left.x;
         float ry = -f.left.y;
         float rz = -f.left.z;
         float ux = f.up.x;
         float uy = f.up.y;
         float uz = f.up.z;
         float ax = rx * c + ux * s;
         float ay = ry * c + uy * s;
         float az = rz * c + uz * s;
         float bx = -rx * s + ux * c;
         float by = -ry * s + uy * c;
         float bz = -rz * s + uz * c;
         int frames = sprite.frames();
         int f0 = Mth.floor(frame);
         float k = frame - (float)f0;
         int[] fr = new int[]{Math.floorMod(f0, frames), Math.floorMod(f0 + 1, frames)};
         float[] al = new float[]{alpha * (1.0F - k), alpha * k};
         float nx = -f.look.x;
         float ny = -f.look.y;
         float nz = -f.look.z;

         for (int i = 0; i < 2; i++) {
            if (!(al[i] <= 0.01F)) {
               float u0 = sprite.frameU(fr[i]);
               float v0 = sprite.frameV(fr[i]);
               float u1 = u0 + sprite.cellU();
               float v1 = v0 + sprite.cellV();
               int col = MeshWriter.argb(rgb, al[i]);
               MeshWriter.vertex(b, cx - ax - bx, cy - ay - by, cz - az - bz, col, u0, v1, -1.0F, 0, 0.0F, 0.0F, 0.0F, nx, ny, nz);
               MeshWriter.vertex(b, cx + ax - bx, cy + ay - by, cz + az - bz, col, u1, v1, -1.0F, 0, 0.0F, 0.0F, 0.0F, nx, ny, nz);
               MeshWriter.vertex(b, cx + ax + bx, cy + ay + by, cz + az + bz, col, u1, v0, -1.0F, 0, 0.0F, 0.0F, 0.0F, nx, ny, nz);
               MeshWriter.vertex(b, cx - ax + bx, cy - ay + by, cz - az + bz, col, u0, v0, -1.0F, 0, 0.0F, 0.0F, 0.0F, nx, ny, nz);
            }
         }
      }
   }

   public static void quad4D(
      BufferBuilder b,
      VfxFrame f,
      double[] a,
      double[] bb,
      double[] c,
      double[] d,
      float ua,
      float va,
      float ub,
      float vb,
      float uc,
      float vc,
      float ud,
      float vd,
      int rgb,
      float alphaAB,
      float alphaCD,
      float dissolveAB,
      float dissolveCD
   ) {
      float ax = f.rx(a[0]);
      float ay = f.ry(a[1]);
      float az = f.rz(a[2]);
      float bx = f.rx(bb[0]);
      float by = f.ry(bb[1]);
      float bz = f.rz(bb[2]);
      float cx = f.rx(c[0]);
      float cy = f.ry(c[1]);
      float cz = f.rz(c[2]);
      float dx = f.rx(d[0]);
      float dy = f.ry(d[1]);
      float dz = f.rz(d[2]);
      float ux = cx - ax;
      float uy = cy - ay;
      float uz = cz - az;
      float vx = dx - bx;
      float vy = dy - by;
      float vz = dz - bz;
      float nx = uy * vz - uz * vy;
      float ny = uz * vx - ux * vz;
      float nz = ux * vy - uy * vx;
      float nl = Mth.sqrt(nx * nx + ny * ny + nz * nz);
      if (!(nl < 1.0E-6F)) {
         nx /= nl;
         ny /= nl;
         nz /= nl;
         if (nx * -ax + ny * -ay + nz * -az < 0.0F) {
            nx = -nx;
            ny = -ny;
            nz = -nz;
         }

         int col = SandCinematic.scale(rgb, shade(nx, ny, nz));
         int cAB = MeshWriter.argb(col, alphaAB);
         int cCD = MeshWriter.argb(col, alphaCD);
         MeshWriter.vertex(b, ax, ay, az, cAB, ua, va, -1.0F, 3, 0.0F, dissolveAB, 0.0F, nx, ny, nz);
         MeshWriter.vertex(b, bx, by, bz, cAB, ub, vb, -1.0F, 3, 0.0F, dissolveAB, 0.0F, nx, ny, nz);
         MeshWriter.vertex(b, cx, cy, cz, cCD, uc, vc, -1.0F, 3, 0.0F, dissolveCD, 0.0F, nx, ny, nz);
         MeshWriter.vertex(b, dx, dy, dz, cCD, ud, vd, -1.0F, 3, 0.0F, dissolveCD, 0.0F, nx, ny, nz);
      }
   }

   public static void quadC(
      BufferBuilder b,
      VfxFrame f,
      double[] a,
      double[] bb,
      double[] c,
      double[] d,
      float ua,
      float va,
      float ub,
      float vb,
      float uc,
      float vc,
      float ud,
      float vd,
      int ca,
      int cb,
      int cc,
      int cd,
      float da,
      float db,
      float dc,
      float dd
   ) {
      float ax = f.rx(a[0]);
      float ay = f.ry(a[1]);
      float az = f.rz(a[2]);
      float bx = f.rx(bb[0]);
      float by = f.ry(bb[1]);
      float bz = f.rz(bb[2]);
      float cx = f.rx(c[0]);
      float cy = f.ry(c[1]);
      float cz = f.rz(c[2]);
      float dx = f.rx(d[0]);
      float dy = f.ry(d[1]);
      float dz = f.rz(d[2]);
      MeshWriter.vertex(b, ax, ay, az, ca, ua, va, -1.0F, 3, 0.0F, da, 0.0F, 0.0F, 1.0F, 0.0F);
      MeshWriter.vertex(b, bx, by, bz, cb, ub, vb, -1.0F, 3, 0.0F, db, 0.0F, 0.0F, 1.0F, 0.0F);
      MeshWriter.vertex(b, cx, cy, cz, cc, uc, vc, -1.0F, 3, 0.0F, dc, 0.0F, 0.0F, 1.0F, 0.0F);
      MeshWriter.vertex(b, dx, dy, dz, cd, ud, vd, -1.0F, 3, 0.0F, dd, 0.0F, 0.0F, 1.0F, 0.0F);
   }

   public static void quad4(
      BufferBuilder b,
      VfxFrame f,
      double[] a,
      double[] bb,
      double[] c,
      double[] d,
      float ua,
      float va,
      float ub,
      float vb,
      float uc,
      float vc,
      float ud,
      float vd,
      int rgb,
      float alphaAB,
      float alphaCD
   ) {
      float ax = f.rx(a[0]);
      float ay = f.ry(a[1]);
      float az = f.rz(a[2]);
      float bx = f.rx(bb[0]);
      float by = f.ry(bb[1]);
      float bz = f.rz(bb[2]);
      float cx = f.rx(c[0]);
      float cy = f.ry(c[1]);
      float cz = f.rz(c[2]);
      float dx = f.rx(d[0]);
      float dy = f.ry(d[1]);
      float dz = f.rz(d[2]);
      float ux = cx - ax;
      float uy = cy - ay;
      float uz = cz - az;
      float vx = dx - bx;
      float vy = dy - by;
      float vz = dz - bz;
      float nx = uy * vz - uz * vy;
      float ny = uz * vx - ux * vz;
      float nz = ux * vy - uy * vx;
      float nl = Mth.sqrt(nx * nx + ny * ny + nz * nz);
      if (!(nl < 1.0E-6F)) {
         nx /= nl;
         ny /= nl;
         nz /= nl;
         if (nx * -ax + ny * -ay + nz * -az < 0.0F) {
            nx = -nx;
            ny = -ny;
            nz = -nz;
         }

         int col = SandCinematic.scale(rgb, shade(nx, ny, nz));
         int cAB = MeshWriter.argb(col, alphaAB);
         int cCD = MeshWriter.argb(col, alphaCD);
         MeshWriter.vertex(b, ax, ay, az, cAB, ua, va, -1.0F, 0, 0.0F, 0.0F, 0.0F, nx, ny, nz);
         MeshWriter.vertex(b, bx, by, bz, cAB, ub, vb, -1.0F, 0, 0.0F, 0.0F, 0.0F, nx, ny, nz);
         MeshWriter.vertex(b, cx, cy, cz, cCD, uc, vc, -1.0F, 0, 0.0F, 0.0F, 0.0F, nx, ny, nz);
         MeshWriter.vertex(b, dx, dy, dz, cCD, ud, vd, -1.0F, 0, 0.0F, 0.0F, 0.0F, nx, ny, nz);
      }
   }

   public static void sphere(
      VfxFrame f,
      MeshTex tex,
      double cx,
      double cy,
      double cz,
      float r,
      float ax,
      float ay,
      float az,
      float squash,
      float u0,
      float uRepeat,
      float v0,
      float vRepeat,
      int rgb,
      float alpha,
      int rings,
      int segments
   ) {
      sphere(f, tex, cx, cy, cz, r, ax, ay, az, squash, u0, uRepeat, v0, vRepeat, rgb, alpha, rings, segments, false, (float) Math.PI);
   }

   public static void sphere(
      VfxFrame f,
      MeshTex tex,
      double cx,
      double cy,
      double cz,
      float r,
      float ax,
      float ay,
      float az,
      float squash,
      float u0,
      float uRepeat,
      float v0,
      float vRepeat,
      int rgb,
      float alpha,
      int rings,
      int segments,
      boolean meridian,
      float cap
   ) {
      if (f.allowMesh()) {
         float ex;
         float ey;
         float ez;
         if (Math.abs(ay) < 0.9F) {
            ex = az;
            ey = 0.0F;
            ez = -ax;
         } else {
            ex = 0.0F;
            ey = -az;
            ez = ay;
         }

         float el = Mth.sqrt(ex * ex + ey * ey + ez * ez);
         ex /= el;
         ey /= el;
         ez /= el;
         float gx = ay * ez - az * ey;
         float gy = az * ex - ax * ez;
         float gz = ax * ey - ay * ex;
         float ox = f.rx(cx);
         float oy = f.ry(cy);
         float oz = f.rz(cz);
         BufferBuilder b = f.mesh(tex);

         for (int ri = 0; ri < rings; ri++) {
            float t0 = cap * (float)ri / (float)rings;
            float t1 = cap * (float)(ri + 1) / (float)rings;

            for (int i = 0; i < segments; i++) {
               float p0 = (float) (Math.PI * 2) * (float)i / (float)segments;
               float p1 = (float) (Math.PI * 2) * (float)(i + 1) / (float)segments;
               float ua = u0 + uRepeat * (float)i / (float)segments;
               float ub = u0 + uRepeat * (float)(i + 1) / (float)segments;
               float va = v0 + vRepeat * (float)ri / (float)rings;
               float vb = v0 + vRepeat * (float)(ri + 1) / (float)rings;
               if (meridian) {
                  float ma = u0 + uRepeat * (float)ri / (float)rings;
                  float mb = u0 + uRepeat * (float)(ri + 1) / (float)rings;
                  float na = v0 + vRepeat * (float)i / (float)segments;
                  float nb = v0 + vRepeat * (float)(i + 1) / (float)segments;
                  sv(b, ox, oy, oz, r, squash, ax, ay, az, ex, ey, ez, gx, gy, gz, t0, p0, rgb, alpha, ma, na);
                  sv(b, ox, oy, oz, r, squash, ax, ay, az, ex, ey, ez, gx, gy, gz, t1, p0, rgb, alpha, mb, na);
                  sv(b, ox, oy, oz, r, squash, ax, ay, az, ex, ey, ez, gx, gy, gz, t1, p1, rgb, alpha, mb, nb);
                  sv(b, ox, oy, oz, r, squash, ax, ay, az, ex, ey, ez, gx, gy, gz, t0, p1, rgb, alpha, ma, nb);
               } else {
                  sv(b, ox, oy, oz, r, squash, ax, ay, az, ex, ey, ez, gx, gy, gz, t0, p0, rgb, alpha, ua, va);
                  sv(b, ox, oy, oz, r, squash, ax, ay, az, ex, ey, ez, gx, gy, gz, t1, p0, rgb, alpha, ua, vb);
                  sv(b, ox, oy, oz, r, squash, ax, ay, az, ex, ey, ez, gx, gy, gz, t1, p1, rgb, alpha, ub, vb);
                  sv(b, ox, oy, oz, r, squash, ax, ay, az, ex, ey, ez, gx, gy, gz, t0, p1, rgb, alpha, ub, va);
               }
            }
         }
      }
   }

   private static void sv(
      BufferBuilder b,
      float ox,
      float oy,
      float oz,
      float r,
      float squash,
      float ax,
      float ay,
      float az,
      float ex,
      float ey,
      float ez,
      float gx,
      float gy,
      float gz,
      float theta,
      float phi,
      int rgb,
      float alpha,
      float u,
      float v
   ) {
      float c = Mth.cos(theta);
      float s = Mth.sin(theta);
      float q = Mth.cos(phi);
      float w = Mth.sin(phi);
      float nx = ax * c + (ex * q + gx * w) * s;
      float ny = ay * c + (ey * q + gy * w) * s;
      float nz = az * c + (ez * q + gz * w) * s;
      float px = ax * c * squash + (ex * q + gx * w) * s;
      float py = ay * c * squash + (ey * q + gy * w) * s;
      float pz = az * c * squash + (ez * q + gz * w) * s;
      int col = MeshWriter.argb(SandCinematic.scale(rgb, shade(nx, ny, nz)), alpha);
      MeshWriter.vertex(b, ox + px * r, oy + py * r, oz + pz * r, col, u, v, -1.0F, 0, 0.0F, 0.0F, 0.0F, nx, ny, nz);
   }

   public static void loop(
      VfxFrame f,
      double cx,
      double cy,
      double cz,
      float ux,
      float uy,
      float uz,
      float vx,
      float vy,
      float vz,
      float radius,
      float width,
      int segments,
      float u0,
      float uRepeat,
      int rgb,
      float alpha
   ) {
      if (f.allowMesh()) {
         MeshWriter.ring(
            f.mesh(MeshTex.SAND_STREAM),
            f.rx(cx),
            f.ry(cy),
            f.rz(cz),
            ux,
            uy,
            uz,
            vx,
            vy,
            vz,
            radius,
            width,
            segments,
            u0,
            uRepeat,
            MeshWriter.argb(rgb, alpha),
            -1.0F,
            0,
            0.0F,
            0.0F
         );
      }
   }

   private SandDraw() {
   }

   static {
      Arrays.fill(HEAT, -1.0F);
   }
}
