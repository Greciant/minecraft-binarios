package com.devilfruits.client.vfx.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.util.Mth;

public final class MeshWriter {
   public static final int RING_NORMAL = 0;
   public static final int HEAT_HAZE = 1;
   public static final int GLASS = 2;
   public static final int BAND = 3;

   public static void vertex(
      BufferBuilder b,
      float x,
      float y,
      float z,
      int argb,
      float u,
      float v,
      float heat,
      int palette,
      float emissive,
      float dissolve,
      float fresnel,
      float nx,
      float ny,
      float nz
   ) {
      b.addVertex(x, y, z)
         .setColor(argb)
         .setUv(u, v)
         .setUv1(VfxFormat.unit(dissolve), VfxFormat.unit(fresnel))
         .setUv2(VfxFormat.heat(heat), VfxFormat.extra(palette, emissive))
         .setNormal(nx, ny, nz);
   }

   public static void strip(
      BufferBuilder b,
      int n,
      float[] xs,
      float[] ys,
      float[] zs,
      float[] width,
      float[] heat,
      float[] alpha,
      float u0,
      float du,
      int rgb,
      int palette,
      float emissive
   ) {
      if (n >= 2) {
         float pax = 0.0F;
         float pay = 0.0F;
         float paz = 0.0F;
         float pbx = 0.0F;
         float pby = 0.0F;
         float pbz = 0.0F;

         for (int i = 0; i < n; i++) {
            int a = Math.max(0, i - 1);
            int c = Math.min(n - 1, i + 1);
            float tx = xs[c] - xs[a];
            float ty = ys[c] - ys[a];
            float tz = zs[c] - zs[a];
            float sx = ty * zs[i] - tz * ys[i];
            float sy = tz * xs[i] - tx * zs[i];
            float sz = tx * ys[i] - ty * xs[i];
            float len = Mth.sqrt(sx * sx + sy * sy + sz * sz);
            if (len < 1.0E-5F) {
               sx = 0.0F;
               sy = 1.0F;
               sz = 0.0F;
               len = 1.0F;
            }

            float w = width[i] / len;
            float ax = xs[i] + sx * w;
            float ay = ys[i] + sy * w;
            float az = zs[i] + sz * w;
            float bx = xs[i] - sx * w;
            float by = ys[i] - sy * w;
            float bz = zs[i] - sz * w;
            if (i > 0) {
               float d = Mth.sqrt(xs[i] * xs[i] + ys[i] * ys[i] + zs[i] * zs[i]) + 1.0E-4F;
               float nx = -xs[i] / d;
               float ny = -ys[i] / d;
               float nz = -zs[i] / d;
               int c0 = argb(rgb, alpha[i - 1]);
               int c1 = argb(rgb, alpha[i]);
               float ua = u0 + (float)(i - 1) * du;
               float ub = u0 + (float)i * du;
               vertex(b, pax, pay, paz, c0, ua, 0.0F, heat[i - 1], palette, emissive, 0.0F, 0.0F, nx, ny, nz);
               vertex(b, pbx, pby, pbz, c0, ua, 1.0F, heat[i - 1], palette, emissive, 0.0F, 0.0F, nx, ny, nz);
               vertex(b, bx, by, bz, c1, ub, 1.0F, heat[i], palette, emissive, 0.0F, 0.0F, nx, ny, nz);
               vertex(b, ax, ay, az, c1, ub, 0.0F, heat[i], palette, emissive, 0.0F, 0.0F, nx, ny, nz);
            }

            pax = ax;
            pay = ay;
            paz = az;
            pbx = bx;
            pby = by;
            pbz = bz;
         }
      }
   }

