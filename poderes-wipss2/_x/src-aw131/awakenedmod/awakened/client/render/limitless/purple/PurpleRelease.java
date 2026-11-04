package com.awakenedmod.awakened.client.render.limitless.purple;

import com.awakenedmod.awakened.client.render.limitless.Ease;
import com.awakenedmod.awakened.client.render.limitless.LimitlessFrame;
import com.awakenedmod.awakened.client.render.limitless.LimitlessMesh;
import com.awakenedmod.awakened.client.render.limitless.SpaceBend;
import com.awakenedmod.awakened.core.Lab;
import org.joml.Vector3f;

final class PurpleRelease {
   private static final String K = "limitless.purple.";
   private static final Lab.Value PULL_STREAKS = Lab.CLIENT.def("limitless.purple.pull_streaks", 22.0, 0.0, 60.0, 1.0);
   private static final Lab.Value PULL_REACH = Lab.CLIENT.def("limitless.purple.pull_reach", 2.4, 0.5, 6.0, 0.1);
   private static final Lab.Value PULL_ALPHA = Lab.CLIENT.def("limitless.purple.pull_alpha", 0.75, 0.0, 1.0, 0.01);
   private static final Lab.Value PULSE_ALPHA = Lab.CLIENT.def("limitless.purple.pulse_alpha", 0.6, 0.0, 1.0, 0.01);
   private static final Lab.Value RING_REACH = Lab.CLIENT.def("limitless.purple.ring_reach", 7.0, 1.0, 16.0, 0.1);
   private static final Lab.Value RING_LIFE = Lab.CLIENT.def("limitless.purple.ring_life", 14.0, 4.0, 30.0, 1.0);
   private static final Lab.Value RING_ALPHA = Lab.CLIENT.def("limitless.purple.ring_alpha", 0.6, 0.0, 1.0, 0.01);
   private static final Lab.Value RING_PAINT = Lab.CLIENT.def("limitless.purple.ring_paint", 0.42, 0.0, 1.0, 0.01);
   private static final Lab.Value SHOCK_REACH = Lab.CLIENT.def("limitless.purple.shock_reach", 3.6, 0.5, 10.0, 0.1);
   private static final Lab.Value SHOCK_LIFE = Lab.CLIENT.def("limitless.purple.shock_life", 10.0, 3.0, 24.0, 1.0);
   private static final Lab.Value SHOCK_ALPHA = Lab.CLIENT.def("limitless.purple.shock_alpha", 0.4, 0.0, 1.0, 0.01);
   private static final int AIR = 14206207;
   private static final int VIO = 10113279;
   private static final int VIO_HOT = 15128319;
   private static final int SEGS = 80;

   void pullIn(LimitlessMesh m, LimitlessFrame f, PurpleBeats b, float age, float motion, float ballR, long seed, float presence) {
      boolean flying = PurpleBeats.released(age);
      float pull = flying ? 1.0F : Ease.smooth(b.pullIn);
      float k = pull * (1.0F - Ease.ramp(age, 50.0F, 3.0F)) * PULL_ALPHA.getF() * presence;
      if (!(k <= 0.003F)) {
         float scale = PurpleBallLook.scale(b);
         Vector3f c = f.rel(b.merge);
         float reach = PULL_REACH.getF() * scale;
         int n = PULL_STREAKS.getI();
         Vector3f d = new Vector3f();

         for (int i = 0; i < n; i++) {
            long si = seed * 6151L + (long)i;
            float z = SpaceBend.hash(si, 1) * 2.0F - 1.0F;
            float az = SpaceBend.hash(si, 2) * 6.2832F;
            float sxy = (float)Math.sqrt((double)(1.0F - z * z));
            d.set(sxy * (float)Math.cos((double)az), z, sxy * (float)Math.sin((double)az));
            float period = 4.0F + 3.0F * SpaceBend.hash(si, 3);
            float u = Ease.frac(motion / period + SpaceBend.hash(si, 4));
            float head = ballR * 1.05F + reach * (1.0F - u) * (1.0F - u);
            float tail = head + reach * 0.35F * (1.0F - 0.5F * u);
            float env = (float)Math.sin(Math.PI * (double)u);
            m.streak(
               new Vector3f(d).mul(tail).add(c),
               new Vector3f(d).mul(head).add(c),
               0.0F,
               0.03F * scale,
               LimitlessMesh.rgba(10113279, 0.0F),
               LimitlessMesh.rgba(15128319, k * env)
            );
         }

         float rr = ballR + reach * (1.0F - pull) * 0.9F + 0.1F;
         m.ring(c, rr, 0.06F * scale + ballR * 0.1F, 64, LimitlessMesh.rgba(15128319, 0.45F * k), LimitlessMesh.rgba(10113279, 0.0F));
         m.disc(c, ballR * 3.2F, 32, LimitlessMesh.rgba(10113279, 0.25F * k * PurpleBallLook.nearFade(c, ballR * 3.2F)), 0);
      }
   }

