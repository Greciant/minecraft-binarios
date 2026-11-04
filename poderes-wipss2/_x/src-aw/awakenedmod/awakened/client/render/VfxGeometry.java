package com.awakenedmod.awakened.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import org.joml.Vector3f;

final class VfxGeometry {
   private VfxGeometry() {
   }

   static int argb(int a, int r, int g, int b) {
      return clamp(a) << 24 | clamp(r) << 16 | clamp(g) << 8 | clamp(b);
   }

   private static int clamp(int v) {
      return v < 0 ? 0 : Math.min(255, v);
   }

   private static void vert(Pose pose, VertexConsumer vc, float x, float y, float z, int color) {
      vc.addVertex(pose, x, y, z).setColor(color);
   }

   private static void tri(Pose pose, VertexConsumer vc, Vector3f a, Vector3f b, Vector3f c, int color) {
      vert(pose, vc, a.x, a.y, a.z, color);
      vert(pose, vc, b.x, b.y, b.z, color);
      vert(pose, vc, c.x, c.y, c.z, color);
      vert(pose, vc, a.x, a.y, a.z, color);
      vert(pose, vc, c.x, c.y, c.z, color);
      vert(pose, vc, b.x, b.y, b.z, color);
   }

   private static void quad(Pose pose, VertexConsumer vc, Vector3f a, Vector3f b, Vector3f c, Vector3f d, int color) {
      tri(pose, vc, a, b, c, color);
      tri(pose, vc, a, c, d, color);
   }

   static void sphere(Pose pose, VertexConsumer vc, float cy, float radius, int rings, int segs, int color) {
      for (int i = 0; i < rings; i++) {
         double t0 = Math.PI * (double)i / (double)rings;
         double t1 = Math.PI * (double)(i + 1) / (double)rings;

         for (int j = 0; j < segs; j++) {
            double p0 = (Math.PI * 2) * (double)j / (double)segs;
            double p1 = (Math.PI * 2) * (double)(j + 1) / (double)segs;
            quad(pose, vc, onSphere(t0, p0, radius, cy), onSphere(t1, p0, radius, cy), onSphere(t1, p1, radius, cy), onSphere(t0, p1, radius, cy), color);
         }
      }
   }

   static void dome(Pose pose, VertexConsumer vc, float cy, float radius, int rings, int segs, float rough, int seed, int color) {
      for (int i = 0; i < rings; i++) {
         for (int j = 0; j < segs; j++) {
            quad(
               pose,
               vc,
               domePt(i, j, rings, segs, radius, cy, rough, seed),
               domePt(i + 1, j, rings, segs, radius, cy, rough, seed),
               domePt(i + 1, j + 1, rings, segs, radius, cy, rough, seed),
               domePt(i, j + 1, rings, segs, radius, cy, rough, seed),
               color
            );
         }
      }
   }

   private static Vector3f domePt(int i, int j, int rings, int segs, float radius, float cy, float rough, int seed) {
      double theta = (Math.PI / 2) * (double)i / (double)rings;
      double phi = (Math.PI * 2) * (double)(j % segs) / (double)segs;
      float f = 1.0F + rough * (noise(i, j % segs, seed) * 2.0F - 1.0F);
      double r = Math.sin(theta) * (double)radius * (double)f;
      return new Vector3f((float)(r * Math.cos(phi)), (float)(Math.cos(theta) * (double)radius * (double)f) + cy, (float)(r * Math.sin(phi)));
   }

   private static float noise(int a, int b, int c) {
      int h = a * 374761393 + b * 668265263 + c * 1442695040;
      h = (h ^ h >>> 13) * 1274126177;
      return (float)((h ^ h >>> 16) & 65535) / 65535.0F;
   }

   static void cap(Pose pose, VertexConsumer vc, float cy, float radius, int rings, int segs, float polarDeg, int color) {
      double maxTheta = Math.toRadians((double)Math.max(1.0F, polarDeg));

      for (int i = 0; i < rings; i++) {
         for (int j = 0; j < segs; j++) {
            quad(
               pose,
               vc,
               capPt(i, j, rings, segs, radius, cy, maxTheta),
               capPt(i + 1, j, rings, segs, radius, cy, maxTheta),
               capPt(i + 1, j + 1, rings, segs, radius, cy, maxTheta),
               capPt(i, j + 1, rings, segs, radius, cy, maxTheta),
               color
            );
         }
      }
   }

