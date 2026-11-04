package com.awakenedmod.awakened.client.render.limitless.voidlook;

import com.awakenedmod.awakened.client.render.FxLayer;
import com.awakenedmod.awakened.client.render.limitless.FrozenGaze;
import com.awakenedmod.awakened.client.render.limitless.LimitlessMesh;
import com.awakenedmod.awakened.client.render.limitless.StarField;
import com.awakenedmod.awakened.client.render.limitless.VoidSky;
import com.awakenedmod.awakened.core.Lab;
import org.joml.Vector3f;

public final class VoidGalaxy extends VoidLookBase {
   private static final String K = "limitless.void.";
   private static final Lab.Value BAND = Lab.CLIENT.def("limitless.void.band", 1.0, 0.0, 3.0, 0.05);
   private static final Lab.Value BAND_TILT = Lab.CLIENT.def("limitless.void.band_tilt", 0.3, -1.5, 1.5, 0.01);
   private static final Lab.Value STARS = Lab.CLIENT.def("limitless.void.stars", 1.0, 0.0, 2.0, 0.05);
   private static final Lab.Value STREAMS = Lab.CLIENT.def("limitless.void.streams", 26.0, 0.0, 60.0, 1.0);
   private static final Lab.Value STREAM_ALPHA = Lab.CLIENT.def("limitless.void.stream_alpha", 0.75, 0.0, 1.0, 0.01);
   private static final Lab.Value RING_ANG = Lab.CLIENT.def("limitless.void.ring_size", 0.82, 0.1, 1.3, 0.01);
   private static final Lab.Value RING_UP = Lab.CLIENT.def("limitless.void.ring_up", -0.78, -1.2, 0.8, 0.01);
   private static final Lab.Value EYE_LEN = Lab.CLIENT.def("limitless.void.eye_len", 2.4, 0.5, 6.0, 0.1);
   private static final Lab.Value RIM = Lab.CLIENT.def("limitless.void.rim", 0.4, 0.0, 1.0, 0.01);
   private static final long LAYOUT = 6953669L;
   private static final int CLOUDS = 260;
   private final float[] cloud = new float[780];
   private final float[] cloudSize = new float[260];
   private final int[] cloudRgb = new int[260];
   private final float[] cloudA = new float[260];
   private static final int SKY_RINGS = 64;
   private static final int SKY_SEGS = 128;
   private final int[] bandSky = new int[8385];
   private StarField stars;
   private float starsTilt = Float.NaN;
   private final Vector3f starsFwd = new Vector3f();

   public VoidGalaxy() {
      for (int i = 0; i < 260; i++) {
         float lon = h(6953669L, i * 4) * (float) (Math.PI * 2);
         float lat = (float)gauss(h(6953669L, i * 4 + 1), h(6953669L, i * 4 + 2)) * 0.09F;
         this.cloud[i * 3] = lon;
         this.cloud[i * 3 + 1] = lat;
         float big = h(6953669L, i * 4 + 3);
         this.cloudSize[i] = 0.05F + 0.16F * big * big;
         float t = h(6953669L, 2000 + i);
         this.cloudRgb[i] = t < 0.45F ? 13161712 : (t < 0.75F ? 15787216 : (t < 0.92F ? 9414888 : 15255768));
         this.cloudA[i] = 0.04F + 0.09F * h(6953669L, 3000 + i);
      }
   }

   private static double gauss(float a, float b) {
      return Math.sqrt(-2.0 * Math.log(Math.max(1.0E-6, (double)a))) * Math.cos((Math.PI * 2) * (double)b);
   }

   @Override
   protected Vector3f ringDir(VoidLookBase.Ctx c) {
      return c.dir(0.0F, RING_UP.getF());
   }

   @Override
   protected int closeGlow() {
      return 9416959;
   }

   private Vector3f pole(VoidLookBase.Ctx c) {
      float t = BAND_TILT.getF();
      return new Vector3f(0.0F, cos(t), 0.0F).fma(sin(t), c.right).fma(0.15F, c.forward).normalize();
   }

