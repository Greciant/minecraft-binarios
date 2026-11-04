package com.awakenedmod.awakened.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

final class VelocityStreaks {
   private static final float COMET_BODY_FRAC = 0.28F;
   private static final float COMET_HEAD_TIP_FRAC = 0.35F;
   private static final float COMET_TAPER_FRAC = 0.6F;
   private static final float COMET_TAPER_WIDTH_FRAC = 0.55F;
   private static final Vector3f UP = new Vector3f(0.0F, 1.0F, 0.0F);
   private static final Vector3f RIGHT = new Vector3f(1.0F, 0.0F, 0.0F);

   private VelocityStreaks() {
   }

   static int lerp(int a, int b, float t) {
      float f = Mth.clamp(t, 0.0F, 1.0F);
      int aa = a >>> 24;
      int ba = b >>> 24;
      return aa + (int)((float)(ba - aa) * f) << 24 | lerpRgb(a, b, f);
   }

   static int lerpRgb(int a, int b, float t) {
      float f = Mth.clamp(t, 0.0F, 1.0F);
      int ar = a >> 16 & 0xFF;
      int ag = a >> 8 & 0xFF;
      int ab = a & 0xFF;
      int br = b >> 16 & 0xFF;
      int bg = b >> 8 & 0xFF;
      int bb = b & 0xFF;
      int r = ar + (int)((float)(br - ar) * f);
      int g = ag + (int)((float)(bg - ag) * f);
      int bl = ab + (int)((float)(bb - ab) * f);
      return r << 16 | g << 8 | bl;
   }

   static int fade(int argb, float f) {
      int a = (int)((float)(argb >>> 24) * Mth.clamp(f, 0.0F, 1.0F));
      return a << 24 | argb & 16777215;
   }

   static float easeOut(float t) {
      float u = 1.0F - Mth.clamp(t, 0.0F, 1.0F);
      return 1.0F - u * u;
   }

   static float ramp(float x, float width) {
      return AnimEnvelopes.smooth(x / width);
   }

   static float noise(int a, int b, int c) {
      int h = a * 668265261 ^ b * 374761393 ^ c * -1640531535;
      h ^= h >>> 15;
      h *= 739982445;
      h ^= h >>> 12;
      h *= 695872825;
      h ^= h >>> 15;
      return (float)(h >>> 8) * 5.9604645E-8F;
   }

   static float signed(int a, int b, int c) {
      return noise(a, b, c) * 2.0F - 1.0F;
   }

   static Vector3f perpendicular(Vector3f dir) {
      Vector3f ref = Math.abs(dir.y) > 0.95F ? RIGHT : UP;
      return new Vector3f(dir).cross(ref).normalize();
   }

   static void tri(
      Pose pose,
      VertexConsumer vc,
      float ax,
      float ay,
      float az,
      int ca,
      float bx,
      float by,
      float bz,
      int cb,
      float cx,
      float cy,
      float cz,
      int cc,
      boolean twoSided
   ) {
      vc.addVertex(pose, ax, ay, az).setColor(ca);
      vc.addVertex(pose, bx, by, bz).setColor(cb);
      vc.addVertex(pose, cx, cy, cz).setColor(cc);
      if (twoSided) {
         vc.addVertex(pose, ax, ay, az).setColor(ca);
         vc.addVertex(pose, cx, cy, cz).setColor(cc);
         vc.addVertex(pose, bx, by, bz).setColor(cb);
      }
   }

   static void tri(Pose pose, VertexConsumer vc, Vector3f a, int ca, Vector3f b, int cb, Vector3f c, int cc, boolean twoSided) {
      tri(pose, vc, a.x, a.y, a.z, ca, b.x, b.y, b.z, cb, c.x, c.y, c.z, cc, twoSided);
   }

   static void quad(Pose pose, VertexConsumer vc, Vector3f a, int ca, Vector3f b, int cb, Vector3f c, int cc, Vector3f d, int cd, boolean twoSided) {
      tri(pose, vc, a, ca, b, cb, c, cc, twoSided);
      tri(pose, vc, a, ca, c, cc, d, cd, twoSided);
   }