   private static Vector3f capPt(int i, int j, int rings, int segs, float radius, float cy, double maxTheta) {
      double theta = maxTheta * (double)i / (double)rings;
      double phi = (Math.PI * 2) * (double)(j % segs) / (double)segs;
      double r = Math.sin(theta) * (double)radius;
      return new Vector3f((float)(r * Math.cos(phi)), (float)(Math.cos(theta) * (double)radius) + cy, (float)(r * Math.sin(phi)));
   }

   static void domeGrid(
      Pose pose, VertexConsumer vc, float cy, float radius, int latLines, int lonLines, int ringSegs, float lineHalfWidthRad, float polarDeg, int color
   ) {
      double polar = Math.toRadians((double)Math.max(1.0F, polarDeg));
      float hw = Math.max(0.0F, lineHalfWidthRad);
      int merSegs = Math.max(6, ringSegs / 5);

      for (int k = 1; k <= latLines; k++) {
         double theta = polar * (double)k / (double)(latLines + 1);
         double t0 = Math.max(0.0, theta - (double)hw);
         double t1 = Math.min(polar, theta + (double)hw);

         for (int j = 0; j < ringSegs; j++) {
            double p0 = (Math.PI * 2) * (double)j / (double)ringSegs;
            double p1 = (Math.PI * 2) * (double)(j + 1) / (double)ringSegs;
            quad(pose, vc, onSphere(t0, p0, radius, cy), onSphere(t1, p0, radius, cy), onSphere(t1, p1, radius, cy), onSphere(t0, p1, radius, cy), color);
         }
      }

      for (int m = 0; m < lonLines; m++) {
         double phi = (Math.PI * 2) * (double)m / (double)lonLines;
         double q0 = phi - (double)hw;
         double q1 = phi + (double)hw;

         for (int i = 0; i < merSegs; i++) {
            double a0 = polar * (double)i / (double)merSegs;
            double a1 = polar * (double)(i + 1) / (double)merSegs;
            quad(pose, vc, onSphere(a0, q0, radius, cy), onSphere(a1, q0, radius, cy), onSphere(a1, q1, radius, cy), onSphere(a0, q1, radius, cy), color);
         }
      }
   }

   static void crescent(Pose pose, VertexConsumer vc, float radius, float maxThick, float arcDeg, int segs, int color) {
      crescentBand(pose, vc, radius, maxThick, arcDeg, segs, 0.0F, -0.5F, 0.5F, color);
   }

   static void crescentBand(Pose pose, VertexConsumer vc, float radius, float maxThick, float arcDeg, int segs, float sweep, float from, float to, int color) {
      double arc = Math.toRadians((double)arcDeg);
      double start = (Math.PI / 2) - arc / 2.0;

      for (int j = 0; j < segs; j++) {
         double f0 = (double)j / (double)segs;
         double f1 = (double)(j + 1) / (double)segs;
         double a0 = start + arc * f0;
         double a1 = start + arc * f1;
         float t0 = (float)((double)maxThick * Math.sin(f0 * Math.PI));
         float t1 = (float)((double)maxThick * Math.sin(f1 * Math.PI));
         Vector3f in0 = crescentPt(a0, radius + t0 * from, sweep);
         Vector3f out0 = crescentPt(a0, radius + t0 * to, sweep);
         Vector3f in1 = crescentPt(a1, radius + t1 * from, sweep);
         Vector3f out1 = crescentPt(a1, radius + t1 * to, sweep);
         quad(pose, vc, in0, out0, out1, in1, color);
      }
   }

   static void crescentBlade(Pose pose, VertexConsumer vc, float radius, float width, float depth, float arcDeg, int segs, int edgeColor, int bodyColor) {
      double arc = Math.toRadians((double)arcDeg);
      double start = (Math.PI / 2) - arc / 2.0;
      Vector3f[] prev = null;

      for (int j = 0; j <= segs; j++) {
         double f = (double)j / (double)segs;
         double a = start + arc * f;
         float t = (float)Math.sin(f * Math.PI);
         float c = (float)Math.cos(a);
         float s = (float)Math.sin(a);
         float rEdge = radius + width * t * 0.5F;
         float rBack = radius - width * t * 0.5F;
         float rRidge = radius + width * t * 0.15F;
         Vector3f[] ring = new Vector3f[]{
            new Vector3f(c * rEdge, s * rEdge, 0.0F),
            new Vector3f(c * rRidge, s * rRidge, depth * t),
            new Vector3f(c * rBack, s * rBack, 0.0F),
            new Vector3f(c * rRidge, s * rRidge, -depth * t)
         };
         if (prev != null) {
            for (int k = 0; k < 4; k++) {
               int n = (k + 1) % 4;
               quad(pose, vc, prev[k], ring[k], ring[n], prev[n], k != 0 && k != 3 ? bodyColor : edgeColor);
            }
         }

         prev = ring;
      }
   }