   private static Vector3f onBand(Vector3f pole, Vector3f a, Vector3f b, float lon, float lat, Vector3f out) {
      float cl = cos(lat);
      return out.set(a).mul(cos(lon) * cl).fma(sin(lon) * cl, b).fma(sin(lat), pole).normalize();
   }

   @Override
   protected void inside(VoidLookBase.Ctx c) {
      float sky = c.open * c.hold;
      VoidSky s = c.sky;
      Vector3f pole = this.pole(c);
      Vector3f[] ax = LimitlessMesh.planeAxes(pole);
      float tilt = BAND_TILT.getF();
      if (this.stars == null || tilt != this.starsTilt || !this.starsFwd.equals(c.forward)) {
         this.stars = StarField.build(
            6953669L,
            6000,
            StarField.band(pole, 0.11F, 0.72F),
            9.0E-4F,
            0.0036F,
            3.5F,
            0.2F,
            new int[]{16777215, 14477055, 16772050, 11060479},
            new float[]{5.0F, 3.0F, 2.0F, 1.0F}
         );
         this.starsTilt = tilt;
         this.starsFwd.set(c.forward);
         this.paintBand(pole, ax);
      }

      float band = BAND.getF() * sky;
      Vector3f ring = this.ringDir(c);
      float ang = RING_ANG.getF();
      LimitlessMesh light = c.f.begin(FxLayer.LIGHT);
      s.dome(light, 64, 128, this.bandSky, band, 0.985F);
      this.stars.draw(light, s, c.time, sky * STARS.getF(), 0.3F, 1.0F, 0.985F);
      c.f.end(FxLayer.LIGHT);
      eclipse(c, ring, ang, -16777216, -16777216);
      light = c.f.begin(FxLayer.LIGHT);
      s.ring(light, ring, ang - 0.005F, ang, ang + 0.005F, 220, col(16777215, 0.0F), col(16775408, sky), col(16777215, 0.0F), 0.97F);
      s.ring(light, ring, ang - 0.02F, ang + 0.003F, ang + 0.07F, 200, col(10469631, 0.0F), col(12112127, 0.5F * sky), col(6258912, 0.0F), 0.972F);
      s.ring(light, ring, ang, ang + 0.03F, ang + 0.35F, 160, col(7311584, 0.0F), col(7311584, 0.12F * sky), col(2767488, 0.0F), 0.98F);
      this.streams(c, light, pole, sky);
      float eyes = sky * c.gazeIn;

      for (int i = 0; i < c.gazeCount; i++) {
         FrozenGaze g = c.gazes[i];
         light.disc(g.left, g.head * 0.3F, 10, col(16054527, 0.85F * eyes), col(9416959, 0.0F));
         light.disc(g.right, g.head * 0.3F, 10, col(16054527, 0.85F * eyes), col(9416959, 0.0F));
         this.eyeStream(
            light,
            g,
            g.left,
            EYE_LEN.getF(),
            0.7F,
            0.18F,
            0.03F,
            col(16054527, 0.9F * eyes),
            col(8366335, 0.5F * eyes),
            26,
            2.5F,
            0.02F,
            c.time,
            (float)i * 1.3F
         );
         this.eyeStream(
            light,
            g,
            g.right,
            EYE_LEN.getF(),
            0.75F,
            0.18F,
            0.03F,
            col(16054527, 0.9F * eyes),
            col(8366335, 0.5F * eyes),
            26,
            2.5F,
            0.02F,
            c.time,
            (float)i * 1.3F + 2.0F
         );
      }

      c.f.end(FxLayer.LIGHT);
   }

