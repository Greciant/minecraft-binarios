package com.awakenedmod.awakened.client.render.limitless.voidlook;

import com.awakenedmod.awakened.client.render.FxLayer;
import com.awakenedmod.awakened.client.render.limitless.Ease;
import com.awakenedmod.awakened.client.render.limitless.FrozenGaze;
import com.awakenedmod.awakened.client.render.limitless.LimitlessComfort;
import com.awakenedmod.awakened.client.render.limitless.LimitlessFrame;
import com.awakenedmod.awakened.client.render.limitless.LimitlessLook;
import com.awakenedmod.awakened.client.render.limitless.LimitlessMesh;
import com.awakenedmod.awakened.client.render.limitless.SpaceBend;
import com.awakenedmod.awakened.client.render.limitless.VoidSky;
import com.awakenedmod.awakened.content.limitless.domain.VoidTimings;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public abstract class VoidLookBase implements LimitlessLook<VoidCue> {
   private static final int MAX_GAZES = 48;
   private static final float DETACH = 6.0F;
   private static final float WRAP_IN = 8.0F;
   private static final float GAZE_OUT = 8.0F;
   private static final float VEIL_IN = 6.0F;
   private static final float GLINT_SIZE = 0.6F;
   private static final float GLINT_ALPHA = 0.55F;
   private static final float GLINT_OUT = 8.0F;
   private final VoidLookBase.Ctx ctx = new VoidLookBase.Ctx();
   protected final Vector3f[] pts = new Vector3f[96];
   protected final float[] widths = new float[96];
   protected final int[] colors = new int[96];

   protected VoidLookBase() {
      for (int i = 0; i < this.pts.length; i++) {
         this.pts[i] = new Vector3f();
      }
   }

   protected abstract void inside(VoidLookBase.Ctx var1);

   protected abstract void outside(VoidLookBase.Ctx var1);

   protected abstract Vector3f ringDir(VoidLookBase.Ctx var1);

   protected abstract int closeGlow();

   public final void draw(LimitlessFrame f, VoidCue cue, float age) {
      VoidLookBase.Ctx c = this.ctx;
      c.f = f;
      c.cue = cue;
      c.seed = cue.seed();
      c.age = age;
      c.time = (float)f.gameTime + f.partialTick;
      c.calm = LimitlessComfort.distortion(Minecraft.getInstance());
      c.centre = cue.centre();
      c.full = cue.radius();
      c.crack = cue.crackAt();
      c.clear = c.crack + 24.0F;
      c.end = c.crack + 36.0F;
      c.radius = c.full * VoidTimings.shell(age, c.crack);
      c.left = VoidTimings.left(age - c.crack);
      c.centreRel.set(f.rel(c.centre));
      if (c.radius > 0.05F) {
         this.ball(c);
      }

      this.glint(c);
      veil(c);
   }

   private void ball(VoidLookBase.Ctx c) {
      LimitlessFrame f = c.f;
      float age = c.age;
      Vec3 fw = c.cue.forward();
      c.forward.set((float)fw.x, 0.0F, (float)fw.z).normalize();
      c.right.set(-c.forward.z, 0.0F, c.forward.x);
      c.sky.set(f.cam, c.centre, (double)c.radius).detach(Ease.ramp(age, c.crack, 6.0F));
      c.open = Ease.ramp(age, 0.0F, 10.0F);
      c.hold = Ease.smooth(c.left / 0.1F);
      if (c.sky.inside()) {
         gaze(c);
         this.inside(c);
      } else {
         if (age >= c.crack) {
            c.hold = c.hold * Ease.ramp(age, c.crack, 8.0F);
            c.gazeCount = 0;
            this.inside(c);
         }

         this.outside(c);
      }
   }

   private static void veil(VoidLookBase.Ctx c) {
      float d = c.centreRel.length();
      if (!(c.age < c.crack) && !(d >= c.full) && !(c.age > c.end)) {
         float passed = c.crack + VoidTimings.leftAt(d / c.full);
         float a;
         if (d < c.radius) {
            float lead = Math.min(6.0F, passed - c.crack);
            a = lead <= 0.0F ? 1.0F : Ease.ramp(c.age, passed - lead, lead);
         } else {
            a = 1.0F - Ease.ramp(c.age, passed, 12.0F);
         }

         if (!(a <= 0.0F)) {
            LimitlessFrame f = c.f;
            int col = LimitlessMesh.rgba(0, a);
            Vector3f fw = new Vector3f(f.camForward).mul(0.5F);
            Vector3f r = new Vector3f(f.camRight).mul(3.0F);
            Vector3f u = new Vector3f(f.camUp).mul(3.0F);
            LimitlessMesh m = f.begin(FxLayer.PAINT_THROUGH_WALLS);
            m.quad(
               new Vector3f(fw).sub(r).sub(u),
               col,
               new Vector3f(fw).add(r).sub(u),
               col,
               new Vector3f(fw).add(r).add(u),
               col,
               new Vector3f(fw).sub(r).add(u),
               col
            );
            f.end(FxLayer.PAINT_THROUGH_WALLS);
         }
      }
   }

   private void glint(VoidLookBase.Ctx c) {
      float rise = 7.2000003F;
      float a = 0.55F * Ease.ramp(c.age, c.clear - rise, rise) * (1.0F - Ease.ramp(c.age, c.clear, 8.0F)) * Ease.ramp(c.centreRel.length(), 1.5F, 1.0F);
      if (!(a <= 0.0F)) {
         int rgb = this.closeGlow();
         LimitlessMesh light = c.f.begin(FxLayer.LIGHT);
         light.disc(c.centreRel, 1.5F, 20, LimitlessMesh.rgba(rgb, a * 0.35F), LimitlessMesh.rgba(rgb, 0.0F));
         light.disc(c.centreRel, 0.6F, 16, LimitlessMesh.rgba(16777215, a), LimitlessMesh.rgba(rgb, 0.0F));
         c.f.end(FxLayer.LIGHT);
      }
   }

   private static void gaze(VoidLookBase.Ctx c) {
      c.gazeCount = 0;
      c.gazeIn = 1.0F - Ease.ramp(c.age, c.crack, 8.0F);
      if (!(c.gazeIn <= 0.0F)) {
         for (Entity e : c.cue.caught()) {
            if (c.gazeCount >= c.gazes.length) {
               break;
            }

            if (e != null && !(e.position().distanceToSqr(c.centre) > (double)(c.full * c.full)) && c.gazes[c.gazeCount].of(e, c.f.partialTick, c.f.cam)) {
               c.gazeOf[c.gazeCount] = e;
               c.gazeCount++;
            }
         }
      }
   }

   protected static void eclipse(VoidLookBase.Ctx c, Vector3f dir, float ang, int centre, int rim) {
      float a = c.hold * c.open;
      if (!(a <= 0.0F)) {
         if (a >= 1.0F) {
            LimitlessMesh solid = c.f.begin(FxLayer.SOLID);
            c.sky.disc(solid, dir, ang, 96, centre, rim, 0.975F);
            c.f.end(FxLayer.SOLID);
         } else {
            LimitlessMesh paint = c.f.begin(FxLayer.PAINT);
            c.sky.disc(paint, dir, ang, 96, LimitlessMesh.fade(centre, a), LimitlessMesh.fade(rim, a), 0.975F);
            c.f.end(FxLayer.PAINT);
         }
      }
   }

   protected static void outline(VoidLookBase.Ctx c, LimitlessMesh light, float scale, float half, int mid, int edge) {
      float d = c.centreRel.length();
      if (!(d <= c.radius * 1.05F)) {
         float rb = c.radius * d / (float)Math.sqrt((double)(d * d - c.radius * c.radius));
         light.ring(c.centreRel, rb * scale, rb * half, 160, mid, edge);
      }
   }

   protected final void stroke(LimitlessMesh m, int n) {
      m.ribbon(this.pts, this.widths, this.colors, n);
   }

   protected final void eyeStream(
      LimitlessMesh m,
      FrozenGaze g,
      Vector3f eye,
      float len,
      float curl,
      float wobble,
      float w0,
      int c0,
      int c1,
      int n,
      float beads,
      float speed,
      float time,
      float phase
   ) {
      Vector3f side = new Vector3f(g.look).cross(g.up);
      wobble *= this.ctx.calm;
      n = Math.min(n, this.pts.length);

      for (int i = 0; i < n; i++) {
         float u = (float)i / (float)(n - 1);
         float d = u * len;
         float wv = wobble * u * sin(u * 5.5F - time * 0.09F + phase);
         this.pts[i].set(eye).fma(d, g.look).fma(curl * u * u * len, g.up).fma(wv, side);
         this.widths[i] = w0 * (1.0F - 0.85F * u);
         float bead = beads <= 0.0F ? 0.0F : (float)Math.pow((double)(0.5F + 0.5F * cos((float) (Math.PI * 2) * (u * beads - time * speed) + phase)), 6.0);
         float a = (1.0F - u) * (1.0F - u) * (0.55F + 0.45F * bead) * Ease.ramp(u, 0.0F, 0.051F);
         this.colors[i] = LimitlessMesh.fade(LimitlessMesh.mix(c0, c1, u), a);
      }

      this.stroke(m, n);
   }

   protected static float beads(float s, float time, float beads, float speed, float sharp, float phase) {
      return (float)Math.pow((double)(0.5F + 0.5F * cos((float) (Math.PI * 2) * (s * beads - time * speed) + phase)), (double)sharp);
   }

   protected static Vector3f onArc(Vector3f c, Vector3f u, Vector3f v, float r, float a, Vector3f out) {
      float ca = cos(a) * r;
      float sa = sin(a) * r;
      return out.set(c.x + u.x * ca + v.x * sa, c.y + u.y * ca + v.y * sa, c.z + u.z * ca + v.z * sa);
   }

   protected static Vector3f randomDir(float a, float b, Vector3f out) {
      float z = a * 2.0F - 1.0F;
      float t = b * (float) (Math.PI * 2);
      float s = (float)Math.sqrt((double)Math.max(0.0F, 1.0F - z * z));
      return out.set(s * cos(t), z, s * sin(t));
   }

   protected static float h(VoidLookBase.Ctx c, int salt) {
      return SpaceBend.hash(c.seed, salt);
   }

   protected static float h(long seed, int salt) {
      return SpaceBend.hash(seed, salt);
   }

   protected static int col(int rgb, float a) {
      return LimitlessMesh.rgba(rgb, Ease.clamp01(a));
   }

   protected static float sin(float x) {
      return (float)Math.sin((double)x);
   }

   protected static float cos(float x) {
      return (float)Math.cos((double)x);
   }

   protected static final class Ctx {
      public LimitlessFrame f;
      public VoidCue cue;
      public long seed;
      public float age;
      public final VoidSky sky = new VoidSky();
      public Vec3 centre;
      public final Vector3f centreRel = new Vector3f();
      public final Vector3f forward = new Vector3f();
      public final Vector3f right = new Vector3f();
      public float radius;
      public float full;
      public float open;
      public float hold;
      public float left;
      public float time;
      public float calm;
      public float crack;
      public float clear;
      public float end;
      public final FrozenGaze[] gazes = new FrozenGaze[48];
      public int gazeCount;
      public float gazeIn;
      public final Entity[] gazeOf = new Entity[48];

      Ctx() {
         for (int i = 0; i < this.gazes.length; i++) {
            this.gazes[i] = new FrozenGaze();
         }
      }

      public Vector3f dir(float side, float up) {
         float cu = (float)Math.cos((double)up);
         float su = (float)Math.sin((double)up);
         float cs = (float)Math.cos((double)side);
         float ss = (float)Math.sin((double)side);
         return new Vector3f(this.forward.x * cs * cu + this.right.x * ss * cu, su, this.forward.z * cs * cu + this.right.z * ss * cu).normalize();
      }
   }
}
