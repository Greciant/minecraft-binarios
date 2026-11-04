package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.core.Lab;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class ChronoThread {
   static final Lab.Value WIDTH = Lab.CLIENT.def("chrono.thread.width", 0.07, 0.01, 0.4, 0.005);
   static final Lab.Value CORE = Lab.CLIENT.def("chrono.thread.core", 0.35, 0.05, 1.0, 0.01);
   static final Lab.Value ALPHA = Lab.CLIENT.def("chrono.thread.alpha", 0.55, 0.0, 1.0, 0.01);
   static final Lab.Value CORE_ALPHA = Lab.CLIENT.def("chrono.thread.core_alpha", 0.8, 0.0, 1.0, 0.01);

   private ChronoThread() {
   }

   public static void preloadLabKeys() {
   }

   public static void draw(Pose pose, MultiBufferSource buffers, List<Vec3> points, Vec3 cam, float lift, float windUp, float alpha) {
      if (points.size() >= 2 && !(alpha <= 0.0F) && !(windUp >= 1.0F)) {
         float total = 0.0F;

         for (int i = 1; i < points.size(); i++) {
            total += (float)points.get(i).distanceTo(points.get(i - 1));
         }

         if (!(total < 0.001F)) {
            float half = WIDTH.getF() * 0.5F;
            float coreHalf = half * CORE.getF();
            int gold = ChronoDial.argb(ALPHA.getF() * alpha, 15123274);
            int core = ChronoDial.argb(CORE_ALPHA.getF() * alpha, 16774088);
            VertexConsumer vc = FxLayers.begin(buffers, FxLayer.LIGHT);
            float cut = Mth.clamp(windUp, 0.0F, 1.0F) * total;
            float walked = 0.0F;

            for (int i = 1; i < points.size(); i++) {
               Vec3 a = points.get(i - 1);
               Vec3 b = points.get(i);
               float len = (float)b.distanceTo(a);
               float segEnd = walked + len;
               if (segEnd > cut && len > 1.0E-4F) {
                  float from = Math.max(0.0F, (cut - walked) / len);
                  Vec3 start = a.lerp(b, (double)from);
                  segment(pose, vc, start, b, cam, lift, half, gold);
                  segment(pose, vc, start, b, cam, lift, coreHalf, core);
               }

               walked = segEnd;
            }
         }
      }
   }

   private static void segment(Pose pose, VertexConsumer vc, Vec3 a, Vec3 b, Vec3 cam, float lift, float half, int color) {
      float ax = (float)(a.x - cam.x);
      float ay = (float)(a.y + (double)lift - cam.y);
      float az = (float)(a.z - cam.z);
      float bx = (float)(b.x - cam.x);
      float by = (float)(b.y + (double)lift - cam.y);
      float bz = (float)(b.z - cam.z);
      float dx = bx - ax;
      float dy = by - ay;
      float dz = bz - az;
      float mx = (ax + bx) * 0.5F;
      float my = (ay + by) * 0.5F;
      float mz = (az + bz) * 0.5F;
      float sx = dy * mz - dz * my;
      float sy = dz * mx - dx * mz;
      float sz = dx * my - dy * mx;
      float sl = Mth.sqrt(sx * sx + sy * sy + sz * sz);
      if (!(sl < 1.0E-6F)) {
         sx *= half / sl;
         sy *= half / sl;
         sz *= half / sl;
         ChronoDial.quad(pose, vc, ax - sx, ay - sy, az - sz, ax + sx, ay + sy, az + sz, bx + sx, by + sy, bz + sz, bx - sx, by - sy, bz - sz, color);
      }
   }
}
