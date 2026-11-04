package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.core.Lab;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class ChronoDial {
   public static final int WHITE_GOLD = 16774088;
   public static final int AMBER = 15123274;
   public static final int MARKS = 12;
   public static final int ALL_LIT = 4095;
   static final Lab.Value MARK_WIDTH = Lab.CLIENT.def("chrono.dial.mark_width", 0.045, 0.005, 0.2, 0.005);
   static final Lab.Value MARK_INNER = Lab.CLIENT.def("chrono.dial.mark_inner", 0.8, 0.3, 1.0, 0.01);
   static final Lab.Value QUARTER_INNER = Lab.CLIENT.def("chrono.dial.quarter_inner", 0.7, 0.3, 1.0, 0.01);
   static final Lab.Value HAND_WIDTH = Lab.CLIENT.def("chrono.dial.hand_width", 0.035, 0.005, 0.2, 0.005);
   static final Lab.Value FRINGE = Lab.CLIENT.def("chrono.dial.fringe", 2.6, 1.0, 6.0, 0.1);
   static final Lab.Value FRINGE_ALPHA = Lab.CLIENT.def("chrono.dial.fringe_alpha", 0.35, 0.0, 1.0, 0.01);
   static final Lab.Value CORE_ALPHA = Lab.CLIENT.def("chrono.dial.core_alpha", 0.85, 0.0, 1.0, 0.01);
   static final Lab.Value DARK_ALPHA = Lab.CLIENT.def("chrono.dial.dark_alpha", 0.22, 0.0, 1.0, 0.01);

   private ChronoDial() {
   }

   public static void preloadLabKeys() {
   }

   public static Vector3f[] upright(Vec3 centre, Vec3 cam) {
      float fx = (float)(centre.x - cam.x);
      float fz = (float)(centre.z - cam.z);
      float len = Mth.sqrt(fx * fx + fz * fz);
      if (len < 1.0E-4F) {
         fx = 0.0F;
         fz = -1.0F;
      } else {
         fx /= len;
         fz /= len;
      }

      return new Vector3f[]{new Vector3f(-fz, 0.0F, fx), new Vector3f(0.0F, 1.0F, 0.0F)};
   }

   public static void draw(Pose pose, MultiBufferSource buffers, Vector3f centre, Vector3f right, Vector3f up, ChronoDial.Face face) {
      if (!(face.alpha() <= 0.0F) && !(face.radius() <= 0.0F)) {
         ChronoDial.Style s = ChronoDial.Style.read();
         VertexConsumer vc = FxLayers.begin(buffers, FxLayer.LIGHT);
         float r = face.radius();
         float reach = r * (1.0F + Math.max(0.0F, face.spring()));
         float lit = s.coreAlpha() * face.alpha();
         float fringe = s.fringeAlpha() * face.alpha();
         float dark = s.darkAlpha() * face.alpha();
         float mw = s.markWidth() * r;

         for (int i = 0; i < 12; i++) {
            float angle = (float)i * (float) (Math.PI / 6);
            float inner = (i % 3 == 0 ? s.quarterInner() : s.markInner()) * reach;
            boolean on = (face.litMask() & 1 << i) != 0;
            if (on) {
               bar(pose, vc, centre, right, up, angle, inner - mw, reach + mw, mw * s.fringe(), argb(fringe, 15123274));
               bar(pose, vc, centre, right, up, angle, inner, reach, mw, argb(lit, 16774088));
            } else {
               bar(pose, vc, centre, right, up, angle, inner, reach, mw, argb(dark, 15123274));
            }
         }

         float hw = s.handWidth() * r;

         for (ChronoDial.Hand hand : face.hands()) {
            float length = hand.length() * r;
            bar(pose, vc, centre, right, up, hand.angle(), -hw * 2.0F, length + hw, hw * s.fringe(), argb(fringe, 15123274));
            bar(pose, vc, centre, right, up, hand.angle(), -hw, length, hw, argb(lit, 16774088));
         }
      }
   }

   private static void bar(Pose pose, VertexConsumer vc, Vector3f c, Vector3f right, Vector3f up, float angle, float from, float to, float w, int color) {
      if (color >>> 24 != 0) {
         float sin = Mth.sin(angle);
         float cos = Mth.cos(angle);
         float ax = sin * right.x + cos * up.x;
         float ay = sin * right.y + cos * up.y;
         float az = sin * right.z + cos * up.z;
         float px = (cos * right.x - sin * up.x) * w;
         float py = (cos * right.y - sin * up.y) * w;
         float pz = (cos * right.z - sin * up.z) * w;
         float x0 = c.x + ax * from;
         float y0 = c.y + ay * from;
         float z0 = c.z + az * from;
         float x1 = c.x + ax * to;
         float y1 = c.y + ay * to;
         float z1 = c.z + az * to;
         quad(pose, vc, x0 - px, y0 - py, z0 - pz, x0 + px, y0 + py, z0 + pz, x1 + px, y1 + py, z1 + pz, x1 - px, y1 - py, z1 - pz, color);
      }
   }

   static void quad(
      Pose pose,
      VertexConsumer vc,
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
      int color
   ) {
      vc.addVertex(pose, ax, ay, az).setColor(color);
      vc.addVertex(pose, bx, by, bz).setColor(color);
      vc.addVertex(pose, cx, cy, cz).setColor(color);
      vc.addVertex(pose, ax, ay, az).setColor(color);
      vc.addVertex(pose, cx, cy, cz).setColor(color);
      vc.addVertex(pose, dx, dy, dz).setColor(color);
      vc.addVertex(pose, ax, ay, az).setColor(color);
      vc.addVertex(pose, cx, cy, cz).setColor(color);
      vc.addVertex(pose, bx, by, bz).setColor(color);
      vc.addVertex(pose, ax, ay, az).setColor(color);
      vc.addVertex(pose, dx, dy, dz).setColor(color);
      vc.addVertex(pose, cx, cy, cz).setColor(color);
   }

   static int argb(float alpha, int rgb) {
      return Mth.clamp(Math.round(alpha * 255.0F), 0, 255) << 24 | rgb & 16777215;
   }

   public static record Face(float radius, int litMask, ChronoDial.Hand[] hands, float alpha, float spring) {
   }

   public static record Hand(float angle, float length) {
   }

   private static record Style(
      float markWidth, float markInner, float quarterInner, float handWidth, float fringe, float fringeAlpha, float coreAlpha, float darkAlpha
   ) {
      static ChronoDial.Style read() {
         return new ChronoDial.Style(
            ChronoDial.MARK_WIDTH.getF(),
            ChronoDial.MARK_INNER.getF(),
            ChronoDial.QUARTER_INNER.getF(),
            ChronoDial.HAND_WIDTH.getF(),
            ChronoDial.FRINGE.getF(),
            ChronoDial.FRINGE_ALPHA.getF(),
            ChronoDial.CORE_ALPHA.getF(),
            ChronoDial.DARK_ALPHA.getF()
         );
      }
   }
}
