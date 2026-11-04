package com.awakenedmod.awakened.client.render;

public final class VelocityTimelessShader {
   private static final float ON_AT = 0.5F;
   private static final float OFF_AT = 0.45F;
   private static final PostEffectSlot SLOT = new PostEffectSlot("timeless", "Timeless");

   private VelocityTimelessShader() {
   }

   public static boolean active() {
      return SLOT.active();
   }

   static void update(float intensity) {
      SLOT.want(intensity >= (SLOT.active() ? 0.45F : 0.5F));
   }

   static void clear() {
      SLOT.clear();
   }
}
