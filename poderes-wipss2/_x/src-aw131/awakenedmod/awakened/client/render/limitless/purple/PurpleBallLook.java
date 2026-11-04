package com.awakenedmod.awakened.client.render.limitless.purple;

import com.awakenedmod.awakened.client.render.FxLayer;
import com.awakenedmod.awakened.client.render.limitless.Ease;
import com.awakenedmod.awakened.client.render.limitless.LimitlessComfort;
import com.awakenedmod.awakened.client.render.limitless.LimitlessFrame;
import com.awakenedmod.awakened.client.render.limitless.LimitlessLook;
import com.awakenedmod.awakened.client.render.limitless.LimitlessMesh;
import net.minecraft.client.multiplayer.ClientLevel;
import org.joml.Vector3f;

public abstract class PurpleBallLook implements LimitlessLook<PurpleCue> {
   private final PurpleBinary binary = new PurpleBinary();
   private final PurpleRelease release = new PurpleRelease();
   private final PurpleTrail trail = new PurpleTrail();

   protected abstract void paintBall(LimitlessFrame var1, LimitlessMesh var2, PurpleBallLook.Ball var3);

   protected abstract void lightBall(LimitlessFrame var1, LimitlessMesh var2, PurpleBallLook.Ball var3);

   public void draw(LimitlessFrame f, PurpleCue cue, float age) {
      PurpleBeats b = new PurpleBeats(cue);
      float calm = LimitlessComfort.distortion(f.mc);
      Vector3f fwd = new Vector3f((float)b.fwd.x, (float)b.fwd.y, (float)b.fwd.z);
      float motion = cue.motion(age);
      if (!PurpleBeats.released(age)) {
         this.charge(f, b, age, cue.chargeAge(age), motion, Ease.clamp01(cue.presence()), cue.seed(), calm, fwd);
      } else {
         this.flight(f, b, age, motion, cue.seed(), calm, fwd);
      }
   }

   public void tick(ClientLevel level, PurpleCue cue, int age) {
      if (PurpleBeats.released((float)age)) {
         this.trail.tick(level, new PurpleBeats(cue), age);
      }
   }

   private static float glow(PurpleBeats b, float age, float fuse) {
      boolean flying = PurpleBeats.released(age);
      float base = 0.3F * Ease.smooth(fuse) + 0.35F * (flying ? 1.0F : Ease.smooth(b.pullIn)) + 0.35F * Ease.ramp(age, 50.0F, 6.0F);
      float over = flying ? (b.strength.overcharged() ? 0.3F : 0.0F) : 0.5F * b.glow();
      return base + over;
   }

   private void charge(LimitlessFrame f, PurpleBeats b, float age, float clock, float motion, float presence, long seed, float calm, Vector3f fwd) {
      float form = PurpleBeats.form(clock) * presence;
      float fuse = PurpleBeats.fuse(clock);
      float held = b.heldScale();
      float r = 0.42F * held * Ease.smooth(fuse);
      PurpleBallLook.Ball ball = new PurpleBallLook.Ball(f.rel(b.merge), r, glow(b, age, fuse), fuse * presence, motion, seed, calm, fwd, false);
      LimitlessMesh paint = f.begin(FxLayer.PAINT);
      this.binary.paint(paint, f, b, clock, form);
      if (r > 0.01F) {
         this.paintBall(f, paint, ball);
      }

      f.end(FxLayer.PAINT);
      LimitlessMesh light = f.begin(FxLayer.LIGHT);
      this.binary.light(light, f, b, clock, form, presence, held);
      if (r > 0.01F) {
         this.lightBall(f, light, ball);
         this.release.pulses(light, ball.c(), r, b.overcharge, presence);
      }

      this.release.pullIn(light, f, b, age, motion, r, seed, presence);
      f.end(FxLayer.LIGHT);
   }

   private void flight(LimitlessFrame f, PurpleBeats b, float age, float motion, long seed, float calm, Vector3f fwd) {
      float live = b.live(age);
      PurpleBallLook.Ball ball = live > 0.0F
         ? new PurpleBallLook.Ball(f.rel(b.centre(age)), b.radius(age), glow(b, age, 1.0F), live, motion, seed, calm, fwd, true)
         : null;
      LimitlessMesh paint = f.begin(FxLayer.PAINT);
      this.release.paintRing(paint, f, b, age);
      this.trail.paint(paint, f, b, age, seed, calm);
      if (ball != null) {
         this.paintBall(f, paint, ball);
      }

      f.end(FxLayer.PAINT);
      LimitlessMesh light = f.begin(FxLayer.LIGHT);
      this.release.pullIn(light, f, b, age, motion, (float)b.strength.launchRadius(), seed, 1.0F);
      this.release.lightRing(light, f, b, age);
      this.trail.light(light, f, b, age, seed, calm);
      if (ball != null) {
         this.lightBall(f, light, ball);
      }

      f.end(FxLayer.LIGHT);
   }

   public static float nearFade(Vector3f c, float discR) {
      return discR <= 0.0F ? 1.0F : Ease.ramp(c.length() / discR, 0.6F, 0.6F);
   }

   static float scale(PurpleBeats b) {
      return (float)(b.strength.radius() / 3.0);
   }

   public static record Ball(Vector3f c, float r, float glow, float a, float age, long seed, float calm, Vector3f fwd, boolean flying) {
   }
}