   private static Vector3f crescentPt(double ang, float r, float sweep) {
      return new Vector3f((float)(Math.cos(ang) * (double)r), (float)(Math.sin(ang) * (double)r), (float)((double)(-sweep) * (1.0 - Math.sin(ang))));
   }

   static void ring(Pose pose, VertexConsumer vc, float y, float rInner, float rOuter, int segs, int color) {
      for (int j = 0; j < segs; j++) {
         double p0 = (Math.PI * 2) * (double)j / (double)segs;
         double p1 = (Math.PI * 2) * (double)(j + 1) / (double)segs;
         Vector3f in0 = new Vector3f((float)(Math.cos(p0) * (double)rInner), y, (float)(Math.sin(p0) * (double)rInner));
         Vector3f out0 = new Vector3f((float)(Math.cos(p0) * (double)rOuter), y, (float)(Math.sin(p0) * (double)rOuter));
         Vector3f in1 = new Vector3f((float)(Math.cos(p1) * (double)rInner), y, (float)(Math.sin(p1) * (double)rInner));
         Vector3f out1 = new Vector3f((float)(Math.cos(p1) * (double)rOuter), y, (float)(Math.sin(p1) * (double)rOuter));
         quad(pose, vc, in0, out0, out1, in1, color);
      }
   }

   static void ring(Pose pose, VertexConsumer vc, float y, float rInner, float rOuter, int segs, float rough, int seed, int color) {
      for (int j = 0; j < segs; j++) {
         double p0 = (Math.PI * 2) * (double)j / (double)segs;
         double p1 = (Math.PI * 2) * (double)((j + 1) % segs) / (double)segs;
         float fo0 = 1.0F + rough * (noise(0, j % segs, seed) * 2.0F - 1.0F);
         float fo1 = 1.0F + rough * (noise(0, (j + 1) % segs, seed) * 2.0F - 1.0F);
         float fi0 = 1.0F + rough * 0.5F * (noise(1, j % segs, seed) * 2.0F - 1.0F);
         float fi1 = 1.0F + rough * 0.5F * (noise(1, (j + 1) % segs, seed) * 2.0F - 1.0F);
         Vector3f in0 = new Vector3f((float)(Math.cos(p0) * (double)rInner * (double)fi0), y, (float)(Math.sin(p0) * (double)rInner * (double)fi0));
         Vector3f out0 = new Vector3f((float)(Math.cos(p0) * (double)rOuter * (double)fo0), y, (float)(Math.sin(p0) * (double)rOuter * (double)fo0));
         Vector3f in1 = new Vector3f((float)(Math.cos(p1) * (double)rInner * (double)fi1), y, (float)(Math.sin(p1) * (double)rInner * (double)fi1));
         Vector3f out1 = new Vector3f((float)(Math.cos(p1) * (double)rOuter * (double)fo1), y, (float)(Math.sin(p1) * (double)rOuter * (double)fo1));
         quad(pose, vc, in0, out0, out1, in1, color);
      }
   }

