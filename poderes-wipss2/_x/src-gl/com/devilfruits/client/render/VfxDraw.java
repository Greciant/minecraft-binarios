package com.devilfruits.client.render;

import com.devilfruits.client.render.vfx.VfxContext;
import com.devilfruits.core.util.DeterministicRandom;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class VfxDraw {
   private static final Vector3f A = new Vector3f();
   private static final Vector3f B = new Vector3f();
   private static final Vector3f D = new Vector3f();
   private static final Vector3f W = new Vector3f();
   private static final Vector3f N = new Vector3f();
   private static final Vector3f BP = new Vector3f();
   private static final Vector3f BA = new Vector3f();
   private static final Vector3f BB = new Vector3f();

   public static void ribbon(VertexConsumer vc, Matrix4f m, Vector3f a, Vector3f b, Vector3f w, float halfWidth, int r, int g, int bl, int alpha) {
      float wx = w.x * halfWidth;
      float wy = w.y * halfWidth;
      float wz = w.z * halfWidth;
      vc.addVertex(m, a.x - wx, a.y - wy, a.z - wz).setColor(r, g, bl, alpha);
      vc.addVertex(m, a.x + wx, a.y + wy, a.z + wz).setColor(r, g, bl, alpha);
      vc.addVertex(m, b.x + wx, b.y + wy, b.z + wz).setColor(r, g, bl, alpha);
      vc.addVertex(m, b.x - wx, b.y - wy, b.z - wz).setColor(r, g, bl, alpha);
   }

   public static void ring(
      VfxContext ctx, float cx, float cy, float cz, Vector3f u, Vector3f v, float radius, float thickness, int segments, int r, int g, int b, float alpha
   ) {
      if (!(alpha <= 0.003F) && !(radius <= 0.0F)) {
         VertexConsumer vc = ctx.buffer;
         Matrix4f m = ctx.matrix;
         int a = Mth.clamp((int)(alpha * 255.0F), 0, 255);
         float inner = Math.max(0.0F, radius - thickness * 0.5F);
         float outer = radius + thickness * 0.5F;

         for (int i = 0; i < segments; i++) {
            float a0 = (float) (Math.PI * 2) * (float)i / (float)segments;
            float a1 = (float) (Math.PI * 2) * (float)(i + 1) / (float)segments;
            float c0 = Mth.cos(a0);
            float s0 = Mth.sin(a0);
            float c1 = Mth.cos(a1);
            float s1 = Mth.sin(a1);
            vert(vc, m, cx, cy, cz, u, v, c0, s0, inner, r, g, b, 0);
            vert(vc, m, cx, cy, cz, u, v, c0, s0, radius, r, g, b, a);
            vert(vc, m, cx, cy, cz, u, v, c1, s1, radius, r, g, b, a);
            vert(vc, m, cx, cy, cz, u, v, c1, s1, inner, r, g, b, 0);
            vert(vc, m, cx, cy, cz, u, v, c0, s0, radius, r, g, b, a);
            vert(vc, m, cx, cy, cz, u, v, c0, s0, outer, r, g, b, 0);
            vert(vc, m, cx, cy, cz, u, v, c1, s1, outer, r, g, b, 0);
            vert(vc, m, cx, cy, cz, u, v, c1, s1, radius, r, g, b, a);
         }
      }
   }

   private static void vert(
      VertexConsumer vc, Matrix4f m, float cx, float cy, float cz, Vector3f u, Vector3f v, float c, float s, float rad, int r, int g, int b, int a
   ) {
      vc.addVertex(m, cx + (u.x * c + v.x * s) * rad, cy + (u.y * c + v.y * s) * rad, cz + (u.z * c + v.z * s) * rad).setColor(r, g, b, a);
   }

   public static void shell(
      VfxContext ctx,
      float cx,
      float cy,
      float cz,
      float rx,
      float ry,
      float rz,
      int stacks,
      int slices,
      int r,
      int g,
      int b,
      float centerAlpha,
      float rimAlpha
   ) {
      if (!(rimAlpha <= 0.003F) || !(centerAlpha <= 0.003F)) {
         VertexConsumer vc = ctx.buffer;
         Matrix4f m = ctx.matrix;
         float len = (float)Math.sqrt((double)(cx * cx + cy * cy + cz * cz));
         float vx = len > 1.0E-4F ? -cx / len : 0.0F;
         float vy = len > 1.0E-4F ? -cy / len : 0.0F;
         float vz = len > 1.0E-4F ? -cz / len : 1.0F;

         for (int i = 0; i < stacks; i++) {
            float t0 = (float) Math.PI * (float)i / (float)stacks;
            float t1 = (float) Math.PI * (float)(i + 1) / (float)stacks;

            for (int j = 0; j < slices; j++) {
               float p0 = (float) (Math.PI * 2) * (float)j / (float)slices;
               float p1 = (float) (Math.PI * 2) * (float)(j + 1) / (float)slices;
               shellVert(vc, m, cx, cy, cz, rx, ry, rz, t0, p0, vx, vy, vz, r, g, b, centerAlpha, rimAlpha);
               shellVert(vc, m, cx, cy, cz, rx, ry, rz, t1, p0, vx, vy, vz, r, g, b, centerAlpha, rimAlpha);
               shellVert(vc, m, cx, cy, cz, rx, ry, rz, t1, p1, vx, vy, vz, r, g, b, centerAlpha, rimAlpha);
               shellVert(vc, m, cx, cy, cz, rx, ry, rz, t0, p1, vx, vy, vz, r, g, b, centerAlpha, rimAlpha);
            }
         }
      }
   }

   private static void shellVert(
      VertexConsumer vc,
      Matrix4f m,
      float cx,
      float cy,
      float cz,
      float rx,
      float ry,
      float rz,
      float theta,
      float phi,
      float vx,
      float vy,
      float vz,
      int r,
      int g,
      int b,
      float ca,
      float ra
   ) {
      float st = Mth.sin(theta);
      float nx = st * Mth.cos(phi);
      float ny = Mth.cos(theta);
      float nz = st * Mth.sin(phi);
      float facing = Math.abs(nx * vx + ny * vy + nz * vz);
      float rim = (1.0F - facing) * (1.0F - facing);
      int a = Mth.clamp((int)((ca + (ra - ca) * rim) * 255.0F), 0, 255);
      vc.addVertex(m, cx + nx * rx, cy + ny * ry, cz + nz * rz).setColor(r, g, b, a);
   }

   public static void billboard(VfxContext ctx, float cx, float cy, float cz, float halfSize, int r, int g, int b, float alpha) {
      int a = Mth.clamp((int)(alpha * 255.0F), 0, 255);
      if (a > 0) {
         Vector3f l = ctx.camLeft;
         Vector3f u = ctx.camUp;
         VertexConsumer vc = ctx.buffer;
         Matrix4f m = ctx.matrix;
         vc.addVertex(m, cx + (l.x + u.x) * halfSize, cy + (l.y + u.y) * halfSize, cz + (l.z + u.z) * halfSize).setColor(r, g, b, a);
         vc.addVertex(m, cx + (l.x - u.x) * halfSize, cy + (l.y - u.y) * halfSize, cz + (l.z - u.z) * halfSize).setColor(r, g, b, a);
         vc.addVertex(m, cx + (-l.x - u.x) * halfSize, cy + (-l.y - u.y) * halfSize, cz + (-l.z - u.z) * halfSize).setColor(r, g, b, a);
         vc.addVertex(m, cx + (-l.x + u.x) * halfSize, cy + (-l.y + u.y) * halfSize, cz + (-l.z + u.z) * halfSize).setColor(r, g, b, a);
      }
   }

   public static void beam(
      VfxContext ctx, float ax, float ay, float az, float bx, float by, float bz, float coreWidth, float glowWidth, int coreRgb, int glowRgb, float alpha
   ) {
      if (!(alpha <= 0.003F)) {
         A.set(ax, ay, az);
         B.set(bx, by, bz);
         D.set(B).sub(A);
         W.set(D).cross(A.x + B.x, A.y + B.y, A.z + B.z);
         if (!(W.lengthSquared() < 1.0E-10F)) {
            W.normalize();
            int ga = Mth.clamp((int)(alpha * 0.45F * 255.0F), 0, 255);
            int ca = Mth.clamp((int)(alpha * 255.0F), 0, 255);
            ribbon(ctx.buffer, ctx.matrix, A, B, W, glowWidth, glowRgb >> 16 & 0xFF, glowRgb >> 8 & 0xFF, glowRgb & 0xFF, ga);
            ribbon(ctx.buffer, ctx.matrix, A, B, W, coreWidth, coreRgb >> 16 & 0xFF, coreRgb >> 8 & 0xFF, coreRgb & 0xFF, ca);
         }
      }
   }

   public static void basis(Vector3f normal, Vector3f u, Vector3f v) {
      if (Math.abs(normal.y) < 0.95F) {
         u.set(0.0F, 1.0F, 0.0F).cross(normal).normalize();
      } else {
         u.set(1.0F, 0.0F, 0.0F).cross(normal).normalize();
      }

      v.set(normal).cross(u).normalize();
   }

   private VfxDraw() {
   }

   public static void texturedBox(
      VertexConsumer vc,
      Matrix4f pose,
      Matrix3f rot,
      float x0,
      float y0,
      float z0,
      float x1,
      float y1,
      float z1,
      int tileU,
      int tileV,
      int tile,
      int texW,
      int texH,
      int argb,
      int light
   ) {
      float sx = x1 - x0;
      float sy = y1 - y0;
      float sz = z1 - z0;
      face(vc, pose, rot, 1.0F, 0.0F, 0.0F, x1, y1, z1, x1, y1, z0, x1, y0, z0, x1, y0, z1, sz, sy, tileU, tileV, tile, texW, texH, argb, light);
      face(vc, pose, rot, -1.0F, 0.0F, 0.0F, x0, y1, z0, x0, y1, z1, x0, y0, z1, x0, y0, z0, sz, sy, tileU, tileV, tile, texW, texH, argb, light);
      face(vc, pose, rot, 0.0F, 1.0F, 0.0F, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, sx, sz, tileU, tileV, tile, texW, texH, argb, light);
      face(vc, pose, rot, 0.0F, -1.0F, 0.0F, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0, sx, sz, tileU, tileV, tile, texW, texH, argb, light);
      face(vc, pose, rot, 0.0F, 0.0F, 1.0F, x0, y1, z1, x1, y1, z1, x1, y0, z1, x0, y0, z1, sx, sy, tileU, tileV, tile, texW, texH, argb, light);
      face(vc, pose, rot, 0.0F, 0.0F, -1.0F, x1, y1, z0, x0, y1, z0, x0, y0, z0, x1, y0, z0, sx, sy, tileU, tileV, tile, texW, texH, argb, light);
   }

   private static void face(
      VertexConsumer vc,
      Matrix4f pose,
      Matrix3f rot,
      float nx,
      float ny,
      float nz,
      float ax,
      float ay,
      float az,
      float bx,
      float by,
      float bz,
      float cx,
      float cy,
      float cz,
      float dx,
      float dy,
      float dz,
      float w,
      float h,
      int tileU,
      int tileV,
      int tile,
      int texW,
      int texH,
      int argb,
      int light
   ) {
      float u0 = ((float)tileU + ((float)tile - w) * 0.5F) / (float)texW;
      float u1 = ((float)tileU + ((float)tile + w) * 0.5F) / (float)texW;
      float v0 = ((float)tileV + ((float)tile - h) * 0.5F) / (float)texH;
      float v1 = ((float)tileV + ((float)tile + h) * 0.5F) / (float)texH;
      rot.transform(N.set(nx, ny, nz));
      int overlay = OverlayTexture.NO_OVERLAY;
      vc.addVertex(pose, ax, ay, az).setColor(argb).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(N.x, N.y, N.z);
      vc.addVertex(pose, bx, by, bz).setColor(argb).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(N.x, N.y, N.z);
      vc.addVertex(pose, cx, cy, cz).setColor(argb).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(N.x, N.y, N.z);
      vc.addVertex(pose, dx, dy, dz).setColor(argb).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(N.x, N.y, N.z);
   }

   public static void tiledBox(VertexConsumer vc, Matrix4f pose, Matrix3f rot, float hx, float hy, float hz, float uOffset, float vOffset, int argb, int light) {
      float x0 = -hx;
      float y0 = -hy;
      float z0 = -hz;
      float sx = hx * 2.0F;
      float sy = hy * 2.0F;
      float sz = hz * 2.0F;
      tiledFace(vc, pose, rot, 1.0F, 0.0F, 0.0F, hx, hy, hz, hx, hy, z0, hx, y0, z0, hx, y0, hz, sz, sy, uOffset, vOffset, argb, light);
      tiledFace(vc, pose, rot, -1.0F, 0.0F, 0.0F, x0, hy, z0, x0, hy, hz, x0, y0, hz, x0, y0, z0, sz, sy, uOffset + 0.37F, vOffset, argb, light);
      tiledFace(vc, pose, rot, 0.0F, 1.0F, 0.0F, x0, hy, z0, hx, hy, z0, hx, hy, hz, x0, hy, hz, sx, sz, uOffset, vOffset + 0.5F, argb, light);
      tiledFace(vc, pose, rot, 0.0F, -1.0F, 0.0F, x0, y0, hz, hx, y0, hz, hx, y0, z0, x0, y0, z0, sx, sz, uOffset + 0.25F, vOffset, argb, light);
      tiledFace(vc, pose, rot, 0.0F, 0.0F, 1.0F, x0, hy, hz, hx, hy, hz, hx, y0, hz, x0, y0, hz, sx, sy, uOffset + 0.62F, vOffset + 0.25F, argb, light);
      tiledFace(vc, pose, rot, 0.0F, 0.0F, -1.0F, hx, hy, z0, x0, hy, z0, x0, y0, z0, hx, y0, z0, sx, sy, uOffset + 0.12F, vOffset + 0.75F, argb, light);
   }

   private static void tiledFace(
      VertexConsumer vc,
      Matrix4f pose,
      Matrix3f rot,
      float nx,
      float ny,
      float nz,
      float ax,
      float ay,
      float az,
      float bx,
      float by,
      float bz,
      float cx,
      float cy,
      float cz,
      float dx,
      float dy,
      float dz,
      float w,
      float h,
      float u0,
      float v0,
      int argb,
      int light
   ) {
      u0 = (float)Math.round(u0 * 16.0F) / 16.0F;
      v0 = (float)Math.round(v0 * 16.0F) / 16.0F;
      float u1 = u0 + w;
      float v1 = v0 + h;
      rot.transform(N.set(nx, ny, nz));
      int overlay = OverlayTexture.NO_OVERLAY;
      vc.addVertex(pose, ax, ay, az).setColor(argb).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(N.x, N.y, N.z);
      vc.addVertex(pose, bx, by, bz).setColor(argb).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(N.x, N.y, N.z);
      vc.addVertex(pose, cx, cy, cz).setColor(argb).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(N.x, N.y, N.z);
      vc.addVertex(pose, dx, dy, dz).setColor(argb).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(N.x, N.y, N.z);
   }

   public static int boltPointCount(int depth) {
      return (1 << depth) + 1;
   }

   public static void boltPath(float[] out, float dx, float dy, float dz, int depth, float jag, long seed) {
      int n = boltPointCount(depth);
      out[0] = 0.0F;
      out[1] = 0.0F;
      out[2] = 0.0F;
      int last = (n - 1) * 3;
      out[last] = dx;
      out[last + 1] = dy;
      out[last + 2] = dz;
      BP.set(dx, dy, dz);
      float len = BP.length();
      if (len < 1.0E-4F) {
         for (int i = 3; i < last; i++) {
            out[i] = 0.0F;
         }
      } else {
         BP.div(len);
         basis(BP, BA, BB);
         DeterministicRandom r = new DeterministicRandom(seed);

         for (int step = n - 1; step > 1; step >>= 1) {
            int half = step >> 1;
            float amp = jag * len * (float)step / (float)(n - 1);

            for (int i = half; i < n - 1; i += step) {
               int lo = (i - half) * 3;
               int hi = (i + half) * 3;
               int mid = i * 3;
               float ou = r.signed(amp);
               float ov = r.signed(amp);
               out[mid] = (out[lo] + out[hi]) * 0.5F + BA.x * ou + BB.x * ov;
               out[mid + 1] = (out[lo + 1] + out[hi + 1]) * 0.5F + BA.y * ou + BB.y * ov;
               out[mid + 2] = (out[lo + 2] + out[hi + 2]) * 0.5F + BA.z * ou + BB.z * ov;
            }
         }
      }
   }
}
