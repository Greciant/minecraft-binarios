package com.awakenedmod.awakened.client.render.limitless.purple;

import com.awakenedmod.awakened.client.render.limitless.CutEdge;
import com.awakenedmod.awakened.client.render.limitless.Ease;
import com.awakenedmod.awakened.client.render.limitless.LimitlessFrame;
import com.awakenedmod.awakened.client.render.limitless.LimitlessMesh;
import com.awakenedmod.awakened.content.limitless.purple.PurpleStrength;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class PurpleBeats {
   public static final float FORM = 10.0F;
   public static final float DRIFT = 8.0F;
   public static final float MEET = 41.0F;
   public static final float FUSED = 47.0F;
   public static final float GROW = 9.0F;
   public static final float BEAT_RISE = 0.4F;
   private static final float PULSE_SWELL = 0.12F;
   private static final int CUT_ANGLES = 72;
   private static final double CUT_STEP = 0.35;
   public final Vec3 left;
   public final Vec3 right;
   public final Vec3 merge;
   public final Vec3 fwd;
   public final int gone;
   public final PurpleStrength strength;
   public final float overcharge;
   public final float pullIn;

   public PurpleBeats(PurpleCue c) {
      this.left = c.left();
      this.right = c.right();
      this.merge = c.merge();
      Vec3 f = c.fwd();
      this.fwd = f.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : f.normalize();
      this.gone = c.gone();
      this.strength = c.strength();
      this.overcharge = Math.max(0.0F, Math.min(4.0F, c.overcharge()));
      this.pullIn = Ease.clamp01(c.pullIn());
   }

   public static float form(float age) {
      return Ease.ramp(age, 0.0F, 10.0F);
   }

   public static float drift(float age) {
      return Ease.smooth((age - 8.0F) / 33.0F);
   }

   public static float fuse(float age) {
      return Ease.ramp(age, 41.0F, 6.0F);
   }

   public static float grow(float age) {
      return Ease.ramp(age, 50.0F, 9.0F);
   }

   public float live(float age) {
      return 1.0F - Ease.ramp(age, (float)this.gone, 10.0F);
   }

   public static boolean released(float age) {
      return age >= 50.0F;
   }

   public Vec3 centre(float age) {
      return this.strength.centre(this.merge, this.fwd, (double)age, this.gone);
   }

   public double travelled(float age) {
      return this.strength.speed() * (double)Math.max(0.0F, Math.min(age, (float)this.gone) - 50.0F);
   }

   public float radius(float age, float chargeR) {
      return (float)this.strength.radius((double)age, (double)chargeR) * (0.15F + 0.85F * this.live(age));
   }

   public float radius(float age) {
      return this.radius(age, (float)this.strength.launchRadius());
   }

   public float swell() {
      if (this.overcharge <= 0.0F) {
         return 0.0F;
      } else {
         int k = Math.min(4, (int)this.overcharge);
         float step = k >= 4 ? 0.0F : Ease.smooth((this.overcharge - (float)k) / 0.4F);
         return ((float)k + step) / 4.0F;
      }
   }

   public float beatPulse() {
      if (!(this.overcharge <= 0.0F) && !(this.overcharge >= 4.0F)) {
         float frac = this.overcharge - (float)((int)this.overcharge);
         float up = Ease.smooth(frac / 0.4F);
         float down = 1.0F - Ease.smooth((frac - 0.4F) / 0.6F);
         return Math.min(up, down) * (1.0F - Ease.smooth(this.pullIn));
      } else {
         return 0.0F;
      }
   }

   public float heldScale() {
      float beats = 1.0F + 1.0F * this.swell() + 0.12F * this.beatPulse();
      float launch = (float)(this.strength.launchRadius() / 0.42);
      return beats + (launch - beats) * Ease.smooth(this.pullIn);
   }

   public float glow() {
      return 0.6F * this.swell() + 0.4F * this.beatPulse();
   }

   public void cutLine(LimitlessFrame f, LimitlessMesh m, float age, float width, int edge, float life) {
      double to = this.travelled(age);
      if (!(to <= 0.0) && f.level != null) {
         double speed = this.strength.speed();
         double radius = this.strength.radius();
         double from = Math.max(0.0, (double)(age - 50.0F) * speed - speed * (double)life - radius);
         if (!(from >= to)) {
            int angles = (int)Math.round(72.0 * radius / PurpleStrength.NORMAL.radius());
            List<List<CutEdge.Hit>> arcs = CutEdge.arcs(f.level, this.merge, this.fwd, from, to, radius + 0.04, angles, 0.35);
            float since0 = age - 50.0F - (float)(radius / speed);

            for (List<CutEdge.Hit> arc : arcs) {
               int n = arc.size();
               Vector3f[] pts = new Vector3f[n];
               float[] w = new float[n];
               int[] c = new int[n];

               for (int i = 0; i < n; i++) {
                  CutEdge.Hit h = arc.get(i);
                  float since = since0 - (float)(h.along() / speed);
                  float k = Ease.ramp(since, 0.0F, 5.0F) * (1.0F - Ease.ramp(since, life * 0.4F, life * 0.6F));
                  pts[i] = f.rel(h.pos());
                  w[i] = width * (0.6F + 0.4F * k);
                  c[i] = LimitlessMesh.fade(edge, k);
               }

               m.ribbon(pts, w, c, n);
            }
         }
      }
   }

   public Vec3 across() {
      Vec3 a = this.right.subtract(this.left);
      return a.lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : a.normalize();
   }

   public Vec3 orb(float age, int side, float sepMeet, float lift) {
      Vec3 hand = side < 0 ? this.left : this.right;
      Vec3 target = this.merge.add(this.across().scale((double)((float)side * sepMeet) * 0.5 * (double)(1.0F - fuse(age))));
      float k = drift(age);
      return lerp(hand, target, (double)k).add(0.0, (double)lift * Math.sin(Math.PI * (double)k), 0.0);
   }

   public static Vec3 lerp(Vec3 a, Vec3 b, double k) {
      return a.add(b.subtract(a).scale(k));
   }
}