   static void cracks(Pose pose, VertexConsumer vc, float y, float rIn, float rOut, int count, int seed, float width, int color) {
      for (int i = 0; i < count; i++) {
         double a0 = (Math.PI * 2) * (double)i / (double)count + (double)(noise(seed, i, 41) - 0.5F) * 0.9;
         float len = rIn + (rOut - rIn) * (0.45F + 0.55F * noise(seed, i, 42));
         float mid = rIn + (len - rIn) * (0.35F + 0.3F * noise(seed, i, 43));
         double a1 = a0 + (double)(noise(seed, i, 44) - 0.5F) * 0.5;
         double a2 = a1 + (double)(noise(seed, i, 45) - 0.5F) * 0.6;
         Vector3f p0 = new Vector3f((float)(Math.cos(a0) * (double)rIn), y, (float)(Math.sin(a0) * (double)rIn));
         Vector3f p1 = new Vector3f((float)(Math.cos(a1) * (double)mid), y, (float)(Math.sin(a1) * (double)mid));
         Vector3f p2 = new Vector3f((float)(Math.cos(a2) * (double)len), y, (float)(Math.sin(a2) * (double)len));
         crackSeg(pose, vc, p0, p1, width, width * 0.45F, color);
         crackSeg(pose, vc, p1, p2, width * 0.45F, 0.0F, color);
      }
   }

   static void crackSeg(Pose pose, VertexConsumer vc, Vector3f a, Vector3f b, float w0, float w1, int color) {
      float dx = b.x - a.x;
      float dz = b.z - a.z;
      float len = (float)Math.sqrt((double)(dx * dx + dz * dz));
      if (!(len < 1.0E-5F)) {
         float px = -dz / len;
         float pz = dx / len;
         quad(
            pose,
            vc,
            new Vector3f(a.x + px * w0, a.y, a.z + pz * w0),
            new Vector3f(b.x + px * w1, b.y, b.z + pz * w1),
            new Vector3f(b.x - px * w1, b.y, b.z - pz * w1),
            new Vector3f(a.x - px * w0, a.y, a.z - pz * w0),
            color
         );
      }
   }

   static void disc(Pose pose, VertexConsumer vc, float y, float radius, int segs, int color) {
      Vector3f center = new Vector3f(0.0F, y, 0.0F);

      for (int j = 0; j < segs; j++) {
         double p0 = (Math.PI * 2) * (double)j / (double)segs;
         double p1 = (Math.PI * 2) * (double)(j + 1) / (double)segs;
         Vector3f a = new Vector3f((float)(Math.cos(p0) * (double)radius), y, (float)(Math.sin(p0) * (double)radius));
         Vector3f b = new Vector3f((float)(Math.cos(p1) * (double)radius), y, (float)(Math.sin(p1) * (double)radius));
         tri(pose, vc, center, a, b, color);
      }
   }

   static void box(Pose pose, VertexConsumer vc, float x0, float y0, float z0, float x1, float y1, float z1, int color) {
      Vector3f a = new Vector3f(x0, y0, z0);
      Vector3f b = new Vector3f(x1, y0, z0);
      Vector3f c = new Vector3f(x1, y0, z1);
      Vector3f d = new Vector3f(x0, y0, z1);
      Vector3f e = new Vector3f(x0, y1, z0);
      Vector3f f = new Vector3f(x1, y1, z0);
      Vector3f g = new Vector3f(x1, y1, z1);
      Vector3f h = new Vector3f(x0, y1, z1);
      quad(pose, vc, a, b, c, d, color);
      quad(pose, vc, e, f, g, h, color);
      quad(pose, vc, a, b, f, e, color);
      quad(pose, vc, d, c, g, h, color);
      quad(pose, vc, a, d, h, e, color);
      quad(pose, vc, b, c, g, f, color);
   }

   static void wallPiece(Pose pose, VertexConsumer vc, double a0, double a1, float rTop, float yTop, float rBot, float yBot, int color) {
      float c0 = (float)Math.cos(a0);
      float s0 = (float)Math.sin(a0);
      float c1 = (float)Math.cos(a1);
      float s1 = (float)Math.sin(a1);
      quad(
         pose,
         vc,
         new Vector3f(c0 * rTop, yTop, s0 * rTop),
         new Vector3f(c1 * rTop, yTop, s1 * rTop),
         new Vector3f(c1 * rBot, yBot, s1 * rBot),
         new Vector3f(c0 * rBot, yBot, s0 * rBot),
         color
      );
   }