   public static void ring(
      BufferBuilder b,
      float cx,
      float cy,
      float cz,
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
      int argb,
      float heat,
      int palette,
      float emissive,
      float dissolve
   ) {
      float nx = uy * vz - uz * vy;
      float ny = uz * vx - ux * vz;
      float nz = ux * vy - uy * vx;
      float r0 = Math.max(0.0F, radius - width * 0.5F);
      float r1 = radius + width * 0.5F;
      float pc = 1.0F;
      float ps = 0.0F;

      for (int i = 1; i <= segments; i++) {
         float a = (float) (Math.PI * 2) * (float)i / (float)segments;
         float c = Mth.cos(a);
         float s = Mth.sin(a);
         float ua = u0 + uRepeat * (float)(i - 1) / (float)segments;
         float ub = u0 + uRepeat * (float)i / (float)segments;
         float dx0 = ux * pc + vx * ps;
         float dy0 = uy * pc + vy * ps;
         float dz0 = uz * pc + vz * ps;
         float dx1 = ux * c + vx * s;
         float dy1 = uy * c + vy * s;
         float dz1 = uz * c + vz * s;
         vertex(b, cx + dx0 * r0, cy + dy0 * r0, cz + dz0 * r0, argb, ua, 0.0F, heat, palette, emissive, dissolve, 0.0F, nx, ny, nz);
         vertex(b, cx + dx0 * r1, cy + dy0 * r1, cz + dz0 * r1, argb, ua, 1.0F, heat, palette, emissive, dissolve, 0.0F, nx, ny, nz);
         vertex(b, cx + dx1 * r1, cy + dy1 * r1, cz + dz1 * r1, argb, ub, 1.0F, heat, palette, emissive, dissolve, 0.0F, nx, ny, nz);
         vertex(b, cx + dx1 * r0, cy + dy1 * r0, cz + dz1 * r0, argb, ub, 0.0F, heat, palette, emissive, dissolve, 0.0F, nx, ny, nz);
         pc = c;
         ps = s;
      }
   }

   public static void cone(
      BufferBuilder b,
      float bx,
      float by,
      float bz,
      float dx,
      float dy,
      float dz,
      float length,
      float r0,
      float r1,
      int segments,
      float u0,
      float uRepeat,
      float v0,
      float vRepeat,
      int argbBase,
      int argbTip,
      float heatBase,
      float heatTip,
      int palette,
      float emissive,
      float dissolve,
      float fresnel
   ) {
      float ax;
      float ay;
      float az;
      if (Math.abs(dy) < 0.9F) {
         ax = dz;
         ay = 0.0F;
         az = -dx;
      } else {
         ax = 0.0F;
         ay = -dz;
         az = dy;
      }

      float al = Mth.sqrt(ax * ax + ay * ay + az * az);
      ax /= al;
      ay /= al;
      az /= al;
      float cx = dy * az - dz * ay;
      float cy = dz * ax - dx * az;
      float cz = dx * ay - dy * ax;
      float tx = bx + dx * length;
      float ty = by + dy * length;
      float tz = bz + dz * length;
      float slope = (r0 - r1) / Math.max(0.001F, length);
      float pc = 1.0F;
      float ps = 0.0F;

      for (int i = 1; i <= segments; i++) {
         float a = (float) (Math.PI * 2) * (float)i / (float)segments;
         float c = Mth.cos(a);
         float s = Mth.sin(a);
         float ua = u0 + uRepeat * (float)(i - 1) / (float)segments;
         float ub = u0 + uRepeat * (float)i / (float)segments;
         float ox0 = ax * pc + cx * ps;
         float oy0 = ay * pc + cy * ps;
         float oz0 = az * pc + cz * ps;
         float ox1 = ax * c + cx * s;
         float oy1 = ay * c + cy * s;
         float oz1 = az * c + cz * s;
         vertex(
            b,
            bx + ox0 * r0,
            by + oy0 * r0,
            bz + oz0 * r0,
            argbBase,
            ua,
            v0,
            heatBase,
            palette,
            emissive,
            dissolve,
            fresnel,
            ox0 + dx * slope,
            oy0 + dy * slope,
            oz0 + dz * slope
         );
         vertex(
            b,
            tx + ox0 * r1,
            ty + oy0 * r1,
            tz + oz0 * r1,
            argbTip,
            ua,
            v0 + vRepeat,
            heatTip,
            palette,
            emissive,
            dissolve,
            fresnel,
            ox0 + dx * slope,
            oy0 + dy * slope,
            oz0 + dz * slope
         );
         vertex(
            b,
            tx + ox1 * r1,
            ty + oy1 * r1,
            tz + oz1 * r1,
            argbTip,
            ub,
            v0 + vRepeat,
            heatTip,
            palette,
            emissive,
            dissolve,
            fresnel,
            ox1 + dx * slope,
            oy1 + dy * slope,
            oz1 + dz * slope
         );
         vertex(
            b,
            bx + ox1 * r0,
            by + oy1 * r0,
            bz + oz1 * r0,
            argbBase,
            ub,
            v0,
            heatBase,
            palette,
            emissive,
            dissolve,
            fresnel,
            ox1 + dx * slope,
            oy1 + dy * slope,
            oz1 + dz * slope
         );
         pc = c;
         ps = s;
      }
   }

