package com.awakenedmod.awakened.client.render.limitless.infinity;

import com.awakenedmod.awakened.client.render.FxLayer;
import com.awakenedmod.awakened.client.render.limitless.Ease;
import com.awakenedmod.awakened.client.render.limitless.Hairline;
import com.awakenedmod.awakened.client.render.limitless.LimitlessComfort;
import com.awakenedmod.awakened.client.render.limitless.LimitlessFrame;
import com.awakenedmod.awakened.client.render.limitless.LimitlessLook;
import com.awakenedmod.awakened.client.render.limitless.LimitlessMesh;
import com.awakenedmod.awakened.core.Lab;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class InfinityZeno implements LimitlessLook<InfinityCue> {
   private static final String K = "limitless.infinity.";
   private static final Lab.Value DEPTH = Lab.CLIENT.def("limitless.infinity.depth", 2.0, 0.3, 5.0, 0.05);
   private static final Lab.Value STEPS = Lab.CLIENT.def("limitless.infinity.steps", 9.0, 3.0, 16.0, 1.0);
   private static final Lab.Value HOOP = Lab.CLIENT.def("limitless.infinity.hoop", 0.42, 0.05, 1.5, 0.01);
   private static final Lab.Value HOOP_ALPHA = Lab.CLIENT.def("limitless.infinity.hoop_alpha", 190.0, 0.0, 255.0, 1.0);
   private static final Lab.Value GHOST_ALPHA = Lab.CLIENT.def("limitless.infinity.ghost_alpha", 210.0, 0.0, 255.0, 1.0);
   private static final Lab.Value WIDTH_PX = Lab.CLIENT.def("limitless.infinity.width_px", 2.8, 0.8, 8.0, 0.1);
   private static final Lab.Value DRIFT = Lab.CLIENT.def("limitless.infinity.drift", 20.0, 4.0, 80.0, 1.0);
   private static final Lab.Value BODY = Lab.CLIENT.def("limitless.infinity.body_alpha", 110.0, 0.0, 255.0, 1.0);
   private static final Lab.Value FADE = Lab.CLIENT.def("limitless.infinity.fade", 7.0, 1.0, 30.0, 1.0);
   private static final int PALE = 11855615;
   private static final int WHITE = 15661567;
   private static final int DARK = 662062;
   private final List<InfinityContact> live = new ArrayList<>();

   public static float tail() {
      return FADE.getF();
   }

   public void draw(LimitlessFrame f, InfinityCue s, float age) {
      float fade = FADE.getF();
      this.live.clear();

      for (InfinityContact c : s.contacts(age, f.partialTick)) {
         if (c.live(fade)) {
            this.live.add(c);
         }
      }

      if (!this.live.isEmpty()) {
         float calm = LimitlessComfort.distortion(f.mc);
         float[] gaps = new float[this.live.size()];

         for (int i = 0; i < gaps.length; i++) {
            InfinityContact cx = this.live.get(i);
            gaps[i] = cx.melee() ? (float)((double)DEPTH.getF() * 0.5 * (double)(1.0F - cx.approach())) : gap(cx, f.partialTick);
         }

         for (int pass = 0; pass < 2; pass++) {
            boolean light = pass == 1;
            FxLayer layer = light ? FxLayer.LIGHT : FxLayer.PAINT;
            LimitlessMesh m = f.begin(layer);

            for (int i = 0; i < gaps.length; i++) {
               InfinityContact cx = this.live.get(i);
               series(f, m, cx, gaps[i], cx.melee() ? 0.5F : 1.0F, light, calm);
            }

            f.end(layer);
         }
      }
   }

   private static float gap(InfinityContact c, float pt) {
      if (c.t() >= 0.0F) {
         return 0.0F;
      } else {
         Entity e = c.projectile();
         return e == null ? 0.0F : (float)e.getPosition(pt).distanceTo(c.at());
      }
   }

   private static void series(LimitlessFrame f, LimitlessMesh m, InfinityContact c, float gap, float size, boolean light, float calm) {
      float depth = DEPTH.getF() * size;
      int steps = STEPS.getI();
      float out = 1.0F - c.released(FADE.getF());
      float in = Ease.smooth(c.approach());
      float drift = c.t() > 0.0F ? c.t() / DRIFT.getF() * (1.0F + 2.0F * c.released(FADE.getF())) * calm : 0.0F;
      float phase = Ease.frac(drift);
      Vec3 from = c.from();
      Vector3f axis = new Vector3f((float)from.x, (float)from.y, (float)from.z).normalize();
      Vector3f p = f.rel(c.at());
      float hoop = HOOP.getF() * (0.6F + 0.4F * size);
      float px = WIDTH_PX.getF();

      for (int k = 0; k < steps; k++) {
         float x = depth * (float)Math.pow(2.0, (double)(-((float)k + phase)));
         if (!(x < gap)) {
            float a = (1.0F - Ease.smooth((x - 0.8F * depth) / (0.2F * depth))) * Ease.smooth(x / 0.03F) * out * in;
            a *= Ease.smooth((x - gap) / 0.25F + (gap == 0.0F ? 1.0F : 0.0F));
            if (!(a <= 0.003F)) {
               float near = 1.0F - x / depth;
               Vector3f at = new Vector3f(axis).mul(x).add(p);
               float rr = 0.025F + hoop * (float)Math.pow((double)(x / depth), 0.6);
               float hw = Hairline.halfWidth(f.mc, at, px);
               Vector3f tail = new Vector3f(axis).mul(x * 1.55F).add(p);
               if (light) {
                  float ha = HOOP_ALPHA.getF() / 255.0F * a * (0.6F + 0.4F * near);
                  m.ringFlat(at, axis, rr, hw * 1.2F, 40, LimitlessMesh.rgba(11855615, ha), 0, 0.0F);
                  m.ringFlat(at, axis, rr, hw * 5.0F, 40, LimitlessMesh.rgba(11855615, ha * 0.18F), 0, 0.0F);
                  m.streak(
                     at,
                     tail,
                     hw * 2.2F,
                     hw * 0.6F,
                     LimitlessMesh.rgba(15661567, GHOST_ALPHA.getF() / 255.0F * a * (0.3F + 0.7F * near)),
                     LimitlessMesh.rgba(11855615, 0.0F)
                  );
               } else {
                  m.ringFlat(at, axis, rr, hw * 2.6F, 40, LimitlessMesh.rgba(662062, BODY.getF() / 255.0F * a), 0, 0.0F);
                  m.streak(at, tail, hw * 2.8F, hw, LimitlessMesh.rgba(662062, BODY.getF() / 255.0F * a), LimitlessMesh.rgba(662062, 0.0F));
               }
            }
         }
      }

      if (light && c.t() >= 0.0F) {
         float glow = Ease.ramp(c.t(), 0.0F, 3.0F) * out;
         m.disc(p, 0.06F * (0.6F + 0.4F * size), 12, LimitlessMesh.rgba(15661567, 0.6F * glow), LimitlessMesh.rgba(11855615, 0.0F));
      }
   }
}