   static void cylinder(Pose pose, VertexConsumer vc, float y0, float r0, float y1, float r1, int segs, float rough, int seed, int color) {
      for (int j = 0; j < segs; j++) {
         double p0 = (Math.PI * 2) * (double)j / (double)segs;
         double p1 = (Math.PI * 2) * (double)((j + 1) % segs) / (double)segs;
         float f0 = 1.0F + rough * (noise(0, j % segs, seed) * 2.0F - 1.0F);
         float f1 = 1.0F + rough * (noise(0, (j + 1) % segs, seed) * 2.0F - 1.0F);
         Vector3f b0 = new Vector3f((float)(Math.cos(p0) * (double)r0 * (double)f0), y0, (float)(Math.sin(p0) * (double)r0 * (double)f0));
         Vector3f b1 = new Vector3f((float)(Math.cos(p1) * (double)r0 * (double)f1), y0, (float)(Math.sin(p1) * (double)r0 * (double)f1));
         Vector3f t0 = new Vector3f((float)(Math.cos(p0) * (double)r1 * (double)f0), y1, (float)(Math.sin(p0) * (double)r1 * (double)f0));
         Vector3f t1 = new Vector3f((float)(Math.cos(p1) * (double)r1 * (double)f1), y1, (float)(Math.sin(p1) * (double)r1 * (double)f1));
         quad(pose, vc, b0, t0, t1, b1, color);
      }
   }

   static void helixStrip(
      Pose pose,
      VertexConsumer vc,
      float y0,
      float r0,
      float y1,
      float r1,
      float taper,
      float turns,
      int steps,
      float halfWidth,
      float startAng,
      float rough,
      int seed,
      int color
   ) {
      Vector3f prevLo = null;
      Vector3f prevHi = null;

      for (int i = 0; i <= steps; i++) {
         float t = (float)i / (float)steps;
         double ang = (double)startAng + (Math.PI * 2) * (double)turns * (double)t;
         float r = (r0 + (r1 - r0) * (float)Math.pow((double)t, (double)taper)) * (1.0F + rough * (noise(i, seed, 3) * 2.0F - 1.0F));
         float w = halfWidth * (0.55F + 0.9F * noise(i, seed, 7));
         float y = y0 + (y1 - y0) * t;
         float x = (float)(Math.cos(ang) * (double)r);
         float z = (float)(Math.sin(ang) * (double)r);
         Vector3f lo = new Vector3f(x, y - w, z);
         Vector3f hi = new Vector3f(x, y + w, z);
         if (prevLo != null) {
            quad(pose, vc, prevLo, prevHi, hi, lo, color);
         }

         prevLo = lo;
         prevHi = hi;
      }
   }

   static void beam(Pose pose, VertexConsumer vc, Vector3f end, float w, int color) {
      Vector3f axis = new Vector3f(end);
      if (!(axis.lengthSquared() < 1.0E-6F)) {
         axis.normalize();
         Vector3f u = new Vector3f(0.0F, 1.0F, 0.0F);
         if (Math.abs(axis.y) > 0.95F) {
            u.set(1.0F, 0.0F, 0.0F);
         }

         Vector3f perp1 = new Vector3f(axis).cross(u).normalize().mul(w);
         Vector3f perp2 = new Vector3f(axis).cross(perp1).normalize().mul(w);
         crossPlane(pose, vc, end, perp1, color);
         crossPlane(pose, vc, end, perp2, color);
      }
   }

   private static void crossPlane(Pose pose, VertexConsumer vc, Vector3f end, Vector3f perp, int color) {
      Vector3f s0 = new Vector3f(perp);
      Vector3f s1 = new Vector3f(perp).negate();
      Vector3f e0 = new Vector3f(end).add(perp);
      Vector3f e1 = new Vector3f(end).sub(perp);
      quad(pose, vc, s0, e0, e1, s1, color);
   }

   static void arc(Pose pose, VertexConsumer vc, Vector3f end, float w, int segs, float jitter, int seed, int color) {
      float len = end.length();
      if (!(len < 1.0E-4F) && segs >= 1) {
         Vector3f axis = new Vector3f(end).div(len);
         Vector3f u = Math.abs(axis.y) > 0.95F ? new Vector3f(1.0F, 0.0F, 0.0F) : new Vector3f(0.0F, 1.0F, 0.0F);
         Vector3f perp1 = new Vector3f(axis).cross(u).normalize();
         Vector3f perp2 = new Vector3f(axis).cross(perp1).normalize();
         float amp = jitter * len;
         Vector3f[] pts = new Vector3f[segs + 1];

         for (int i = 0; i <= segs; i++) {
            float t = (float)i / (float)segs;
            Vector3f p = new Vector3f(end).mul(t);
            if (i > 0 && i < segs) {
               float taper = (float)Math.sin((double)t * Math.PI);
               float jx = (noise(i, 0, seed) * 2.0F - 1.0F) * amp * taper;
               float jy = (noise(i, 1, seed) * 2.0F - 1.0F) * amp * taper;
               p.add(new Vector3f(perp1).mul(jx)).add(new Vector3f(perp2).mul(jy));
            }

            pts[i] = p;
         }

         Vector3f q1 = new Vector3f(perp1).mul(w);
         Vector3f q2 = new Vector3f(perp2).mul(w);

         for (int i = 0; i < segs; i++) {
            segmentQuad(pose, vc, pts[i], pts[i + 1], q1, color);
            segmentQuad(pose, vc, pts[i], pts[i + 1], q2, color);
         }
      }
   }

