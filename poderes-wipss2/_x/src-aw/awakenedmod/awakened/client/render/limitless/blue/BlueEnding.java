package com.awakenedmod.awakened.client.render.limitless.blue;

import com.awakenedmod.awakened.client.render.FxLayer;
import com.awakenedmod.awakened.client.render.limitless.Ease;
import com.awakenedmod.awakened.client.render.limitless.LimitlessComfort;
import com.awakenedmod.awakened.client.render.limitless.LimitlessFrame;
import com.awakenedmod.awakened.client.render.limitless.LimitlessMesh;
import com.awakenedmod.awakened.client.render.limitless.SpaceBend;
import com.awakenedmod.awakened.content.limitless.blue.BlueEnd;
import com.awakenedmod.awakened.core.Lab;
import org.joml.Vector3f;

public final class BlueEnding {
   private static final String K = "limitless.blue.end.";
   private static final Lab.Value IN_REACH = Lab.CLIENT.def("limitless.blue.end.implode_reach", 3.4, 0.5, 8.0, 0.05);
   private static final Lab.Value IN_TIME = Lab.CLIENT.def("limitless.blue.end.implode_time", 6.0, 2.0, 12.0, 0.5);
   private static final Lab.Value IN_ALPHA = Lab.CLIENT.def("limitless.blue.end.implode_alpha", 150.0, 0.0, 255.0, 1.0);
   private static final Lab.Value IN_SPOKES = Lab.CLIENT.def("limitless.blue.end.implode_spokes", 18.0, 0.0, 48.0, 1.0);
   private static final Lab.Value POP_REACH = Lab.CLIENT.def("limitless.blue.end.pop_reach", 4.2, 0.5, 10.0, 0.05);
   private static final Lab.Value POP_ALPHA = Lab.CLIENT.def("limitless.blue.end.pop_alpha", 140.0, 0.0, 255.0, 1.0);
   private static final int CYAN = 4633855;
   private static final int DEEP_BLUE = 666784;
   private static final int PALE = 13168895;

   private BlueEnding() {
   }

   static float squeeze(BlueCue s, float age) {
      return s.ending() != BlueEnd.IMPLODE ? 0.0F : Ease.ramp(age, s.release() - 6.0F, 6.0F);
   }

   static float fadeTicks(BlueCue s) {
      return s.ending() == BlueEnd.POP ? 4.0F : 8.0F;
   }

   public static void draw(LimitlessFrame f, BlueCue s, float age) {
      float t = age - s.release();
      if (!(t < 0.0F) && !(t > 12.0F)) {
         Vector3f c = f.rel(s.orb());
         float comfort = LimitlessComfort.distortion(f.mc);
         switch (s.ending()) {
            case IMPLODE:
               implode(f, c, s.seed(), t, comfort);
               break;
            case POP:
               pop(f, c, s.seed(), t, comfort);
         }
      }
   }

   private static void implode(LimitlessFrame f, Vector3f c, long seed, float t, float comfort) {
      float time = IN_TIME.getF();
      float u = Ease.clamp01(t / time);
      float closing = (float)Math.pow((double)u, 1.8);
      float r = IN_REACH.getF() * (1.0F - closing) + 0.05F;
      float a = IN_ALPHA.getF() * Ease.ramp(t, 0.0F, 2.0F) * (1.0F - Ease.ramp(t, time - 2.0F, 2.0F));
      LimitlessMesh light = f.begin(FxLayer.LIGHT);
      if (a >= 1.0F && comfort > 0.0F) {
         SpaceBend frame = SpaceBend.random(seed * 83L + 5L, 1.0F, 1.0F, 0.0F, 0.0F, 0.0F, 1.0F);
         Vector3f ax = frame.axis();
         Vector3f uu = frame.start();
         Vector3f v = new Vector3f(ax).cross(uu);
         float twist = 4.0F + 8.0F * closing;
         float spin = t * 0.5F;
         float alpha = a * comfort;
         light.sphere(c, r, 14, 30, (nn, facing) -> {
            float face = 0.3F + 0.7F * (float)Math.sqrt((double)Math.max(0.0F, facing));
            return LimitlessMesh.rgba(4633855, alpha / 255.0F * BlueLapse.band(nn, ax, uu, v, twist, spin) * face);
         });
      }

      int n = IN_SPOKES.getI();
      float sa = a / 255.0F;
      Vector3f dir = new Vector3f();

      for (int i = 0; i < n && sa > 0.004F; i++) {
         long si = seed * 389L + (long)i;
         dir.set(SpaceBend.hash(si, 1) * 2.0F - 1.0F, SpaceBend.hash(si, 2) * 2.0F - 1.0F, SpaceBend.hash(si, 3) * 2.0F - 1.0F);
         if (!(dir.lengthSquared() < 1.0E-4F)) {
            dir.normalize();
            float head = r * (0.6F + 0.4F * SpaceBend.hash(si, 4));
            float tail = head + 0.6F + 1.4F * (1.0F - closing);
            Vector3f a0 = new Vector3f(dir).mul(tail).add(c);
            Vector3f b0 = new Vector3f(dir).mul(head).add(c);
            light.streak(a0, b0, 0.01F, 0.06F, LimitlessMesh.rgba(4633855, 0.0F), LimitlessMesh.rgba(13168895, sa * 0.9F));
         }
      }

      float wink = Ease.ramp(t, time - 2.0F, 2.0F) * (1.0F - Ease.ramp(t, time + 1.0F, 3.0F));
      if (wink > 0.01F) {
         light.disc(c, 0.22F * (1.0F - 0.6F * Ease.ramp(t, time, 4.0F)), 16, LimitlessMesh.rgba(13168895, 0.75F * wink), LimitlessMesh.rgba(4633855, 0.0F));
      }

      f.end(FxLayer.LIGHT);
   }

