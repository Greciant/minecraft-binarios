package com.awakenedmod.awakened.client.graviton;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class GravitonDraw {
   private GravitonDraw() {
   }

   public static int argb(float alpha, int rgb) {
      int a = Mth.clamp(Math.round(alpha * 255.0F), 0, 255);
      return a << 24 | rgb & 16777215;
   }

   public static void tri(Pose pose, VertexConsumer vc, Vec3 a, int ca, Vec3 b, int cb, Vec3 c, int cc) {
      vc.addVertex(pose, (float)a.x, (float)a.y, (float)a.z).setColor(ca);
      vc.addVertex(pose, (float)b.x, (float)b.y, (float)b.z).setColor(cb);
      vc.addVertex(pose, (float)c.x, (float)c.y, (float)c.z).setColor(cc);
   }

   public static void quad(Pose pose, VertexConsumer vc, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int c0, int c1, boolean bothSides) {
      tri(pose, vc, a, c0, b, c1, c, c1);
      tri(pose, vc, a, c0, c, c1, d, c0);
      if (bothSides) {
         tri(pose, vc, a, c0, c, c1, b, c1);
         tri(pose, vc, a, c0, d, c0, c, c1);
      }
   }

   public static void ribbon(
      Pose pose,
      VertexConsumer vc,
      Vec3[] pts,
      float[] fade,
      Vec3 cam,
      double inner,
      double outer,
      float alphaIn,
      float alphaOut,
      int rgbIn,
      int rgbOut,
      boolean bothSides
   ) {
      Vec3[] side = new Vec3[pts.length];

      for (int i = 0; i < pts.length; i++) {
         Vec3 t = pts[Math.min(i + 1, pts.length - 1)].subtract(pts[Math.max(i - 1, 0)]);
         Vec3 s = t.cross(cam.subtract(pts[i]));
         side[i] = s.lengthSqr() < 1.0E-9 ? new Vec3(0.0, 1.0, 0.0) : s.normalize();
      }

      for (int i = 0; i + 1 < pts.length; i++) {
         int ci0 = argb(alphaIn * fade[i], rgbIn);
         int ci1 = argb(alphaIn * fade[i + 1], rgbIn);
         int co0 = argb(alphaOut * fade[i], rgbOut);
         int co1 = argb(alphaOut * fade[i + 1], rgbOut);

         for (int sign = -1; sign <= 1; sign += 2) {
            Vec3 i0 = pts[i].add(side[i].scale(inner * (double)sign));
            Vec3 i1 = pts[i + 1].add(side[i + 1].scale(inner * (double)sign));
            Vec3 o0 = pts[i].add(side[i].scale(outer * (double)sign));
            Vec3 o1 = pts[i + 1].add(side[i + 1].scale(outer * (double)sign));
            tri(pose, vc, i0, ci0, i1, ci1, o1, co1);
            tri(pose, vc, i0, ci0, o1, co1, o0, co0);
            if (bothSides) {
               tri(pose, vc, i0, ci0, o1, co1, i1, ci1);
               tri(pose, vc, i0, ci0, o0, co0, o1, co1);
            }
         }
      }
   }

   public static void ribbonLoop(
      Pose pose, VertexConsumer vc, Vec3[] pts, float[] fade, Vec3 cam, double half, float alpha, float edge, int rgb, boolean bothSides
   ) {
      int n = pts.length;
      Vec3[] side = new Vec3[n];

      for (int i = 0; i < n; i++) {
         Vec3 t = pts[(i + 1) % n].subtract(pts[(i + n - 1) % n]);
         Vec3 s = t.cross(cam.subtract(pts[i]));
         side[i] = s.lengthSqr() < 1.0E-12 ? new Vec3(0.0, 1.0, 0.0) : s.normalize();
      }

      for (int i = 0; i < n; i++) {
         int j = (i + 1) % n;
         int c0 = argb(alpha * fade[i], rgb);
         int c1 = argb(alpha * fade[j], rgb);
         int e0 = argb(alpha * fade[i] * edge, rgb);
         int e1 = argb(alpha * fade[j] * edge, rgb);

         for (int sign = -1; sign <= 1; sign += 2) {
            Vec3 o0 = pts[i].add(side[i].scale(half * (double)sign));
            Vec3 o1 = pts[j].add(side[j].scale(half * (double)sign));
            tri(pose, vc, pts[i], c0, pts[j], c1, o1, e1);
            tri(pose, vc, pts[i], c0, o1, e1, o0, e0);
            if (bothSides) {
               tri(pose, vc, pts[i], c0, o1, e1, pts[j], c1);
               tri(pose, vc, pts[i], c0, o0, e0, o1, e1);
            }
         }
      }
   }

   public static void bracket(
      Pose pose, VertexConsumer vc, Vec3 c, Vec3 right, Vec3 up, double hw, double hh, double arm, double thick, int color, boolean bothSides
   ) {
      for (int sx = -1; sx <= 1; sx += 2) {
         for (int sy = -1; sy <= 1; sy += 2) {
            Vec3 corner = c.add(right.scale(hw * (double)sx)).add(up.scale(hh * (double)sy));
            Vec3 inX = right.scale((double)(-sx));
            Vec3 inY = up.scale((double)(-sy));
            bar(pose, vc, corner, corner.add(inX.scale(arm)), inY, thick, color, bothSides);
            bar(pose, vc, corner.add(inY.scale(thick)), corner.add(inY.scale(arm)), inX, thick, color, bothSides);
         }
      }
   }

   private static void bar(Pose pose, VertexConsumer vc, Vec3 a, Vec3 b, Vec3 inward, double thick, int color, boolean bothSides) {
      Vec3 off = inward.scale(thick);
      quad(pose, vc, a, b, b.add(off), a.add(off), color, color, bothSides);
   }
}
