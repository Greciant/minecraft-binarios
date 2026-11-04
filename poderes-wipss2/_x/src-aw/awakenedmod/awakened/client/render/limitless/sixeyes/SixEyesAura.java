package com.awakenedmod.awakened.client.render.limitless.sixeyes;

import com.awakenedmod.awakened.client.render.FxLayer;
import com.awakenedmod.awakened.client.render.HeadAnchor;
import com.awakenedmod.awakened.client.render.limitless.BodyCubes;
import com.awakenedmod.awakened.client.render.limitless.Ease;
import com.awakenedmod.awakened.client.render.limitless.LimitlessComfort;
import com.awakenedmod.awakened.client.render.limitless.LimitlessFrame;
import com.awakenedmod.awakened.client.render.limitless.LimitlessLook;
import com.awakenedmod.awakened.client.render.limitless.LimitlessMesh;
import com.awakenedmod.awakened.client.render.limitless.SpaceBend;
import com.awakenedmod.awakened.core.Lab;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;

public final class SixEyesAura implements LimitlessLook<SixEyesCue> {
   private static final String K = "limitless.sixeyes.";
   private static final Lab.Value FILL = Lab.CLIENT.def("limitless.sixeyes.fill_alpha", 46.0, 0.0, 255.0, 1.0);
   private static final Lab.Value FRINGE = Lab.CLIENT.def("limitless.sixeyes.fringe_alpha", 16.0, 0.0, 255.0, 1.0);
   private static final Lab.Value FRINGE_W = Lab.CLIENT.def("limitless.sixeyes.fringe_width", 0.07, 0.0, 0.4, 0.005);
   private static final Lab.Value WISPS = Lab.CLIENT.def("limitless.sixeyes.wisps", 26.0, 0.0, 60.0, 1.0);
   private static final Lab.Value WISP_H = Lab.CLIENT.def("limitless.sixeyes.wisp_height", 0.45, 0.1, 2.0, 0.05);
   private static final Lab.Value WISP_W = Lab.CLIENT.def("limitless.sixeyes.wisp_width", 0.04, 0.01, 0.3, 0.005);
   private static final Lab.Value WISP_ALPHA = Lab.CLIENT.def("limitless.sixeyes.wisp_alpha", 70.0, 0.0, 255.0, 1.0);
   private static final Lab.Value WISP_PERIOD = Lab.CLIENT.def("limitless.sixeyes.wisp_period", 36.0, 8.0, 120.0, 1.0);
   private static final Lab.Value EYE_FWD = Lab.CLIENT.def("limitless.sixeyes.eye_forward_px", 4.4, 0.0, 10.0, 0.1);
   private static final Lab.Value EYE_ALPHA = Lab.CLIENT.def("limitless.sixeyes.eye_alpha", 120.0, 0.0, 255.0, 1.0);
   private static final int DEEP = 2053631;
   private static final int FRINGE_COL = 3968255;
   private static final int WISP = 4889855;
   private static final int WISP_HOT = 10935039;

   public void draw(LimitlessFrame f, SixEyesCue s, float age) {
      float in = Ease.ramp(age, 0.0F, 10.0F);
      List<LivingEntity> seen = s.seen();
      float calm = LimitlessComfort.distortion(f.mc);
      LimitlessMesh m = f.begin(FxLayer.LIGHT_THROUGH_WALLS);
      Vector3f[] grown = new Vector3f[8];

      for (LivingEntity e : seen) {
         List<Vector3f[]> boxes = BodyCubes.collect(f, e);
         float bodyIn = in * s.presence(e);
         float breathe = 0.85F + 0.15F * (float)Math.sin((double)(age * 0.07F + (float)e.getId())) * calm;
         int fill = LimitlessMesh.rgba(2053631, FILL.getF() / 255.0F * bodyIn * breathe);
         int fringe = LimitlessMesh.rgba(3968255, FRINGE.getF() / 255.0F * bodyIn * breathe);

         for (Vector3f[] c : boxes) {
            BodyCubes.frontFaces(m, c, fill);
            BodyCubes.frontFaces(m, BodyCubes.inflate(c, FRINGE_W.getF() * 0.5F, grown), fringe);
            BodyCubes.frontFaces(m, BodyCubes.inflate(c, FRINGE_W.getF(), grown), LimitlessMesh.fade(fringe, 0.6F));
         }

         wisps(f, m, boxes, s.seed() * 31L + (long)e.getId(), age, bodyIn);
      }

      f.end(FxLayer.LIGHT_THROUGH_WALLS);
      HeadAnchor.Head head = s.head();
      if (head != null) {
         LimitlessMesh light = f.begin(FxLayer.LIGHT);
         eyes(f, light, head, age, in);
         f.end(FxLayer.LIGHT);
      }
   }

