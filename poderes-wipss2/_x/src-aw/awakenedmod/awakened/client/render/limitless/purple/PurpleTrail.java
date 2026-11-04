package com.awakenedmod.awakened.client.render.limitless.purple;

import com.awakenedmod.awakened.client.render.limitless.Ease;
import com.awakenedmod.awakened.client.render.limitless.LimitlessFrame;
import com.awakenedmod.awakened.client.render.limitless.LimitlessMesh;
import com.awakenedmod.awakened.client.render.limitless.SpaceBend;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Lab;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

final class PurpleTrail {
   private static final String K = "limitless.purple.";
   private static final Lab.Value HANG = Lab.CLIENT.def("limitless.purple.streak_hang", 20.0, 4.0, 60.0, 1.0);
   private static final Lab.Value WIDTH = Lab.CLIENT.def("limitless.purple.streak_width", 0.42, 0.05, 1.0, 0.01);
   private static final Lab.Value ALPHA = Lab.CLIENT.def("limitless.purple.streak_alpha", 0.45, 0.0, 1.0, 0.01);
   private static final Lab.Value PAINT_ALPHA = Lab.CLIENT.def("limitless.purple.streak_paint", 0.55, 0.0, 1.0, 0.01);
   private static final Lab.Value STRANDS = Lab.CLIENT.def("limitless.purple.streak_strands", 7.0, 0.0, 20.0, 1.0);
   private static final Lab.Value DISC_ALPHA = Lab.CLIENT.def("limitless.purple.streak_discs", 0.14, 0.0, 0.4, 0.005);
   private static final Lab.Value DISC_PAINT = Lab.CLIENT.def("limitless.purple.streak_disc_paint", 0.08, 0.0, 0.4, 0.005);
   private static final Lab.Value DUST = Lab.CLIENT.def("limitless.purple.streak_dust", 7.0, 0.0, 40.0, 1.0);
   private static final Lab.Value WAKE_EVERY = Lab.CLIENT.def("limitless.purple.wake_every", 7.0, 7.0, 20.0, 1.0);
   private static final Lab.Value WAKE_LIFE = Lab.CLIENT.def("limitless.purple.wake_life", 16.0, 4.0, 40.0, 1.0);
   private static final Lab.Value WAKE_ALPHA = Lab.CLIENT.def("limitless.purple.wake_alpha", 0.55, 0.0, 1.0, 0.01);
   private static final Lab.Value WAKE_GROW = Lab.CLIENT.def("limitless.purple.wake_grow", 1.5, 0.0, 4.0, 0.05);
   private static final Lab.Value CUT_LIFE = Lab.CLIENT.def("limitless.purple.cut_life", 72.0, 10.0, 72.0, 1.0);
   private static final Lab.Value CUT_ALPHA = Lab.CLIENT.def("limitless.purple.cut_alpha", 235.0, 0.0, 255.0, 1.0);
   private static final int AIR = 14206207;
   private static final int VIO = 10113279;
   private static final int VIO_DEEP = 5907136;
   private static final int VIO_HOT = 15128319;
   private static final int STRAND = 12159743;
   private static final int[] DUSK = new int[]{11563263, 10115312, 13214463, 14204159};
   private static final int SAMPLES = 40;
   private static final float DISC_STEP = 1.2F;
   private final Vector3f[] pts = new Vector3f[40];
   private final float[] widths = new float[40];
   private final int[] colors = new int[40];

   PurpleTrail() {
      for (int i = 0; i < 40; i++) {
         this.pts[i] = new Vector3f();
      }
   }

   private static float passed(PurpleBeats b, double d) {
      return 50.0F + (float)(d / b.strength.speed());
   }

   private static float lit(float since, float hang) {
      return Ease.ramp(since, 0.0F, 2.0F) * (1.0F - Ease.ramp(since, hang * 0.45F, hang * 0.55F));
   }

   private static float whole(double d, float since, float hang, long seed) {
      float breakup = Ease.ramp(since, hang * 0.3F, hang * 0.45F);
      if (breakup <= 0.0F) {
         return 1.0F;
      } else {
         float x = (float)(d / 0.7);
         int cell = (int)Math.floor((double)x);
         float fr = Ease.smooth(x - (float)cell);
         float noise = SpaceBend.hash(seed, 900 + cell) * (1.0F - fr) + SpaceBend.hash(seed, 901 + cell) * fr;
         return Ease.clamp01((noise - breakup * 0.95F) * 5.0F + (1.0F - breakup));
      }
   }