   private void paintBand(Vector3f pole, Vector3f[] ax) {
      Vector3f d = new Vector3f();
      Vector3f[] cd = new Vector3f[260];

      for (int k = 0; k < 260; k++) {
         cd[k] = onBand(pole, ax[0], ax[1], this.cloud[k * 3], this.cloud[k * 3 + 1], new Vector3f());
      }

      for (int i = 0; i <= 64; i++) {
         for (int j = 0; j <= 128; j++) {
            VoidSky.domeDir(64, 128, i, j, d);
            float lat = (float)Math.asin((double)Math.max(-1.0F, Math.min(1.0F, d.dot(pole))));
            float glow = (float)(Math.exp(-((double)lat / 0.16) * ((double)lat / 0.16)) * 0.85 + Math.exp(-((double)lat / 0.45) * ((double)lat / 0.45)) * 0.25);
            float lane = (float)Math.exp(-(((double)lat - 0.015) / 0.035) * (((double)lat - 0.015) / 0.035));
            float k0 = 0.32F * glow * (1.0F - 0.75F * lane);
            float r = k0 * 150.0F / 255.0F;
            float g = k0 * 165.0F / 255.0F;
            float b = k0 * 215.0F / 255.0F;

            for (int k = 0; k < 260; k++) {
               float z = d.dot(cd[k]);
               if (!(z < 0.9F)) {
                  float ang2 = 2.0F * (1.0F - z);
                  float w = (float)Math.exp((double)(-ang2 / (this.cloudSize[k] * this.cloudSize[k]) * 1.6F)) * this.cloudA[k] * (1.0F - 0.6F * lane);
                  int c = this.cloudRgb[k];
                  r += (float)(c >> 16 & 0xFF) / 255.0F * w;
                  g += (float)(c >> 8 & 0xFF) / 255.0F * w;
                  b += (float)(c & 0xFF) / 255.0F * w;
               }
            }

            this.bandSky[i * 129 + j] = VoidSky.light(r, g, b);
         }
      }
   }

   private void streams(VoidLookBase.Ctx c, LimitlessMesh light, Vector3f pole, float sky) {
      int count = STREAMS.getI();
      float alpha = STREAM_ALPHA.getF() * sky;
      Vector3f n = new Vector3f();
      Vector3f u = new Vector3f();
      Vector3f v = new Vector3f();
      Vector3f centre = new Vector3f();
      int np = 40;

      for (int i = 0; i < count; i++) {
         Vector3f[] pa = LimitlessMesh.planeAxes(pole);
         float lean = (h(6953669L, 700 + i * 5) - 0.5F) * 0.5F;
         n.set(pole).fma(lean, pa[0]).normalize();
         Vector3f[] ax = LimitlessMesh.planeAxes(n);
         u.set(ax[0]);
         v.set(ax[1]);
         float r = (3.5F + 7.5F * h(6953669L, 701 + i * 5)) * c.left;
         centre.set(c.centreRel).fma((h(6953669L, 702 + i * 5) - 0.5F) * 5.0F * c.left, n);
         float span = 1.2F + 1.4F * h(6953669L, 703 + i * 5);
         float a0 = h(6953669L, 704 + i * 5) * (float) (Math.PI * 2) + c.time * 0.006F;
         boolean wide = i % 4 == 0;
         int rgb = i % 3 == 0 ? 15791359 : 10272511;
         float ph = (float)i * 1.91F;

         for (int k = 0; k < np; k++) {
            float sk = (float)k / (float)(np - 1);
            onArc(centre, u, v, r, a0 + span * sk, this.pts[k]);
            float ends = sin(sk * (float) Math.PI);
            if (wide) {
               this.widths[k] = (0.3F * ends + 0.02F) * c.left;
               this.colors[k] = col(6982888, 0.08F * alpha * ends);
            } else {
               this.widths[k] = 0.016F * ends + 0.003F;
               this.colors[k] = col(rgb, alpha * ends * (0.2F + 0.8F * beads(sk, c.time, 1.6F, 0.01F, 4.0F, ph)));
            }
         }

         this.stroke(light, np);
      }
   }

   @Override
   protected void outside(VoidLookBase.Ctx c) {
      Vector3f pole = this.pole(c);
      float rim = RIM.getF();
      LimitlessMesh light = c.f.begin(FxLayer.LIGHT);
      light.sphere(c.centreRel, c.radius * 1.004F, 32, 64, (nrm, facing) -> {
         float lat = nrm.dot(pole);
         float band = (float)Math.exp(-((double)lat / 0.12) * ((double)lat / 0.12));
         float e = 1.0F - Math.max(0.0F, facing);
         return col(9413856, rim * (0.35F * band + e * e * e * e * (0.4F + 0.6F * band)));
      });
      c.f.end(FxLayer.LIGHT);
   }
}