   public static void sphere(
      BufferBuilder b,
      float cx,
      float cy,
      float cz,
      float radius,
      int rings,
      int segments,
      float u0,
      float v0,
      float uRepeat,
      int argb,
      float heat,
      int palette,
      float emissive,
      float dissolve,
      float fresnel
   ) {
      for (int r = 0; r < rings; r++) {
         float t0 = (float) Math.PI * (float)r / (float)rings;
         float t1 = (float) Math.PI * (float)(r + 1) / (float)rings;
         float y0 = Mth.cos(t0);
         float y1 = Mth.cos(t1);
         float s0 = Mth.sin(t0);
         float s1 = Mth.sin(t1);

         for (int i = 0; i < segments; i++) {
            float a0 = (float) (Math.PI * 2) * (float)i / (float)segments;
            float a1 = (float) (Math.PI * 2) * (float)(i + 1) / (float)segments;
            float c0 = Mth.cos(a0);
            float n0 = Mth.sin(a0);
            float c1 = Mth.cos(a1);
            float n1 = Mth.sin(a1);
            float ua = u0 + uRepeat * (float)i / (float)segments;
            float ub = u0 + uRepeat * (float)(i + 1) / (float)segments;
            float va = v0 + (float)r / (float)rings;
            float vb = v0 + (float)(r + 1) / (float)rings;
            sv(b, cx, cy, cz, radius, c0 * s0, y0, n0 * s0, argb, ua, va, heat, palette, emissive, dissolve, fresnel);
            sv(b, cx, cy, cz, radius, c0 * s1, y1, n0 * s1, argb, ua, vb, heat, palette, emissive, dissolve, fresnel);
            sv(b, cx, cy, cz, radius, c1 * s1, y1, n1 * s1, argb, ub, vb, heat, palette, emissive, dissolve, fresnel);
            sv(b, cx, cy, cz, radius, c1 * s0, y0, n1 * s0, argb, ub, va, heat, palette, emissive, dissolve, fresnel);
         }
      }
   }

   public static void dome(
      BufferBuilder b,
      float cx,
      float cy,
      float cz,
      float ax,
      float ay,
      float az,
      float radius,
      float maxAngle,
      int rings,
      int segments,
      float u0,
      float v0,
      float uRepeat,
      float vRepeat,
      int argb,
      float heat,
      int palette,
      float emissive,
      float dissolve,
      float fresnel
   ) {
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
      float fx = ay * ez - az * ey;
      float fy = az * ex - ax * ez;
      float fz = ax * ey - ay * ex;

      for (int r = 0; r < rings; r++) {
         float t0 = maxAngle * (float)r / (float)rings;
         float t1 = maxAngle * (float)(r + 1) / (float)rings;
         float c0 = Mth.cos(t0);
         float s0 = Mth.sin(t0);
         float c1 = Mth.cos(t1);
         float s1 = Mth.sin(t1);
         float va = v0 + vRepeat * (float)r / (float)rings;
         float vb = v0 + vRepeat * (float)(r + 1) / (float)rings;

         for (int i = 0; i < segments; i++) {
            float p0 = (float) (Math.PI * 2) * (float)i / (float)segments;
            float p1 = (float) (Math.PI * 2) * (float)(i + 1) / (float)segments;
            float q0 = Mth.cos(p0);
            float w0 = Mth.sin(p0);
            float q1 = Mth.cos(p1);
            float w1 = Mth.sin(p1);
            float ua = u0 + uRepeat * (float)i / (float)segments;
            float ub = u0 + uRepeat * (float)(i + 1) / (float)segments;
            capVertex(
               b,
               cx,
               cy,
               cz,
               radius,
               ax * c0 + (ex * q0 + fx * w0) * s0,
               ay * c0 + (ey * q0 + fy * w0) * s0,
               az * c0 + (ez * q0 + fz * w0) * s0,
               argb,
               ua,
               va,
               heat,
               palette,
               emissive,
               dissolve,
               fresnel
            );
            capVertex(
               b,
               cx,
               cy,
               cz,
               radius,
               ax * c1 + (ex * q0 + fx * w0) * s1,
               ay * c1 + (ey * q0 + fy * w0) * s1,
               az * c1 + (ez * q0 + fz * w0) * s1,
               argb,
               ua,
               vb,
               heat,
               palette,
               emissive,
               dissolve,
               fresnel
            );
            capVertex(
               b,
               cx,
               cy,
               cz,
               radius,
               ax * c1 + (ex * q1 + fx * w1) * s1,
               ay * c1 + (ey * q1 + fy * w1) * s1,
               az * c1 + (ez * q1 + fz * w1) * s1,
               argb,
               ub,
               vb,
               heat,
               palette,
               emissive,
               dissolve,
               fresnel
            );
            capVertex(
               b,
               cx,
               cy,
               cz,
               radius,
               ax * c0 + (ex * q1 + fx * w1) * s0,
               ay * c0 + (ey * q1 + fy * w1) * s0,
               az * c0 + (ez * q1 + fz * w1) * s0,
               argb,
               ub,
               va,
               heat,
               palette,
               emissive,
               dissolve,
               fresnel
            );
         }
      }
   }