   private static double[] stretch(PurpleBeats b, float age, float hang) {
      double to = b.travelled(age);
      double speed = b.strength.speed();
      double from = Math.max(0.0, (double)(age - 50.0F - hang) * speed);
      return to - from < 0.05 ? null : new double[]{from, Math.min(to, from + (double)hang * speed)};
   }

   void paint(LimitlessMesh m, LimitlessFrame f, PurpleBeats b, float age, long seed, float calm) {
      float hang = HANG.getF();
      double[] span = stretch(b, age, hang);
      if (span != null) {
         float a = PAINT_ALPHA.getF();

         for (int i = 0; i < 40; i++) {
            double d = span[0] + (span[1] - span[0]) * (double)i / 39.0;
            float since = age - passed(b, d);
            float k = lit(since, hang) * whole(d, since, hang, seed);
            this.pts[i].set(f.rel(b.merge, b.fwd, d));
            this.widths[i] = width(b, d, since, hang);
            this.colors[i] = LimitlessMesh.rgba(5907136, a * k);
         }

         m.ribbon(this.pts, this.widths, this.colors, 40);
         discs(m, f, b, age, span, hang, seed, 5907136, DISC_PAINT.getF());
      }

      wake(m, f, b, age, true, calm);
   }

   void light(LimitlessMesh m, LimitlessFrame f, PurpleBeats b, float age, long seed, float calm) {
      float hang = HANG.getF();
      double[] span = stretch(b, age, hang);
      if (span != null) {
         float a = ALPHA.getF();

         for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < 40; i++) {
               double d = span[0] + (span[1] - span[0]) * (double)i / 39.0;
               float since = age - passed(b, d);
               float k = lit(since, hang) * whole(d, since, hang, seed);
               this.pts[i].set(f.rel(b.merge, b.fwd, d));
               float w = width(b, d, since, hang);
               this.widths[i] = pass == 0 ? w * 0.85F : w * 0.22F;
               this.colors[i] = pass == 0 ? LimitlessMesh.rgba(10113279, a * k) : LimitlessMesh.rgba(15128319, 0.5F * a * k);
            }

            m.ribbon(this.pts, this.widths, this.colors, 40);
         }