   static void helix(
      Pose pose, VertexConsumer vc, float radius, float height, float turns, float startAngle, float growFrac, float w, int segsPerTurn, int color
   ) {
      int total = Math.max(4, Math.round((float)segsPerTurn * turns));
      int drawn = Math.max(1, Math.round((float)total * Math.min(1.0F, growFrac)));
      Vector3f up = new Vector3f(0.0F, w, 0.0F);
      Vector3f prev = helixPt(0, total, radius, height, turns, startAngle);

      for (int i = 1; i <= drawn; i++) {
         Vector3f next = helixPt(i, total, radius, height, turns, startAngle);
         double mid = (double)startAngle + (double)(turns * 2.0F) * Math.PI * (double)((float)i - 0.5F) / (double)total;
         Vector3f radial = new Vector3f((float)Math.cos(mid), 0.0F, (float)Math.sin(mid)).mul(w);
         segmentQuad(pose, vc, prev, next, radial, color);
         segmentQuad(pose, vc, prev, next, up, color);
         prev = next;
      }
   }

   private static Vector3f helixPt(int i, int total, float radius, float height, float turns, float start) {
      float t = (float)i / (float)total;
      double ang = (double)start + (double)(turns * 2.0F) * Math.PI * (double)t;
      return new Vector3f((float)(Math.cos(ang) * (double)radius), height * t, (float)(Math.sin(ang) * (double)radius));
   }

   private static void segmentQuad(Pose pose, VertexConsumer vc, Vector3f a, Vector3f b, Vector3f q, int color) {
      Vector3f a0 = new Vector3f(a).add(q);
      Vector3f a1 = new Vector3f(a).sub(q);
      Vector3f b0 = new Vector3f(b).add(q);
      Vector3f b1 = new Vector3f(b).sub(q);
      quad(pose, vc, a0, b0, b1, a1, color);
   }

   static void annulusXY(Pose pose, VertexConsumer vc, float z, float rInner, float rOuter, int segs, float ellipse, int color) {
      annulusXY(pose, vc, z, rInner, rOuter, segs, ellipse, 0.0F, 0.0F, color, color);
   }

   static void annulusXY(
      Pose pose, VertexConsumer vc, float z, float rInner, float rOuter, int segs, float ellipse, float wobAmp, float wobPhase, int colorIn, int colorOut
   ) {
      for (int j = 0; j < segs; j++) {
         double p0 = (Math.PI * 2) * (double)j / (double)segs;
         double p1 = (Math.PI * 2) * (double)(j + 1) / (double)segs;
         float f0 = wobble(p0, wobAmp, wobPhase);
         float f1 = wobble(p1, wobAmp, wobPhase);
         Vector3f in0 = new Vector3f(
            (float)(Math.cos(p0) * (double)rInner * (double)f0), (float)(Math.sin(p0) * (double)rInner * (double)f0 * (double)ellipse), z
         );
         Vector3f out0 = new Vector3f(
            (float)(Math.cos(p0) * (double)rOuter * (double)f0), (float)(Math.sin(p0) * (double)rOuter * (double)f0 * (double)ellipse), z
         );
         Vector3f in1 = new Vector3f(
            (float)(Math.cos(p1) * (double)rInner * (double)f1), (float)(Math.sin(p1) * (double)rInner * (double)f1 * (double)ellipse), z
         );
         Vector3f out1 = new Vector3f(
            (float)(Math.cos(p1) * (double)rOuter * (double)f1), (float)(Math.sin(p1) * (double)rOuter * (double)f1 * (double)ellipse), z
         );
         gradQuad(pose, vc, in0, colorIn, out0, colorOut, out1, colorOut, in1, colorIn);
      }
   }

