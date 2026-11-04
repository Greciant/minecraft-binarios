package com.devilfruits.fruits.supa_supa.client.blade;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class BladeMesh {
   private static final Vector3f P = new Vector3f();
   private static final Vector3f N = new Vector3f();
   private static final float[] A0 = new float[3];
   private static final float[] A1 = new float[3];
   private static final float[] A3 = new float[3];

   static BladeMesh.Shape shape(
      float[] origin, float[] alongAx, float[] acrossAx, float[] thickAx, float from, float to, int stations, BladeMesh.Profile p, float spine, float ridge
   ) {
      float[] al = new float[stations];
      float[] bk = new float[stations];
      float[] ed = new float[stations];
      float[] hf = new float[stations];
      float[] out = new float[3];
      boolean own = false;

      for (int i = 0; i < stations; i++) {
         float s = (float)i / (float)(stations - 1);
         out[2] = -1.0F;
         p.at(s, out);
         al[i] = Mth.lerp(s, from, to);
         bk[i] = out[0];
         ed[i] = out[1];
         hf[i] = out[2];
         own |= out[2] >= 0.0F;
      }

      return new BladeMesh.Shape(origin, alongAx, acrossAx, thickAx, al, bk, ed, own ? hf : null, spine, ridge);
   }

   static void emit(VertexConsumer vc, Pose pose, BladeMesh.Shape s, float sx, float reach, int light, int argb, float vScale) {
      Matrix4f m = pose.pose();
      Matrix3f nm = pose.normal();
      int n = s.along.length;
      float limit = s.along[0] + (s.along[n - 1] - s.along[0]) * Mth.clamp(reach, 0.0F, 1.0F);
      boolean rising = s.along[n - 1] > s.along[0];
      float thickHalf = s.spine * 0.5F;

      for (int i = 0; i < n - 1; i++) {
         float al0 = s.along[i];
         float al1 = s.along[i + 1];
         float b0 = s.back[i];
         float e0 = s.edge[i];
         float b1 = s.back[i + 1];
         float e1 = s.edge[i + 1];
         if (rising ? al0 >= limit : al0 <= limit) {
            break;
         }

         if (rising ? al1 > limit : al1 < limit) {
            float f = (limit - al0) / (al1 - al0);
            al1 = limit;
            float bf = Mth.lerp(f, b0, b1);
            float ef = Mth.lerp(f, e0, e1);
            float mid = (bf + ef) * 0.5F;
            b1 = Mth.lerp(0.6F, bf, mid);
            e1 = Mth.lerp(0.6F, ef, mid);
         }

         float u0 = (al0 - s.along[0]) / 16.0F;
         float u1 = (al1 - s.along[0]) / 16.0F;
         float r0 = Mth.lerp(s.ridge, b0, e0);
         float r1 = Mth.lerp(s.ridge, b1, e1);
         float w0 = Math.abs(e0 - b0);
         float w1 = Math.abs(e1 - b1);
         float h0 = s.half != null ? s.half[i] : thickHalf * Math.min(1.0F, w0 / 1.2F + 0.15F);
         float h1 = s.half != null ? s.half[i + 1] : thickHalf * Math.min(1.0F, w1 / 1.2F + 0.15F);

         for (int side = -1; side <= 1; side += 2) {
            quad(vc, m, nm, s, sx, side, al0, b0, h0, al1, b1, h1, al1, r1, h1 * 0.85F, al0, r0, h0 * 0.85F, u0, u1, 0.0F, s.ridge * vScale, light, argb);
            quad(vc, m, nm, s, sx, side, al0, r0, h0 * 0.85F, al1, r1, h1 * 0.85F, al1, e1, 0.0F, al0, e0, 0.0F, u0, u1, s.ridge * vScale, vScale, light, argb);
         }

         quadSpine(vc, m, nm, s, sx, al0, b0, h0, al1, b1, h1, u0, u1, light, argb);
         if (i == n - 2 && Math.abs(e1 - b1) > 0.05F) {
            cap(vc, m, nm, s, sx, al1, b1, Mth.lerp(s.ridge, b1, e1), e1, h1, light, argb);
         }
      }
   }

   private static void cap(
      VertexConsumer vc, Matrix4f m, Matrix3f nm, BladeMesh.Shape s, float sx, float al, float b, float r, float e, float h, int light, int argb
   ) {
      nm.transform(s.a[0] * sx, s.a[1], s.a[2], N);
      float u = 0.5F;
      vertex(vc, m, s, sx, al, b, -h, u, 0.1F, light, argb);
      vertex(vc, m, s, sx, al, b, h, u, 0.1F, light, argb);
      vertex(vc, m, s, sx, al, r, h * 0.85F, u, 0.5F, light, argb);
      vertex(vc, m, s, sx, al, r, -h * 0.85F, u, 0.5F, light, argb);
      vertex(vc, m, s, sx, al, r, -h * 0.85F, u, 0.5F, light, argb);
      vertex(vc, m, s, sx, al, r, h * 0.85F, u, 0.5F, light, argb);
      vertex(vc, m, s, sx, al, e, 0.0F, u, 0.95F, light, argb);
      vertex(vc, m, s, sx, al, e, 0.0F, u, 0.95F, light, argb);
   }

   private static void quad(
      VertexConsumer vc,
      Matrix4f m,
      Matrix3f nm,
      BladeMesh.Shape s,
      float sx,
      int side,
      float al0,
      float c0,
      float t0,
      float al1,
      float c1,
      float t1,
      float al2,
      float c2,
      float t2,
      float al3,
      float c3,
      float t3,
      float u0,
      float u1,
      float v0,
      float v1,
      int light,
      int argb
   ) {
      point(s, sx, al0, c0, t0 * (float)side, A0);
      point(s, sx, al1, c1, t1 * (float)side, A1);
      point(s, sx, al3, c3, t3 * (float)side, A3);
      float ex = A1[0] - A0[0];
      float ey = A1[1] - A0[1];
      float ez = A1[2] - A0[2];
      float fx = A3[0] - A0[0];
      float fy = A3[1] - A0[1];
      float fz = A3[2] - A0[2];
      float nx = ey * fz - ez * fy;
      float ny = ez * fx - ex * fz;
      float nz = ex * fy - ey * fx;
      float len = Mth.sqrt(nx * nx + ny * ny + nz * nz);
      if (!(len < 1.0E-6F)) {
         float tx = s.t[0] * sx;
         float ty = s.t[1];
         float tz = s.t[2];
         if ((nx * tx + ny * ty + nz * tz) * (float)side < 0.0F) {
            nx = -nx;
            ny = -ny;
            nz = -nz;
         }

         nx /= len;
         ny /= len;
         nz /= len;
         nm.transform(nx, ny, nz, N);
         vertex(vc, m, s, sx, al0, c0, t0 * (float)side, u0, v0, light, argb);
         vertex(vc, m, s, sx, al1, c1, t1 * (float)side, u1, v0, light, argb);
         vertex(vc, m, s, sx, al2, c2, t2 * (float)side, u1, v1, light, argb);
         vertex(vc, m, s, sx, al3, c3, t3 * (float)side, u0, v1, light, argb);
      }
   }

   private static void quadSpine(
      VertexConsumer vc,
      Matrix4f m,
      Matrix3f nm,
      BladeMesh.Shape s,
      float sx,
      float al0,
      float b0,
      float h0,
      float al1,
      float b1,
      float h1,
      float u0,
      float u1,
      int light,
      int argb
   ) {
      float nx = -s.c[0] * sx;
      float ny = -s.c[1];
      float nz = -s.c[2];
      nm.transform(nx, ny, nz, N);
      float v = 0.02F;
      vertex(vc, m, s, sx, al0, b0, -h0, u0, v, light, argb);
      vertex(vc, m, s, sx, al1, b1, -h1, u1, v, light, argb);
      vertex(vc, m, s, sx, al1, b1, h1, u1, v, light, argb);
      vertex(vc, m, s, sx, al0, b0, h0, u0, v, light, argb);
   }

   private static void point(BladeMesh.Shape s, float sx, float al, float cr, float th, float[] out) {
      out[0] = (s.o[0] + s.a[0] * al + s.c[0] * cr + s.t[0] * th) * sx;
      out[1] = s.o[1] + s.a[1] * al + s.c[1] * cr + s.t[1] * th;
      out[2] = s.o[2] + s.a[2] * al + s.c[2] * cr + s.t[2] * th;
   }

   private static void vertex(VertexConsumer vc, Matrix4f m, BladeMesh.Shape s, float sx, float al, float cr, float th, float u, float v, int light, int argb) {
      float x = (s.o[0] + s.a[0] * al + s.c[0] * cr + s.t[0] * th) * sx;
      float y = s.o[1] + s.a[1] * al + s.c[1] * cr + s.t[1] * th;
      float z = s.o[2] + s.a[2] * al + s.c[2] * cr + s.t[2] * th;
      m.transformPosition(x / 16.0F, y / 16.0F, z / 16.0F, P);
      vc.addVertex(P.x, P.y, P.z, argb, u, v, OverlayTexture.NO_OVERLAY, light, N.x, N.y, N.z);
   }

   static void edgeLine(VertexConsumer vc, Pose pose, BladeMesh.Shape s, float sx, float reach, int argb, float u, float v) {
      Matrix4f m = pose.pose();
      Matrix3f nm = pose.normal();
      int n = s.along.length;
      float limit = s.along[0] + (s.along[n - 1] - s.along[0]) * Mth.clamp(reach, 0.0F, 1.0F);
      boolean rising = s.along[n - 1] > s.along[0];
      float inset = 0.28F;

      for (int i = 0; i < n - 1; i++) {
         float al0 = s.along[i];
         float al1 = s.along[i + 1];
         if (rising ? al0 >= limit : al0 <= limit) {
            break;
         }

         al1 = rising ? Math.min(al1, limit) : Math.max(al1, limit);
         float e0 = s.edge[i];
         float e1 = s.edge[i + 1];
         float dir0 = Math.signum(e0 - s.back[i]);
         float dir1 = Math.signum(e1 - s.back[i + 1]);

         for (int side = -1; side <= 1; side += 2) {
            float th = 0.04F * (float)side;
            nm.transform(s.t[0] * (float)side * sx, s.t[1] * (float)side, s.t[2] * (float)side, N);
            vertex(vc, m, s, sx, al0, e0, th, u, v, 15728880, argb);
            vertex(vc, m, s, sx, al1, e1, th, u, v, 15728880, argb);
            vertex(vc, m, s, sx, al1, e1 - dir1 * inset, th, u, v, 15728880, argb & 16777215);
            vertex(vc, m, s, sx, al0, e0 - dir0 * inset, th, u, v, 15728880, argb & 16777215);
         }
      }
   }

   static void ring(
      VertexConsumer vc,
      Pose pose,
      float cx,
      float y,
      float rIn,
      float rOut,
      float thick,
      int teeth,
      float tilt,
      float angle,
      float sx,
      int light,
      int argb,
      int edgeArgb,
      float u,
      float v
   ) {
      Matrix4f m = pose.pose();
      Matrix3f nm = pose.normal();
      int seg = teeth * 2;
      float ct = Mth.cos(tilt);
      float st = Mth.sin(tilt);
      float h = thick * 0.5F;

      for (int i = 0; i < seg; i++) {
         float a0 = angle + (float) (Math.PI * 2) * (float)i / (float)seg;
         float a1 = angle + (float) (Math.PI * 2) * (float)(i + 1) / (float)seg;
         float t0 = (float)(i % 2) * 0.5F;
         float t1 = t0 + 0.5F;
         float o0 = rOut - 0.5F + 0.5F * t0;
         float o1 = rOut - 0.5F + 0.5F * t1;

         for (int side = -1; side <= 1; side += 2) {
            float dy = h * (float)side;
            ringVertex(vc, m, nm, cx, y, rIn, a0, dy, ct, st, sx, u, v, light, argb, 0.0F, (float)side, 0.0F);
            ringVertex(vc, m, nm, cx, y, o0, a0, dy, ct, st, sx, u, v, light, argb, 0.0F, (float)side, 0.0F);
            ringVertex(vc, m, nm, cx, y, o1, a1, dy, ct, st, sx, u, v, light, argb, 0.0F, (float)side, 0.0F);
            ringVertex(vc, m, nm, cx, y, rIn, a1, dy, ct, st, sx, u, v, light, argb, 0.0F, (float)side, 0.0F);
         }

         float am = (a0 + a1) * 0.5F;
         ringVertex(vc, m, nm, cx, y, o0, a0, -h, ct, st, sx, u, v, light, edgeArgb, Mth.cos(am), 0.0F, Mth.sin(am));
         ringVertex(vc, m, nm, cx, y, o1, a1, -h, ct, st, sx, u, v, light, edgeArgb, Mth.cos(am), 0.0F, Mth.sin(am));
         ringVertex(vc, m, nm, cx, y, o1, a1, h, ct, st, sx, u, v, light, edgeArgb, Mth.cos(am), 0.0F, Mth.sin(am));
         ringVertex(vc, m, nm, cx, y, o0, a0, h, ct, st, sx, u, v, light, edgeArgb, Mth.cos(am), 0.0F, Mth.sin(am));
         if (i % 2 == 1) {
            ringVertex(vc, m, nm, cx, y, rOut - 0.5F, a1, -h, ct, st, sx, u, v, light, argb, -Mth.sin(a1), 0.0F, Mth.cos(a1));
            ringVertex(vc, m, nm, cx, y, rOut, a1, -h, ct, st, sx, u, v, light, argb, -Mth.sin(a1), 0.0F, Mth.cos(a1));
            ringVertex(vc, m, nm, cx, y, rOut, a1, h, ct, st, sx, u, v, light, argb, -Mth.sin(a1), 0.0F, Mth.cos(a1));
            ringVertex(vc, m, nm, cx, y, rOut - 0.5F, a1, h, ct, st, sx, u, v, light, argb, -Mth.sin(a1), 0.0F, Mth.cos(a1));
         }

         ringVertex(vc, m, nm, cx, y, rIn, a0, -h, ct, st, sx, u, v, light, argb, -Mth.cos(am), 0.0F, -Mth.sin(am));
         ringVertex(vc, m, nm, cx, y, rIn, a1, -h, ct, st, sx, u, v, light, argb, -Mth.cos(am), 0.0F, -Mth.sin(am));
         ringVertex(vc, m, nm, cx, y, rIn, a1, h, ct, st, sx, u, v, light, argb, -Mth.cos(am), 0.0F, -Mth.sin(am));
         ringVertex(vc, m, nm, cx, y, rIn, a0, h, ct, st, sx, u, v, light, argb, -Mth.cos(am), 0.0F, -Mth.sin(am));
      }
   }

   static void ringBlur(VertexConsumer vc, Pose pose, float cx, float y, float r0, float r1, float tilt, float sx, int inner, int outer, float u, float v) {
      Matrix4f m = pose.pose();
      Matrix3f nm = pose.normal();
      float ct = Mth.cos(tilt);
      float st = Mth.sin(tilt);
      int seg = 24;

      for (int i = 0; i < seg; i++) {
         float a0 = (float) (Math.PI * 2) * (float)i / (float)seg;
         float a1 = (float) (Math.PI * 2) * (float)(i + 1) / (float)seg;
         ringVertex(vc, m, nm, cx, y, r0, a0, 0.0F, ct, st, sx, u, v, 15728880, inner, 0.0F, 1.0F, 0.0F);
         ringVertex(vc, m, nm, cx, y, r1, a0, 0.0F, ct, st, sx, u, v, 15728880, outer, 0.0F, 1.0F, 0.0F);
         ringVertex(vc, m, nm, cx, y, r1, a1, 0.0F, ct, st, sx, u, v, 15728880, outer, 0.0F, 1.0F, 0.0F);
         ringVertex(vc, m, nm, cx, y, r0, a1, 0.0F, ct, st, sx, u, v, 15728880, inner, 0.0F, 1.0F, 0.0F);
      }
   }

   private static void ringVertex(
      VertexConsumer vc,
      Matrix4f m,
      Matrix3f nm,
      float cx,
      float y,
      float r,
      float a,
      float dy,
      float ct,
      float st,
      float sx,
      float u,
      float v,
      int light,
      int argb,
      float nx,
      float ny,
      float nz
   ) {
      float lx = r * Mth.cos(a);
      float lz = r * Mth.sin(a);
      float py = dy * ct - lz * st;
      float pz = dy * st + lz * ct;
      float x = (cx + lx) * sx;
      float yy = y + py;
      m.transformPosition(x / 16.0F, yy / 16.0F, pz / 16.0F, P);
      float ny2 = ny * ct - nz * st;
      float nz2 = ny * st + nz * ct;
      nm.transform(nx * sx, ny2, nz2, N);
      vc.addVertex(P.x, P.y, P.z, argb, u, v, OverlayTexture.NO_OVERLAY, light, N.x, N.y, N.z);
   }

   static void sheen(VertexConsumer vc, Pose pose, BladeMesh.Shape s, float sx) {
      Matrix4f m = pose.pose();
      int n = s.along.length;

      for (int i = 0; i < n - 1; i++) {
         for (int side = -1; side <= 1; side += 2) {
            float h0 = (s.half != null ? s.half[i] : s.spine * 0.5F) * (float)side;
            float h1 = (s.half != null ? s.half[i + 1] : s.spine * 0.5F) * (float)side;
            sheenVertex(vc, m, s, sx, s.along[i], s.back[i], h0);
            sheenVertex(vc, m, s, sx, s.along[i + 1], s.back[i + 1], h1);
            sheenVertex(vc, m, s, sx, s.along[i + 1], s.edge[i + 1], 0.0F);
            sheenVertex(vc, m, s, sx, s.along[i], s.edge[i], 0.0F);
         }
      }
   }

   private static void sheenVertex(VertexConsumer vc, Matrix4f m, BladeMesh.Shape s, float sx, float al, float cr, float th) {
      float x = (s.o[0] + s.a[0] * al + s.c[0] * cr + s.t[0] * th) * sx;
      float y = s.o[1] + s.a[1] * al + s.c[1] * cr + s.t[1] * th;
      float z = s.o[2] + s.a[2] * al + s.c[2] * cr + s.t[2] * th;
      m.transformPosition(x / 16.0F, y / 16.0F, z / 16.0F, P);
      vc.addVertex(P.x, P.y, P.z).setUv(al * 0.045F + cr * 0.02F, cr * 0.05F - al * 0.012F);
   }

   private BladeMesh() {
   }

   interface Profile {
      void at(float var1, float[] var2);
   }

   public static final class Shape {
      final float[] along;
      final float[] back;
      final float[] edge;
      final float spine;
      final float ridge;
      final float[] o;
      final float[] a;
      final float[] c;
      final float[] t;
      final float[] half;
      final float length;

      Shape(
         float[] origin, float[] alongAx, float[] acrossAx, float[] thickAx, float[] along, float[] back, float[] edge, float[] half, float spine, float ridge
      ) {
         this.o = origin;
         this.a = alongAx;
         this.c = acrossAx;
         this.t = thickAx;
         this.along = along;
         this.back = back;
         this.edge = edge;
         this.half = half;
         this.spine = spine;
         this.ridge = ridge;
         this.length = Math.abs(along[along.length - 1] - along[0]);
      }

      public float root() {
         return this.along[0];
      }
   }
}