   private static void capVertex(
      BufferBuilder b,
      float cx,
      float cy,
      float cz,
      float r,
      float nx,
      float ny,
      float nz,
      int argb,
      float u,
      float v,
      float heat,
      int palette,
      float emissive,
      float dissolve,
      float fresnel
   ) {
      vertex(b, cx + nx * r, cy + ny * r, cz + nz * r, argb, u, v, heat, palette, emissive, dissolve, fresnel, nx, ny, nz);
   }

   private static void sv(
      BufferBuilder b,
      float cx,
      float cy,
      float cz,
      float r,
      float nx,
      float ny,
      float nz,
      int argb,
      float u,
      float v,
      float heat,
      int palette,
      float emissive,
      float dissolve,
      float fresnel
   ) {
      vertex(b, cx + nx * r, cy + ny * r, cz + nz * r, argb, u, v, heat, palette, emissive, dissolve, fresnel, nx, ny, nz);
   }

   public static void distortQuad(
      BufferBuilder b,
      float x,
      float y,
      float z,
      float size,
      float strength,
      int mode,
      boolean flat,
      float lx,
      float ly,
      float lz,
      float upx,
      float upy,
      float upz,
      float u0,
      float v0,
      float u1,
      float v1,
      float noiseU,
      float noiseV
   ) {
      float ax;
      float ay;
      float az;
      float bx;
      float by;
      float bz;
      if (flat) {
         ax = size;
         ay = 0.0F;
         az = 0.0F;
         bx = 0.0F;
         by = 0.0F;
         bz = size;
      } else {
         ax = -lx * size;
         ay = -ly * size;
         az = -lz * size;
         bx = upx * size;
         by = upy * size;
         bz = upz * size;
      }

      int argb = Mth.clamp((int)(strength * 255.0F), 0, 255) << 24 | 16777215;
      noiseU = (noiseU - (float)Mth.floor(noiseU)) * 0.75F;
      noiseV = (noiseV - (float)Mth.floor(noiseV)) * 0.75F;
      dv(b, x - ax - bx, y - ay - by, z - az - bz, argb, u0, v1, noiseU, noiseV, mode);
      dv(b, x + ax - bx, y + ay - by, z + az - bz, argb, u1, v1, noiseU + 0.25F, noiseV, mode);
      dv(b, x + ax + bx, y + ay + by, z + az + bz, argb, u1, v0, noiseU + 0.25F, noiseV + 0.25F, mode);
      dv(b, x - ax + bx, y - ay + by, z - az + bz, argb, u0, v0, noiseU, noiseV + 0.25F, mode);
   }

   private static void dv(BufferBuilder b, float x, float y, float z, int argb, float u, float v, float nu, float nv, int mode) {
      b.addVertex(x, y, z).setColor(argb).setUv(u, v).setUv1(VfxFormat.unit(nu), VfxFormat.unit(nv)).setUv2(mode, 0).setNormal(0.0F, 1.0F, 0.0F);
   }

   public static void distortVertex(BufferBuilder b, float x, float y, float z, float ox, float oy, float strength, float v, int mode) {
      int r = Mth.clamp((int)((ox * 0.5F + 0.5F) * 255.0F), 0, 255);
      int g = Mth.clamp((int)((oy * 0.5F + 0.5F) * 255.0F), 0, 255);
      int argb = Mth.clamp((int)(strength * 255.0F), 0, 255) << 24 | r << 16 | g << 8;
      b.addVertex(x, y, z).setColor(argb).setUv(0.5F, v).setUv1(0, 0).setUv2(mode, 0).setNormal(0.0F, 1.0F, 0.0F);
   }

   public static int argb(int rgb, float alpha) {
      return Mth.clamp((int)(alpha * 255.0F + 0.5F), 0, 255) << 24 | rgb & 16777215;
   }

   private MeshWriter() {
   }
}