         this.strands(m, f, b, age, span, hang, seed, a);
         discs(m, f, b, age, span, hang, seed, 10113279, DISC_ALPHA.getF());
      }

      wake(m, f, b, age, false, calm);
      b.cutLine(f, m, age, 0.06F, LimitlessMesh.rgba(14201087, CUT_ALPHA.getF() / 255.0F), CUT_LIFE.getF());
   }

   private static float width(PurpleBeats b, double d, float since, float hang) {
      return WIDTH.getF() * b.radius(passed(b, d)) * (1.0F - 0.45F * Ease.ramp(since, 0.0F, hang));
   }

   private void strands(LimitlessMesh m, LimitlessFrame f, PurpleBeats b, float age, double[] span, float hang, long seed, float a) {
      int n = STRANDS.getI();
      Vec3 ref = Math.abs(b.fwd.y) < 0.95 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0);
      Vec3 u = ref.cross(b.fwd).normalize();
      Vec3 v = b.fwd.cross(u).normalize();

      for (int s = 0; s < n; s++) {
         long ss = seed * 4241L + (long)s;
         float az0 = ((float)s + 0.5F * SpaceBend.hash(ss, 1)) / (float)n * 6.2832F;
         float twist = (SpaceBend.hash(ss, 2) - 0.5F) * 0.08F;
         float out = 0.45F + 0.35F * SpaceBend.hash(ss, 3);

         for (int i = 0; i < 40; i++) {
            double d = span[0] + (span[1] - span[0]) * (double)i / 39.0;
            float since = age - passed(b, d);
            float k = lit(since, hang) * whole(d, since, hang, ss);
            float rad = b.radius(passed(b, d));
            float off = rad * out * (1.0F + 0.25F * Ease.ramp(since, 0.0F, hang));
            double az = (double)az0 + (double)twist * d;
            Vec3 p = b.merge.add(b.fwd.scale(d)).add(u.scale(Math.cos(az) * (double)off)).add(v.scale(Math.sin(az) * (double)off));
            this.pts[i].set(f.rel(p));
            this.widths[i] = rad * 0.025F;
            this.colors[i] = LimitlessMesh.rgba(12159743, a * k);
         }

         m.ribbon(this.pts, this.widths, this.colors, 40);
      }
   }

   private static void discs(LimitlessMesh m, LimitlessFrame f, PurpleBeats b, float age, double[] span, float hang, long seed, int rgb, float a) {
      if (!(a <= 0.0F)) {
         Vector3f normal = new Vector3f((float)b.fwd.x, (float)b.fwd.y, (float)b.fwd.z);

         for (double d = Math.ceil(span[0] / 1.2F) * 1.2F; d <= span[1]; d++) {
            float since = age - passed(b, d);
            float k = lit(since, hang) * whole(d, since, hang, seed);
            Vector3f c = f.rel(b.merge, b.fwd, d);
            float near = Ease.ramp(c.length(), 1.5F, 2.5F);
            float r = width(b, d, since, hang) * 1.1F;
            if (k * near > 0.003F) {
               m.ringFlat(c, normal, r * 0.5F, r * 0.5F, 36, LimitlessMesh.rgba(rgb, a * k * near), LimitlessMesh.rgba(rgb, 0.0F), 0.0F);
            }
         }
      }
   }

   private static void wake(LimitlessMesh m, LimitlessFrame f, PurpleBeats b, float age, boolean paint, float calm) {
      float alpha = WAKE_ALPHA.getF() * calm;
      float travel = age - 50.0F;
      int every = WAKE_EVERY.getI();
      float life = WAKE_LIFE.getF();
      if (!(alpha <= 0.0F) && !(travel <= 0.0F)) {
         Vector3f normal = new Vector3f((float)b.fwd.x, (float)b.fwd.y, (float)b.fwd.z);
         int first = Math.max(1, (int)Math.ceil((double)((travel - life) / (float)every)));

         for (int k = first; (float)(k * every) <= travel; k++) {
            float born = (float)(50 + k * every);
            if (born > (float)b.gone) {
               break;
            }

            float t = age - born;
            float u = t / life;
            if (!(u >= 1.0F)) {
               float r0w = b.radius(born);
               float r = r0w * (1.0F + WAKE_GROW.getF() * Ease.out(u, 2.0F));
               float a = alpha * Ease.ramp(t, 0.0F, 3.0F) * (1.0F - u) * (1.0F - u);
               Vector3f c = f.rel(b.centre(born));
               if (paint) {
                  m.ringFlat(c, normal, r, r0w * (0.12F + 0.2F * u), 64, LimitlessMesh.rgba(14206207, 0.45F * a), LimitlessMesh.rgba(14206207, 0.0F), 0.0F);
               } else {
                  m.ringFlat(c, normal, r, r0w * (0.1F + 0.18F * u), 64, LimitlessMesh.rgba(10113279, a), LimitlessMesh.rgba(10113279, 0.0F), 0.0F);
                  m.ringFlat(c, normal, r, r0w * 0.025F, 64, LimitlessMesh.rgba(15128319, 0.7F * a), LimitlessMesh.rgba(15128319, 0.0F), 0.0F);
               }
            }
         }
      }
   }

   void tick(ClientLevel level, PurpleBeats b, int age) {
      float hang = HANG.getF();
      double[] span = stretch(b, (float)age, hang);
      int n = Math.round((float)DUST.getI() * PurpleBallLook.scale(b));
      if (span != null && n > 0) {
         Vec3 ref = Math.abs(b.fwd.y) < 0.95 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0);
         Vec3 u = ref.cross(b.fwd).normalize();
         Vec3 v = b.fwd.cross(u).normalize();
         double speed = b.strength.speed();
         double near = b.travelled((float)age) - (double)hang * 0.3 * speed;
         double far = Math.max(span[0], b.travelled((float)age) - (double)hang * 0.9 * speed);
         if (!(near <= far)) {
            for (int i = 0; i < n; i++) {
               double d = far + (near - far) * level.random.nextDouble();
               float rad = b.radius(passed(b, d)) * WIDTH.getF();
               double az = level.random.nextDouble() * Math.PI * 2.0;
               double off = (double)rad * Math.sqrt(level.random.nextDouble());
               Vec3 side = u.scale(Math.cos(az)).add(v.scale(Math.sin(az)));
               Vec3 p = b.merge.add(b.fwd.scale(d)).add(side.scale(off));
               int col = DUSK[level.random.nextInt(DUSK.length)];
               Particle part = Minecraft.getInstance()
                  .particleEngine
                  .createParticle(Fx.dust(col, 0.8F + 0.6F * level.random.nextFloat()), p.x, p.y, p.z, 0.0, 0.0, 0.0);
               if (part != null) {
                  Vec3 vel = side.scale(0.02 + 0.03 * level.random.nextDouble()).add(0.0, -0.004, 0.0);
                  part.setParticleSpeed(vel.x, vel.y, vel.z);
               }
            }
         }
      }
   }
}