   private static float wobble(double ang, float amp, float phase) {
      return amp <= 0.0F
         ? 1.0F
         : 1.0F + amp * (0.6F * (float)Math.sin(3.0 * ang + (double)phase) + 0.4F * (float)Math.sin(5.0 * ang - (double)(0.7F * phase) + 1.3F));
   }

   private static void gradQuad(Pose pose, VertexConsumer vc, Vector3f a, int ca, Vector3f b, int cb, Vector3f c, int cc, Vector3f d, int cd) {
      cvert(pose, vc, a.x, a.y, a.z, ca);
      cvert(pose, vc, b.x, b.y, b.z, cb);
      cvert(pose, vc, c.x, c.y, c.z, cc);
      cvert(pose, vc, a.x, a.y, a.z, ca);
      cvert(pose, vc, c.x, c.y, c.z, cc);
      cvert(pose, vc, b.x, b.y, b.z, cb);
      cvert(pose, vc, a.x, a.y, a.z, ca);
      cvert(pose, vc, c.x, c.y, c.z, cc);
      cvert(pose, vc, d.x, d.y, d.z, cd);
      cvert(pose, vc, a.x, a.y, a.z, ca);
      cvert(pose, vc, d.x, d.y, d.z, cd);
      cvert(pose, vc, c.x, c.y, c.z, cc);
   }

   static void spiralXY(
      Pose pose,
      VertexConsumer vc,
      float z,
      float rInner,
      float rOuter,
      float startRad,
      float sweepRad,
      float halfWidth,
      int segs,
      float ellipse,
      int colorIn,
      int colorOut
   ) {
      for (int i = 0; i < segs; i++) {
         float t0 = (float)i / (float)segs;
         float t1 = (float)(i + 1) / (float)segs;
         int c0 = argbMix(colorIn, colorOut, t0);
         int c1 = argbMix(colorIn, colorOut, t1);
         float w0 = halfWidth * (0.35F + 0.65F * t0);
         float w1 = halfWidth * (0.35F + 0.65F * t1);
         Vector3f in0 = spiralPt(t0, rInner, rOuter, startRad, sweepRad, -w0, ellipse, z);
         Vector3f out0 = spiralPt(t0, rInner, rOuter, startRad, sweepRad, w0, ellipse, z);
         Vector3f in1 = spiralPt(t1, rInner, rOuter, startRad, sweepRad, -w1, ellipse, z);
         Vector3f out1 = spiralPt(t1, rInner, rOuter, startRad, sweepRad, w1, ellipse, z);
         gradQuad(pose, vc, in0, c0, out0, c0, out1, c1, in1, c1);
      }
   }

   private static Vector3f spiralPt(float t, float rInner, float rOuter, float start, float sweep, float wOff, float ellipse, float z) {
      float ang = start + sweep * t;
      float r = rInner + (rOuter - rInner) * (float)Math.pow((double)t, 0.85) + wOff;
      return new Vector3f((float)Math.cos((double)ang) * r, (float)Math.sin((double)ang) * r * ellipse, z);
   }

   private static int argbMix(int a, int b, float t) {
      int aa = a >>> 24;
      int ar = a >> 16 & 0xFF;
      int ag = a >> 8 & 0xFF;
      int ab = a & 0xFF;
      int ba = b >>> 24;
      int br = b >> 16 & 0xFF;
      int bg = b >> 8 & 0xFF;
      int bb = b & 0xFF;
      return aa + (int)((float)(ba - aa) * t) << 24
         | ar + (int)((float)(br - ar) * t) << 16
         | ag + (int)((float)(bg - ag) * t) << 8
         | ab + (int)((float)(bb - ab) * t);
   }

   private static Vector3f onSphere(double theta, double phi, float radius, float cy) {
      double r = Math.sin(theta);
      return new Vector3f((float)(r * Math.cos(phi)), (float)Math.cos(theta), (float)(r * Math.sin(phi))).mul(radius).add(0.0F, cy, 0.0F);
   }

   static float noise01(int a, int b, int c) {
      return noise(a, b, c);
   }

