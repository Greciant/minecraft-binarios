package com.awakenedmod.awakened.client.chrono.stasis;

import net.minecraft.util.Mth;

public abstract class StasisStyle {
   public int releaseTicks() {
      return 0;
   }

   public void draw(StasisProp prop, PropCanvas canvas) {
   }

   public void cast(StasisCue cue) {
   }

   public void tick(StasisCue cue) {
   }

   public void release(StasisCue cue) {
   }

   public static float sized(float dimension, float k, float min, float max) {
      return Mth.clamp(dimension * k, min, Math.max(min, max));
   }

   public static float easeIn(float t) {
      t = Mth.clamp(t, 0.0F, 1.0F);
      return t * t;
   }

   public static float easeOut(float t) {
      t = Mth.clamp(t, 0.0F, 1.0F);
      return 1.0F - (1.0F - t) * (1.0F - t);
   }
}