   private static void wisps(LimitlessFrame f, LimitlessMesh m, List<Vector3f[]> boxes, long seed, float age, float in) {
      int n = WISPS.getI();
      if (!boxes.isEmpty() && n != 0) {
         float h = WISP_H.getF();
         float w = WISP_W.getF();
         float period = WISP_PERIOD.getF();
         float alpha = WISP_ALPHA.getF() / 255.0F * in;
         int seg = 9;
         Vector3f[] pts = new Vector3f[seg];
         float[] wid = new float[seg];
         int[] col = new int[seg];
         Vector3f base = new Vector3f();

         for (int i = 0; i < n; i++) {
            long si = seed * 977L + (long)i;
            Vector3f[] c = boxes.get((int)(SpaceBend.hash(si, 1) * (float)boxes.size()) % boxes.size());
            float side = SpaceBend.hash(si, 2) * 4.0F;
            int sd = (int)side;
            float along = side - (float)sd;
            float u = sd == 0 ? along : (sd == 1 ? 1.0F : (sd == 2 ? 1.0F - along : 0.0F));
            float wz = sd == 0 ? 0.0F : (sd == 1 ? along : (sd == 2 ? 1.0F : 1.0F - along));
            float up = c[2].y > c[0].y ? 1.0F : 0.0F;
            float h3 = SpaceBend.hash(si, 3);
            float v = up + (1.0F - 2.0F * up) * h3 * h3 * 0.8F;
            BodyCubes.lerp(c, u, v, wz, base);
            float p = period * (0.8F + 0.4F * SpaceBend.hash(si, 4));
            float tau = Ease.frac((age + SpaceBend.hash(si, 5) * p) / p);
            float env = (float)Math.sin(Math.PI * (double)tau);
            float hi = h * (0.6F + 0.6F * SpaceBend.hash(si, 6));
            float sway = SpaceBend.hash(si, 7) * 6.28F;

            for (int k = 0; k < seg; k++) {
               float sk = (float)k / (float)(seg - 1);
               float y = hi * Math.max(0.0F, tau - 0.55F + 0.55F * sk);
               float lat = 0.05F * (y / hi) * (float)Math.sin((double)(age * 0.09F + sway + sk * 2.5F));
               pts[k] = new Vector3f(base).add(f.camRight.x * lat, y, f.camRight.z * lat);
               wid[k] = w * (float)Math.sin(Math.PI * (double)(0.15F + 0.85F * sk)) * (1.0F - 0.6F * sk) * (0.5F + 0.5F * env) + 0.002F;
               float a = alpha * env * (float)Math.sin(Math.PI * (double)Math.min(1.0F, sk * 1.15F + 0.05F));
               col[k] = LimitlessMesh.rgba(
                  LimitlessMesh.mix(LimitlessMesh.rgba(4889855, 1.0F), LimitlessMesh.rgba(10935039, 1.0F), 0.3F * (1.0F - sk)) & 16777215, a
               );
            }

            m.ribbon(pts, wid, col, seg);

            for (int k = 0; k < seg; k++) {
               wid[k] *= 2.6F;
               col[k] = LimitlessMesh.fade(col[k], 0.3F);
            }

            m.ribbon(pts, wid, col, seg);
         }
      }
   }

   private static void eyes(LimitlessFrame f, LimitlessMesh m, HeadAnchor.Head head, float age, float in) {
      float a = EYE_ALPHA.getF() / 255.0F * in;
      Vector3f back = new Vector3f(head.forward()).negate();
      Vector3f up = head.up();

      for (int side = -1; side <= 1; side += 2) {
         Vector3f p = f.rel(head.eye(side, EYE_FWD.getF()));
         m.disc(p, 0.07F, 14, LimitlessMesh.rgba(5219583, 0.55F * a), 0);
         m.disc(p, 0.022F, 8, LimitlessMesh.rgba(14742783, a), LimitlessMesh.rgba(8176895, 0.0F));
         int seg = 5;
         Vector3f[] pts = new Vector3f[seg];
         float[] wid = new float[seg];
         int[] col = new int[seg];

         for (int k = 0; k < seg; k++) {
            float sk = (float)k / (float)(seg - 1);
            float sway = 0.02F * (float)Math.sin((double)(age * 0.11F + sk * 3.0F));
            pts[k] = new Vector3f(p).fma(0.16F * sk, back).fma(0.1F * sk * sk, up).fma(sway, f.camRight);
            wid[k] = 0.018F * (1.0F - sk) + 0.002F;
            col[k] = LimitlessMesh.rgba(7321855, 0.6F * a * (1.0F - sk));
         }

         m.ribbon(pts, wid, col, seg);
      }
   }
}