   static void streak(Pose pose, VertexConsumer vc, Vector3f from, Vector3f to, float wFrom, float wTo, int colFrom, int colTo) {
      Vector3f dir = new Vector3f(to).sub(from);
      float len = dir.length();
      if (!(len < 1.0E-4F)) {
         dir.div(len);
         Vector3f p1 = perpendicular(dir);
         Vector3f p2 = new Vector3f(dir).cross(p1).normalize();
         crossQuad(pose, vc, from, to, p1, wFrom, wTo, colFrom, colTo);
         crossQuad(pose, vc, from, to, p2, wFrom, wTo, colFrom, colTo);
      }
   }

   private static void crossQuad(Pose pose, VertexConsumer vc, Vector3f a, Vector3f b, Vector3f side, float wa, float wb, int ca, int cb) {
      Vector3f a0 = new Vector3f(side).mul(-wa).add(a);
      Vector3f a1 = new Vector3f(side).mul(wa).add(a);
      Vector3f b0 = new Vector3f(side).mul(-wb).add(b);
      Vector3f b1 = new Vector3f(side).mul(wb).add(b);
      quad(pose, vc, a0, ca, b0, cb, b1, cb, a1, ca, true);
   }

   static void cometStreak(
      Pose pose, VertexConsumer vc, Vector3f head, Vector3f tail, Vector3f ref, float halfWidth, int headCol, int bodyCol, int tailCol, boolean twoSided
   ) {
      float dx = tail.x - head.x;
      float dy = tail.y - head.y;
      float dz = tail.z - head.z;
      float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
      if (!(len < 1.0E-4F)) {
         Vector3f dir = new Vector3f(dx / len, dy / len, dz / len);
         Vector3f s1 = new Vector3f(dir).cross(ref);
         if (s1.lengthSquared() < 1.0E-6F) {
            s1.set(ref.y, ref.z, ref.x).cross(dir);
         }

         s1.normalize();
         Vector3f s2 = new Vector3f(dir).cross(s1).normalize();
         int taperCol = lerp(bodyCol, tailCol, 0.44444445F);
         cometRibbon(pose, vc, head, dx, dy, dz, s1, halfWidth, headCol, bodyCol, taperCol, tailCol, twoSided);
         cometRibbon(pose, vc, head, dx, dy, dz, s2, halfWidth, headCol, bodyCol, taperCol, tailCol, twoSided);
      }
   }

   private static void cometRibbon(
      Pose pose, VertexConsumer vc, Vector3f head, float dx, float dy, float dz, Vector3f side, float w, int c0, int c1, int c2, int c3, boolean twoSided
   ) {
      float[] t = new float[]{0.0F, 0.28F, 0.6F, 1.0F};
      float[] hw = new float[]{w * 0.35F, w, w * 0.55F, 0.0F};
      int[] col = new int[]{c0, c1, c2, c3};

      for (int i = 0; i < 3; i++) {
         float px = head.x + dx * t[i];
         float py = head.y + dy * t[i];
         float pz = head.z + dz * t[i];
         float qx = head.x + dx * t[i + 1];
         float qy = head.y + dy * t[i + 1];
         float qz = head.z + dz * t[i + 1];
         float pw = hw[i];
         float qw = hw[i + 1];
         float pAx = px + side.x * pw;
         float pAy = py + side.y * pw;
         float pAz = pz + side.z * pw;
         float pBx = px - side.x * pw;
         float pBy = py - side.y * pw;
         float pBz = pz - side.z * pw;
         float qAx = qx + side.x * qw;
         float qAy = qy + side.y * qw;
         float qAz = qz + side.z * qw;
         float qBx = qx - side.x * qw;
         float qBy = qy - side.y * qw;
         float qBz = qz - side.z * qw;
         tri(pose, vc, pAx, pAy, pAz, col[i], pBx, pBy, pBz, col[i], qBx, qBy, qBz, col[i + 1], twoSided);
         if (qw > 0.0F) {
            tri(pose, vc, pAx, pAy, pAz, col[i], qBx, qBy, qBz, col[i + 1], qAx, qAy, qAz, col[i + 1], twoSided);
         }
      }
   }

