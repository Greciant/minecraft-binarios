package com.awakenedmod.awakened.content.limitless.domain;

import net.minecraft.util.Mth;

public final class VoidTimings {
   public static final int INTRO = 44;
   public static final int TITLE = 12;
   public static final int SWEEP_AT = 20;
   public static final int SWEEP = 10;
   public static final int PULL_BACK = 30;
   public static final int OPEN_FADE = 10;
   public static final int COLLAPSE = 24;
   public static final int UNVEIL = 12;
   public static final int CRACK_TO_CLEAR = 24;
   public static final int CRACK_TO_END = 36;
   public static final int CRACK_TO_RELEASE = 44;

   private VoidTimings() {
   }

   public static float lookAge(float t) {
      return t - 20.0F;
   }

   public static float swept(float t) {
      float u = Mth.clamp((t - 20.0F) / 10.0F, 0.0F, 1.0F);
      return 1.0F - (1.0F - u) * (1.0F - u) * (1.0F - u);
   }

   public static float left(float sinceCrack) {
      float u = Mth.clamp(sinceCrack / 24.0F, 0.0F, 1.0F);
      return 1.0F - u * u;
   }

   public static float leftAt(float share) {
      return 24.0F * (float)Math.sqrt((double)(1.0F - Mth.clamp(share, 0.0F, 1.0F)));
   }

   public static float shell(float age, float crack) {
      return swept(age + 20.0F) * left(age - crack);
   }
}
