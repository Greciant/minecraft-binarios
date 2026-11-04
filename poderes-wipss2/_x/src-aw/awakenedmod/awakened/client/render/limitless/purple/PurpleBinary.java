package com.awakenedmod.awakened.client.render.limitless.purple;

import com.awakenedmod.awakened.client.render.limitless.Ease;
import com.awakenedmod.awakened.client.render.limitless.LimitlessFrame;
import com.awakenedmod.awakened.client.render.limitless.LimitlessMesh;
import com.awakenedmod.awakened.core.Lab;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class PurpleBinary {
   private static final String K = "limitless.purple.";
   private static final Lab.Value ORB = Lab.CLIENT.def("limitless.purple.orb", 0.24, 0.05, 1.0, 0.01);
   private static final Lab.Value TURNS = Lab.CLIENT.def("limitless.purple.turns", 3.2, 0.0, 10.0, 0.1);
   private static final Lab.Value TRAIL = Lab.CLIENT.def("limitless.purple.trail", 11.0, 1.0, 20.0, 0.5);
   private static final Lab.Value TRAIL_W = Lab.CLIENT.def("limitless.purple.trail_width", 0.12, 0.01, 0.4, 0.005);
   private static final Lab.Value TRAIL_ALPHA = Lab.CLIENT.def("limitless.purple.trail_alpha", 210.0, 0.0, 255.0, 1.0);
   private static final int BLUE = 2777855;
   private static final int BLUE_HOT = 10275839;
   private static final int RED = 16718390;
   private static final int RED_HOT = 16752298;
   private static final int VIO_GLOW = 9715967;
   private static final int SAMPLES = 18;
   private final Vector3f[] pts = new Vector3f[18];
   private final float[] widths = new float[18];
   private final int[] colors = new int[18];

   public PurpleBinary() {
      for (int i = 0; i < 18; i++) {
         this.pts[i] = new Vector3f();
      }
   }

   public static Vec3 orb(PurpleBeats b, float age, int side) {
      float drift = PurpleBeats.drift(age);
      float fuse = PurpleBeats.fuse(age);
      Vec3 handsMid = PurpleBeats.lerp(b.left, b.right, 0.5);
      Vec3 mid = PurpleBeats.lerp(handsMid, b.merge, (double)drift);
      double sep = b.left.distanceTo(b.right) * (double)(1.0F - 0.62F * (float)Math.pow((double)drift, 0.8)) * (double)(1.0F - fuse);
      double ang = (double)TURNS.getF() * Math.PI * 2.0 * (Math.pow((double)drift, 2.2) + 0.25 * (double)fuse);
      Vec3 across = b.across();
      Vec3 up = b.fwd.cross(across).normalize().scale(-1.0);
      Vec3 dir = across.scale(Math.cos(ang)).add(up.scale(Math.sin(ang)));
      return mid.add(dir.scale((double)side * sep * 0.5));
   }

   private static float orbRadius(float age, float form) {
      return ORB.getF() * (0.3F + 0.7F * form) * (1.0F - 0.5F * PurpleBeats.fuse(age));
   }

   public void paint(LimitlessMesh m, LimitlessFrame f, PurpleBeats b, float age, float form) {
      float fuse = PurpleBeats.fuse(age);
      float r = orbRadius(age, form);
      this.trail(m, f, b, age, -1, 2777855, 0.55F, 1.6F, form);
      this.trail(m, f, b, age, 1, 16718390, 0.55F, 1.6F, form);
      orbBody(m, f.rel(orb(b, age, -1)), r, 1194184, 10275839, form * (1.0F - fuse));
      orbBody(m, f.rel(orb(b, age, 1)), r, 11536930, 16752298, form * (1.0F - fuse));
   }

   public void light(LimitlessMesh m, LimitlessFrame f, PurpleBeats b, float age, float form, float presence, float held) {
      float fuse = PurpleBeats.fuse(age);
      float r = orbRadius(age, form);
      Vector3f bc = f.rel(orb(b, age, -1));
      Vector3f rc = f.rel(orb(b, age, 1));
      this.trail(m, f, b, age, -1, 10275839, TRAIL_ALPHA.getF() / 255.0F, 0.6F, form);
      this.trail(m, f, b, age, 1, 16752298, TRAIL_ALPHA.getF() / 255.0F, 0.6F, form);
      float g = form * (1.0F - fuse);
      m.disc(bc, r * 2.6F, 24, LimitlessMesh.rgba(2777855, 0.4F * g), 0);
      m.disc(rc, r * 2.6F, 24, LimitlessMesh.rgba(16718390, 0.32F * g), 0);
      m.disc(bc, r * 0.6F, 14, LimitlessMesh.rgba(15398143, 0.8F * g), LimitlessMesh.rgba(2777855, 0.0F));
      m.disc(rc, r * 0.6F, 14, LimitlessMesh.rgba(16771822, 0.7F * g), LimitlessMesh.rgba(16718390, 0.0F));
      float meet = Ease.ramp(age, 31.0F, 14.0F) * presence;
      m.disc(f.rel(b.merge), (0.5F + 0.5F * meet) * held, 28, LimitlessMesh.rgba(9715967, 0.3F * meet), 0);
   }

   private static void orbBody(LimitlessMesh m, Vector3f c, float r, int deep, int face, float a) {
      if (!(a <= 0.0F)) {
         m.sphere(
            c,
            r,
            8,
            14,
            (n, facing) -> LimitlessMesh.fade(LimitlessMesh.mix(LimitlessMesh.rgba(deep, 1.0F), LimitlessMesh.rgba(face, 1.0F), Math.max(0.0F, facing)), a)
         );
      }
   }

   private void trail(LimitlessMesh m, LimitlessFrame f, PurpleBeats b, float age, int side, int rgb, float alpha, float widthK, float form) {
      float span = TRAIL.getF();
      float fuseFade = 1.0F - PurpleBeats.fuse(age);
      float a = alpha * form * fuseFade * Ease.ramp(age, 8.0F, 6.0F);
      if (!(a <= 0.003F)) {
         float w = TRAIL_W.getF() * widthK;

         for (int i = 0; i < 18; i++) {
            float t = (float)i / 17.0F;
            float at = Math.max(0.0F, age - span * (1.0F - t));
            this.pts[i].set(f.rel(orb(b, at, side)));
            this.widths[i] = w * (0.15F + 0.85F * t);
            this.colors[i] = LimitlessMesh.rgba(rgb, a * t * t);
         }

         m.ribbon(this.pts, this.widths, this.colors, 18);
      }
   }
}