   static void flameRibbon(
      Pose pose,
      VertexConsumer vc,
      float baseX,
      float baseZ,
      float baseY,
      float height,
      float baseW,
      int segs,
      float sway,
      float phase,
      float leanFrac,
      int bodyColor,
      int coreColor,
      int alpha
   ) {
      if (segs >= 1 && !(height <= 0.0F) && alpha > 0) {
         float rLen = (float)Math.sqrt((double)(baseX * baseX + baseZ * baseZ));
         float dirX = rLen > 1.0E-4F ? baseX / rLen : 1.0F;
         float dirZ = rLen > 1.0E-4F ? baseZ / rLen : 0.0F;
         float tanX = -dirZ;
         float tanZ = dirX;
         Vector3f[] pts = new Vector3f[segs + 1];
         float[] widths = new float[segs + 1];
         int[] cols = new int[segs + 1];

         for (int i = 0; i <= segs; i++) {
            float f = (float)i / (float)segs;
            float lean = 1.0F - leanFrac * f * f;
            float y = baseY + height * (float)Math.sin((double)f * (Math.PI / 2));
            float w1 = (float)Math.sin((double)(f * 6.5F + phase * 2.2F));
            float w2 = (float)Math.sin((double)(f * 11.0F - phase * 1.3F + 1.7F));
            float licks = (0.65F * w1 + 0.35F * w2) * sway * (0.35F + f);
            float px = baseX * lean + dirX * licks * 0.5F + tanX * licks;
            float pz = baseZ * lean + dirZ * licks * 0.5F + tanZ * licks;
            pts[i] = new Vector3f(px, y, pz);
            widths[i] = baseW * (1.0F - f) * (1.0F - f);
            cols[i] = argbLerp(coreColor, bodyColor, f, alpha);
         }

         for (int i = 0; i < segs; i++) {
            ribbonSeg(pose, vc, pts[i], pts[i + 1], widths[i], widths[i + 1], tanX, 0.0F, tanZ, cols[i], cols[i + 1]);
            ribbonSeg(pose, vc, pts[i], pts[i + 1], widths[i], widths[i + 1], dirX, 0.0F, dirZ, cols[i], cols[i + 1]);
         }
      }
   }

   private static void ribbonSeg(Pose pose, VertexConsumer vc, Vector3f a, Vector3f b, float wa, float wb, float ax, float ay, float az, int colA, int colB) {
      float a0x = a.x + ax * wa;
      float a0y = a.y + ay * wa;
      float a0z = a.z + az * wa;
      float a1x = a.x - ax * wa;
      float a1y = a.y - ay * wa;
      float a1z = a.z - az * wa;
      float b0x = b.x + ax * wb;
      float b0y = b.y + ay * wb;
      float b0z = b.z + az * wb;
      float b1x = b.x - ax * wb;
      float b1y = b.y - ay * wb;
      float b1z = b.z - az * wb;
      cvert(pose, vc, a0x, a0y, a0z, colA);
      cvert(pose, vc, b0x, b0y, b0z, colB);
      cvert(pose, vc, b1x, b1y, b1z, colB);
      cvert(pose, vc, a0x, a0y, a0z, colA);
      cvert(pose, vc, b1x, b1y, b1z, colB);
      cvert(pose, vc, b0x, b0y, b0z, colB);
      cvert(pose, vc, a0x, a0y, a0z, colA);
      cvert(pose, vc, b1x, b1y, b1z, colB);
      cvert(pose, vc, a1x, a1y, a1z, colA);
      cvert(pose, vc, a0x, a0y, a0z, colA);
      cvert(pose, vc, a1x, a1y, a1z, colA);
      cvert(pose, vc, b1x, b1y, b1z, colB);
   }

   private static void cvert(Pose pose, VertexConsumer vc, float x, float y, float z, int color) {
      vc.addVertex(pose, x, y, z).setColor(color);
   }

   private static int argbLerp(int from, int to, float t, int alpha) {
      int fr = from >> 16 & 0xFF;
      int fg = from >> 8 & 0xFF;
      int fb = from & 0xFF;
      int tr = to >> 16 & 0xFF;
      int tg = to >> 8 & 0xFF;
      int tb = to & 0xFF;
      int r = fr + (int)((float)(tr - fr) * t);
      int g = fg + (int)((float)(tg - fg) * t);
      int b = fb + (int)((float)(tb - fb) * t);
      return argb(alpha, r, g, b);
   }
}
