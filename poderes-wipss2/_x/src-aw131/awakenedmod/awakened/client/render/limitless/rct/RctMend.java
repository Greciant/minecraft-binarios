package com.awakenedmod.awakened.client.render.limitless.rct;

import com.awakenedmod.awakened.client.render.FxLayer;
import com.awakenedmod.awakened.client.render.limitless.Ease;
import com.awakenedmod.awakened.client.render.limitless.LimitlessComfort;
import com.awakenedmod.awakened.client.render.limitless.LimitlessFrame;
import com.awakenedmod.awakened.client.render.limitless.LimitlessLook;
import com.awakenedmod.awakened.client.render.limitless.LimitlessMesh;
import com.awakenedmod.awakened.client.render.limitless.SpaceBend;
import com.awakenedmod.awakened.core.Lab;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class RctMend implements LimitlessLook<RctCue> {
   private static final String K = "limitless.rct.";
   private static final Lab.Value RINGS = Lab.CLIENT.def("limitless.rct.rings", 2.0, 0.0, 4.0, 1.0);
   private static final Lab.Value RING_PERIOD = Lab.CLIENT.def("limitless.rct.ring_period", 34.0, 10.0, 120.0, 1.0);
   private static final Lab.Value RING_ALPHA = Lab.CLIENT.def("limitless.rct.ring_alpha", 110.0, 0.0, 255.0, 1.0);
   private static final Lab.Value RING_WIDTH = Lab.CLIENT.def("limitless.rct.ring_width", 0.035, 0.005, 0.2, 0.005);
   private static final Lab.Value RING_RADIUS = Lab.CLIENT.def("limitless.rct.ring_radius", 0.55, 0.2, 1.2, 0.01);
   private static final Lab.Value MOTES = Lab.CLIENT.def("limitless.rct.motes", 10.0, 0.0, 40.0, 1.0);
   private static final Lab.Value MOTE_PERIOD = Lab.CLIENT.def("limitless.rct.mote_period", 40.0, 10.0, 120.0, 1.0);
   private static final Lab.Value MOTE_SIZE = Lab.CLIENT.def("limitless.rct.mote_size", 0.035, 0.005, 0.15, 0.005);
   private static final Lab.Value MOTE_ALPHA = Lab.CLIENT.def("limitless.rct.mote_alpha", 170.0, 0.0, 255.0, 1.0);
   private static final Lab.Value GLOW_ALPHA = Lab.CLIENT.def("limitless.rct.glow_alpha", 60.0, 0.0, 255.0, 1.0);
   private static final Lab.Value GLOW_SIZE = Lab.CLIENT.def("limitless.rct.glow_size", 0.32, 0.05, 1.0, 0.01);
   private static final Lab.Value GLOW_PERIOD = Lab.CLIENT.def("limitless.rct.glow_period", 40.0, 10.0, 120.0, 1.0);
   private static final int PALE = 15267583;
   private static final int SOFT = 10409727;
   private static final int CORE = 16777215;
   private static final float OWN_TOP = 0.55F;
   private static final Vector3f UP = new Vector3f(0.0F, 1.0F, 0.0F);

   public void draw(LimitlessFrame f, RctCue cue, float age) {
      float presence = Ease.clamp01(cue.presence());
      if (!(presence <= 0.0F)) {
         Vec3 feet = cue.feet();
         float h = cue.height();
         float top = h * (cue.ownView() ? 0.55F : 0.95F);
         LimitlessMesh m = f.begin(FxLayer.LIGHT);
         rings(f, m, feet, h, top, age, presence);
         motes(f, m, feet, h, top, age, presence, cue.seed());
         if (!cue.ownView()) {
            glow(f, m, feet, h, age, presence);
         }

         f.end(FxLayer.LIGHT);
      }
   }

   private static void rings(LimitlessFrame f, LimitlessMesh m, Vec3 feet, float h, float top, float age, float presence) {
      int n = RINGS.getI();
      float period = RING_PERIOD.getF();
      float radius = RING_RADIUS.getF();
      float half = RING_WIDTH.getF();
      float alpha = RING_ALPHA.getF() / 255.0F * presence;
      float bottom = h * 0.08F;

      for (int i = 0; i < n; i++) {
         float t = Ease.frac(age / period + (float)i / (float)n);
         float env = (float)Math.sin(Math.PI * (double)t);
         Vec3 at = feet.add(0.0, (double)(bottom + (top - bottom) * t), 0.0);
         m.ringFlat(f.rel(at), UP, radius * (1.0F - 0.22F * t), half, 40, LimitlessMesh.rgba(15267583, alpha * env), LimitlessMesh.rgba(10409727, 0.0F), 0.0F);
      }
   }

   private static void motes(LimitlessFrame f, LimitlessMesh m, Vec3 feet, float h, float top, float age, float presence, long seed) {
      int n = MOTES.getI();
      float period = MOTE_PERIOD.getF();
      float size = MOTE_SIZE.getF();
      float alpha = MOTE_ALPHA.getF() / 255.0F * presence;
      float bottom = h * 0.1F;

      for (int i = 0; i < n; i++) {
         long si = seed * 977L + (long)i;
         float p = period * (0.8F + 0.4F * SpaceBend.hash(si, 1));
         float tau = Ease.frac((age + SpaceBend.hash(si, 2) * p) / p);
         float env = (float)Math.sin(Math.PI * (double)tau);
         double ang = (double)SpaceBend.hash(si, 3) * Math.PI * 2.0 + (double)age * 0.01;
         double r = (0.32 + 0.18 * (double)SpaceBend.hash(si, 4)) * (1.0 - 0.4 * (double)tau);
         Vec3 at = feet.add(Math.cos(ang) * r, (double)(bottom + (top - bottom) * tau), Math.sin(ang) * r);
         Vector3f rel = f.rel(at);
         m.disc(rel, size * 2.5F, 8, LimitlessMesh.rgba(10409727, 0.25F * alpha * env), LimitlessMesh.rgba(10409727, 0.0F));
         m.disc(rel, size, 8, LimitlessMesh.rgba(16777215, alpha * env), LimitlessMesh.rgba(15267583, 0.0F));
      }
   }

   private static void glow(LimitlessFrame f, LimitlessMesh m, Vec3 feet, float h, float age, float presence) {
      float breath = 1.0F + 0.12F * (float)Math.sin((double)age * Math.PI * 2.0 / (double)GLOW_PERIOD.getF()) * LimitlessComfort.distortion(f.mc);
      Vector3f chest = f.rel(feet.add(0.0, (double)h * 0.62, 0.0));
      float d = chest.length();
      if (d > 0.001F) {
         chest.mul(1.0F - Math.min(0.25F, d * 0.5F) / d);
      }

      float a = GLOW_ALPHA.getF() / 255.0F * presence;
      m.disc(chest, GLOW_SIZE.getF() * breath, 20, LimitlessMesh.rgba(15267583, a), LimitlessMesh.rgba(10409727, 0.0F));
   }
}