   private static void pop(LimitlessFrame f, Vector3f c, long seed, float t, float comfort) {
      float u = Ease.clamp01(t / 12.0F);
      float out = Ease.out(u, 2.4F);
      float reach = POP_REACH.getF();
      float a = POP_ALPHA.getF() / 255.0F * Ease.ramp(t, 0.0F, 2.0F) * (1.0F - Ease.ramp(t, 5.3999996F, 6.6000004F));
      if (!(a <= 0.004F)) {
         SpaceBend frame = SpaceBend.random(seed * 97L + 11L, 1.0F, 1.0F, 0.0F, 0.0F, 0.0F, 1.0F);
         Vector3f ax = frame.axis();
         Vector3f uu = frame.start();
         Vector3f v = new Vector3f(ax).cross(uu);
         float rBlue = 0.4F + reach * 0.55F * out;
         float rRed = 0.3F + reach * out;
         LimitlessMesh paint = f.begin(FxLayer.PAINT);
         paint.ringFlat(c, ax, rRed, 0.35F + 0.3F * out, 48, LimitlessMesh.rgba(5636104, a * 0.55F), LimitlessMesh.rgba(5636104, 0.0F), t * 0.2F);
         paint.ringFlat(c, uu, rBlue, 0.3F + 0.25F * out, 48, LimitlessMesh.rgba(666784, a * 0.55F), LimitlessMesh.rgba(666784, 0.0F), -t * 0.2F);
         f.end(FxLayer.PAINT);
         LimitlessMesh light = f.begin(FxLayer.LIGHT);
         if (comfort > 0.0F) {
            float shell = a * comfort;
            light.sphere(c, rBlue, 14, 30, (nn, facing) -> {
               float face = 0.3F + 0.7F * (float)Math.sqrt((double)Math.max(0.0F, facing));
               return LimitlessMesh.rgba(4633855, shell * 0.8F * BlueLapse.band(nn, ax, uu, v, 5.0F, t * 0.4F) * face);
            });
            light.sphere(c, rRed, 14, 30, (nn, facing) -> {
               float edge = 1.0F - Math.max(0.0F, facing);
               float bands = BlueLapse.band(nn, ax, uu, v, -5.0F, -t * 0.4F + 1.05F);
               return LimitlessMesh.rgba(16716806, shell * (0.25F + 0.75F * bands) * (0.3F + 0.7F * edge * edge));
            });
         }

         light.ringFlat(c, ax, rRed, 0.12F + 0.1F * out, 64, LimitlessMesh.rgba(16716806, a * 0.85F), LimitlessMesh.rgba(16716806, 0.0F), t * 0.3F);
         light.ringFlat(c, uu, rBlue, 0.1F + 0.08F * out, 64, LimitlessMesh.rgba(4633855, a * 0.85F), LimitlessMesh.rgba(4633855, 0.0F), -t * 0.3F);
         float heart = a * (1.0F - Ease.ramp(t, 1.0F, 5.0F));
         if (heart > 0.004F) {
            light.disc(
               c,
               0.5F + 0.4F * out,
               20,
               LimitlessMesh.mix(LimitlessMesh.rgba(13168895, heart), LimitlessMesh.rgba(16758934, heart), u * 2.0F),
               LimitlessMesh.rgba(16716806, 0.0F)
            );
         }

         f.end(FxLayer.LIGHT);
      }
   }
}