   void pulses(LimitlessMesh m, Vector3f c, float r, float held, float presence) {
      int beats = 4;
      if (!(held <= 0.0F)) {
         int j = (int)Math.floor((double)held);
         if (j < beats) {
            float u = held - (float)j;
            float amp = (float)(j + 1) / (float)beats;
            float a = PULSE_ALPHA.getF() * amp * Ease.ramp(u, 0.0F, 0.3F) * (1.0F - u) * (1.0F - u) * presence;
            float rr = r * (1.15F + (1.4F + 0.5F * (float)j) * Ease.out(u, 2.0F));
            m.ring(c, rr, r * (0.06F + 0.12F * u), 64, LimitlessMesh.rgba(15128319, a), LimitlessMesh.rgba(10113279, 0.0F));
            m.ring(c, rr * 0.92F, r * (0.2F + 0.2F * u), 48, LimitlessMesh.rgba(10113279, 0.45F * a), 0);
         }
      }
   }

   private static float ringU(float age) {
      float u = (age - 50.0F) / RING_LIFE.getF();
      return !(u < 0.0F) && !(u >= 1.0F) ? u : -1.0F;
   }

   private static float ringA(float age, float u) {
      return Ease.ramp(age, 50.0F, 2.0F) * (float)Math.pow((double)(1.0F - u), 1.6F);
   }

   private static float ringR(float u, float scale) {
      return 0.4F + RING_REACH.getF() * scale * Ease.out(u, 3.5F);
   }

   void paintRing(LimitlessMesh m, LimitlessFrame f, PurpleBeats b, float age) {
      float scale = PurpleBallLook.scale(b);
      float u = ringU(age);
      if (!(u < 0.0F)) {
         float a = RING_PAINT.getF() * ringA(age, u);
         Vector3f normal = new Vector3f((float)b.fwd.x, (float)b.fwd.y, (float)b.fwd.z);
         float r = ringR(u, scale);
         m.ringFlat(f.rel(b.merge), normal, r, (0.15F + 0.7F * u) * scale, 80, LimitlessMesh.rgba(14206207, a), LimitlessMesh.rgba(14206207, 0.0F), 0.0F);
      }
   }

   void lightRing(LimitlessMesh m, LimitlessFrame f, PurpleBeats b, float age) {
      float scale = PurpleBallLook.scale(b);
      Vector3f c = f.rel(b.merge);
      float u = ringU(age);
      if (u >= 0.0F) {
         float a = RING_ALPHA.getF() * ringA(age, u);
         Vector3f normal = new Vector3f((float)b.fwd.x, (float)b.fwd.y, (float)b.fwd.z);
         float r = ringR(u, scale);
         m.ringFlat(c, normal, r, (0.08F + 0.3F * u) * scale, 80, LimitlessMesh.rgba(10113279, a), LimitlessMesh.rgba(10113279, 0.0F), 0.0F);
         m.ringFlat(c, normal, r, 0.04F * scale, 80, LimitlessMesh.rgba(15128319, 0.8F * a), LimitlessMesh.rgba(15128319, 0.0F), 0.0F);
      }

      float t = age - 50.0F;
      float us = t / SHOCK_LIFE.getF();
      if (us >= 0.0F && us < 1.0F) {
         float a = SHOCK_ALPHA.getF() * Ease.ramp(t, 0.0F, 2.0F) * (1.0F - us) * (1.0F - us);
         float r = 0.4F + SHOCK_REACH.getF() * scale * Ease.out(us, 2.0F);
         m.sphere(c, r, 14, 36, (n, facing) -> LimitlessMesh.rgba(15128319, a * (float)Math.pow((double)(1.0F - Math.max(0.0F, facing)), 2.5)));
      }
   }
}
