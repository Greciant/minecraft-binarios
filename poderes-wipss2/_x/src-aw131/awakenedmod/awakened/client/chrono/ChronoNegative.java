package com.awakenedmod.awakened.client.chrono;

import com.awakenedmod.awakened.client.render.NegativeLayers;
import com.awakenedmod.awakened.core.Lab;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class ChronoNegative {
   static final Lab.Value GROW = Lab.CLIENT.def("chrono.negative.grow", 10.0, 1.0, 40.0, 1.0);
   static final Lab.Value DRAIN_FROM = Lab.CLIENT.def("chrono.negative.drain_from", 30.0, 0.0, 80.0, 1.0);
   static final Lab.Value DRAIN_TO = Lab.CLIENT.def("chrono.negative.drain_to", 40.0, 1.0, 80.0, 1.0);
   static final Lab.Value START_RADIUS = Lab.CLIENT.def("chrono.negative.start_radius", 0.3, 0.0, 4.0, 0.05);
   static final Lab.Value RINGS = Lab.CLIENT.def("chrono.negative.rings", 48.0, 8.0, 128.0, 1.0);
   static final Lab.Value SEGS = Lab.CLIENT.def("chrono.negative.segs", 96.0, 8.0, 256.0, 1.0);
   private static final int WHITE = -1;
   private static final float QUAD_Z = 0.2F;
   private static final float QUAD_HALF = 8.0F;
   private static ChronoNegative.UnitSphere sphere;

   private ChronoNegative() {
   }

   static void preloadLabKeys() {
   }

   public static Vec3 centre(ChronoFxClient.Effect e, float age) {
      return e.origin().lerp(e.pos, (double)growth(age));
   }

   public static float outer(ChronoFxClient.Effect e, float age) {
      return Mth.lerp(growth(age), Math.min(START_RADIUS.getF(), e.radius), e.radius);
   }

   public static float inner(ChronoFxClient.Effect e, float age) {
      float from = DRAIN_FROM.getF();
      float to = Math.max(from + 0.5F, DRAIN_TO.getF());
      float t = Mth.clamp((age - from) / (to - from), 0.0F, 1.0F);
      return e.radius * t * t * (3.0F - 2.0F * t);
   }

   public static boolean shown(ChronoFxClient.Effect e, float age) {
      return !ChronoView.noNegative() && !e.ended() && inner(e, age) < outer(e, age) - 0.001F;
   }

   public static boolean covers(ChronoFxClient.Effect e, Vec3 point, float age) {
      if (!shown(e, age)) {
         return false;
      } else {
         double d2 = point.distanceToSqr(centre(e, age));
         float in = inner(e, age);
         float out = outer(e, age);
         return d2 < (double)out * (double)out && d2 >= (double)in * (double)in;
      }
   }

   private static float growth(float age) {
      float t = Mth.clamp(age / Math.max(1.0F, GROW.getF()), 0.0F, 1.0F);
      return 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
   }

   static boolean draw(PoseStack world, MultiBufferSource buffers, ChronoFxClient.Effect e, Vec3 cam, float partialTick) {
      float age = e.age(partialTick);
      if (!shown(e, age)) {
         return false;
      } else {
         Vec3 c = centre(e, age);
         float out = outer(e, age);
         float in = inner(e, age);
         double d2 = cam.distanceToSqr(c);
         boolean inOuter = d2 < (double)out * (double)out;
         boolean inInner = in > 0.0F && d2 < (double)in * (double)in;
         VertexConsumer vc = buffers.getBuffer(NegativeLayers.triangles());
         world.pushPose();
         world.translate(c.x - cam.x, c.y - cam.y, c.z - cam.z);
         sphere(world.last(), vc, out, inOuter, -1);
         if (in > 0.0F) {
            sphere(world.last(), vc, in, inInner, -1);
         }

         world.popPose();
         return inOuter != inInner;
      }
   }

   static void screenQuad(PoseStack view, MultiBufferSource buffers) {
      screenQuad(view.last(), buffers.getBuffer(NegativeLayers.trianglesNoDepth()), -1);
   }

   static void screenQuad(Pose pose, VertexConsumer vc, int color) {
      float s = 8.0F;
      float z = -0.2F;
      vert(pose, vc, -s, -s, z, color);
      vert(pose, vc, s, -s, z, color);
      vert(pose, vc, s, s, z, color);
      vert(pose, vc, -s, -s, z, color);
      vert(pose, vc, s, s, z, color);
      vert(pose, vc, -s, s, z, color);
   }

   static void sphere(Pose pose, VertexConsumer vc, float radius, boolean inward, int color) {
      ChronoNegative.UnitSphere unit = unitSphere(RINGS.getI(), SEGS.getI());
      float[] p = unit.points;
      int stride = unit.segs + 1;

      for (int i = 0; i < unit.rings; i++) {
         for (int j = 0; j < unit.segs; j++) {
            int a = i * stride + j;
            int b = (i + 1) * stride + j;
            int c = (i + 1) * stride + j + 1;
            int d = i * stride + j + 1;
            if (inward) {
               tri(pose, vc, p, a, b, c, radius, color);
               tri(pose, vc, p, a, c, d, radius, color);
            } else {
               tri(pose, vc, p, a, c, b, radius, color);
               tri(pose, vc, p, a, d, c, radius, color);
            }
         }
      }
   }

   private static void tri(Pose pose, VertexConsumer vc, float[] p, int a, int b, int c, float r, int color) {
      vert(pose, vc, p[a * 3] * r, p[a * 3 + 1] * r, p[a * 3 + 2] * r, color);
      vert(pose, vc, p[b * 3] * r, p[b * 3 + 1] * r, p[b * 3 + 2] * r, color);
      vert(pose, vc, p[c * 3] * r, p[c * 3 + 1] * r, p[c * 3 + 2] * r, color);
   }

   private static void vert(Pose pose, VertexConsumer vc, float x, float y, float z, int color) {
      vc.addVertex(pose, x, y, z).setColor(color);
   }

   private static ChronoNegative.UnitSphere unitSphere(int rings, int segs) {
      ChronoNegative.UnitSphere s = sphere;
      if (s == null || s.rings != rings || s.segs != segs) {
         s = new ChronoNegative.UnitSphere(rings, segs);
         sphere = s;
      }

      return s;
   }

   private static final class UnitSphere {
      final int rings;
      final int segs;
      final float[] points;

      UnitSphere(int rings, int segs) {
         this.rings = rings;
         this.segs = segs;
         this.points = new float[(rings + 1) * (segs + 1) * 3];
         int k = 0;

         for (int i = 0; i <= rings; i++) {
            float theta = (float) Math.PI * (float)i / (float)rings;
            float st = Mth.sin(theta);
            float ct = Mth.cos(theta);

            for (int j = 0; j <= segs; j++) {
               float phi = (float) (Math.PI * 2) * (float)j / (float)segs;
               this.points[k++] = st * Mth.cos(phi);
               this.points[k++] = ct;
               this.points[k++] = st * Mth.sin(phi);
            }
         }
      }
   }
}