   static void lightning(Pose pose, VertexConsumer vc, Vector3f root, Vector3f tip, float rootHalf, int segs, float jitter, int seed, int colRoot, int colTip) {
      Vector3f axis = new Vector3f(tip).sub(root);
      float len = axis.length();
      if (!(len < 1.0E-4F) && segs >= 1) {
         axis.div(len);
         Vector3f p1 = perpendicular(axis);
         Vector3f p2 = new Vector3f(axis).cross(p1).normalize();
         float amp = jitter * len;
         Vector3f prev = new Vector3f(root);

         for (int i = 1; i <= segs; i++) {
            float t = (float)i / (float)segs;
            Vector3f p = new Vector3f(root).lerp(tip, t);
            if (i < segs) {
               float env = Mth.sin(t * (float) Math.PI);
               p.add(new Vector3f(p1).mul(signed(i, 0, seed) * amp * env)).add(new Vector3f(p2).mul(signed(i, 1, seed) * amp * env));
            }

            float t0 = (float)(i - 1) / (float)segs;
            streak(pose, vc, prev, p, rootHalf * (1.0F - t0), rootHalf * (1.0F - t), lerp(colRoot, colTip, t0), lerp(colRoot, colTip, t));
            prev = p;
         }
      }
   }

   static void bolt(Pose pose, VertexConsumer vc, Vector3f from, Vector3f to, int segs, float jitter, int seed, float w, int col, float haloW, int halo) {
      Vector3f[] joints = boltJoints(from, to, segs, jitter, seed);
      if (joints != null) {
         boltPath(pose, vc, joints, 1.0F, w, col, haloW, halo);
      }
   }

   static Vector3f[] boltJoints(Vector3f from, Vector3f to, int segs, float jitter, int seed) {
      Vector3f dir = new Vector3f(to).sub(from);
      if (!(dir.lengthSquared() < 1.0E-6F) && segs >= 1) {
         Vector3f axis = new Vector3f(dir).normalize();
         Vector3f p1 = perpendicular(axis);
         Vector3f p2 = new Vector3f(axis).cross(p1).normalize();
         Vector3f[] joints = new Vector3f[segs + 1];
         joints[0] = new Vector3f(from);

         for (int i = 1; i <= segs; i++) {
            float t = (float)i / (float)segs;
            Vector3f pt = new Vector3f(dir).mul(t).add(from);
            if (i < segs) {
               float env = Mth.sin(t * (float) Math.PI);
               pt.add(new Vector3f(p1).mul(signed(seed, i, 11) * jitter * env));
               pt.add(new Vector3f(p2).mul(signed(seed, i, 23) * jitter * env));
            }

            joints[i] = pt;
         }

         return joints;
      } else {
         return null;
      }
   }

   static void boltPath(Pose pose, VertexConsumer vc, Vector3f[] joints, float reveal, float w, int col, float haloW, int halo) {
      int segs = joints.length - 1;
      float shown = Mth.clamp(reveal, 0.0F, 1.0F) * (float)segs;

      for (int i = 1; i <= segs; i++) {
         float part = Mth.clamp(shown - (float)(i - 1), 0.0F, 1.0F);
         if (part <= 0.0F) {
            break;
         }

         Vector3f prev = joints[i - 1];
         Vector3f pt = part >= 1.0F ? joints[i] : new Vector3f(prev).lerp(joints[i], part);
         if (haloW > 0.0F) {
            streak(pose, vc, prev, pt, haloW, haloW, halo, halo);
         }

         streak(pose, vc, prev, pt, w, w, col, col);
      }
   }

   static void flatStreak(
      Pose pose, VertexConsumer vc, float ox, float y, float oz, float dx, float dz, float length, float rootWidth, int colRoot, int colTip, boolean twoSided
   ) {
      float px = -dz * rootWidth * 0.5F;
      float pz = dx * rootWidth * 0.5F;
      tri(pose, vc, ox + px, y, oz + pz, colRoot, ox - px, y, oz - pz, colRoot, ox + dx * length, y, oz + dz * length, colTip, twoSided);
   }
}
