package com.devilfruits.fruits.supa_supa.client.blade;

import net.minecraft.util.Mth;

final class BladeShapes {
   private static final float[] ORIGIN = new float[]{0.0F, 0.0F, 0.0F};
   private static final float[] DOWN = new float[]{0.0F, 1.0F, 0.0F};
   private static final float[] OUT = new float[]{-1.0F, 0.0F, 0.0F};
   private static final float[] IN = new float[]{1.0F, 0.0F, 0.0F};
   private static final float[] FRONT = new float[]{0.0F, 0.0F, -1.0F};
   private static final float[] Z = new float[]{0.0F, 0.0F, 1.0F};
   private static final float[] X = new float[]{1.0F, 0.0F, 0.0F};
   static final float OUTER = 3.0F;
   static final float FIST = 10.0F;
   static final BladeMesh.Shape IN_WIDE = inArm(3.5F, 2.4F, false);
   static final BladeMesh.Shape IN_SLIM = inArm(3.5F, 2.4F, true);
   static final BladeMesh.Shape OUT_WIDE = pastFist(16.0F, 2.4F, 3.6F, false);
   static final BladeMesh.Shape OUT_SLIM = pastFist(16.0F, 2.4F, 3.6F, true);
   static final BladeMesh.Shape XIN_WIDE = inArm(3.0F, 2.8F, false);
   static final BladeMesh.Shape XIN_SLIM = inArm(3.0F, 2.8F, true);
   static final BladeMesh.Shape XOUT_WIDE = pastFist(19.5F, 2.8F, 4.0F, false);
   static final BladeMesh.Shape XOUT_SLIM = pastFist(19.5F, 2.8F, 4.0F, true);
   static final BladeMesh.Shape[] CLAWS = new BladeMesh.Shape[4];
   static final BladeMesh.Shape INNER;
   static final BladeMesh.Shape SKATE;

   static float smooth(float x) {
      x = Mth.clamp(x, 0.0F, 1.0F);
      return x * x * (3.0F - 2.0F * x);
   }

   static float easeOut(float x) {
      x = Mth.clamp(x, 0.0F, 1.0F);
      return 1.0F - (1.0F - x) * (1.0F - x);
   }

   static boolean isInArm(BladeMesh.Shape s) {
      return s == IN_WIDE || s == IN_SLIM || s == XIN_WIDE || s == XIN_SLIM;
   }

   static boolean isPastFist(BladeMesh.Shape s) {
      return s == OUT_WIDE || s == OUT_SLIM || s == XOUT_WIDE || s == XOUT_SLIM;
   }

   private static float centre(boolean slim) {
      return slim ? -0.5F : -1.0F;
   }

   private static BladeMesh.Shape inArm(float from, float edge, boolean slim) {
      float len = 10.0F - from;
      float armHalf = (slim ? 1.5F : 2.0F) - 0.03F;
      return BladeMesh.shape(new float[]{centre(slim), 0.0F, 0.0F}, DOWN, Z, X, from, 10.0F, 22, (s, out) -> {
         float y = from + len * s;
         out[0] = 0.0F;
         out[1] = Math.min(edge, Math.max(0.0F, y - from));
         out[2] = armHalf;
      }, 1.0F, 0.55F);
   }

   private static BladeMesh.Shape pastFist(float to, float edge, float tipLen, boolean slim) {
      float from = 9.6F;
      float len = to - from;
      return BladeMesh.shape(new float[]{centre(slim), 0.0F, 0.0F}, DOWN, Z, X, from, to, 26, (s, out) -> {
         float y = from + len * s;
         float k = Mth.clamp((y - (to - tipLen)) / tipLen, 0.0F, 1.0F);
         float e = edge * (float)Math.sqrt((double)(1.0F - k * k));
         out[0] = 0.0F;
         out[1] = e;
         out[2] = 0.7F * (k > 0.0F ? Math.min(1.0F, e / 1.2F + 0.15F) : 1.0F);
      }, 1.0F, 0.55F);
   }

   private BladeShapes() {
   }

   static {
      float[] xs = new float[]{-2.45F, -1.5F, -0.55F, 0.4F};

      for (int i = 0; i < 4; i++) {
         float len = i != 0 && i != 3 ? 5.4F : 4.6F;
         CLAWS[i] = BladeMesh.shape(new float[]{xs[i], 0.0F, -0.4F}, DOWN, FRONT, X, 9.4F, 9.4F + len, 16, (s, out) -> {
            float back = 0.35F + 1.7F * s * s;
            out[0] = back;
            out[1] = back + 0.95F * (float)Math.pow((double)(1.0F - s), 0.8);
         }, 0.42F, 0.4F);
      }

      INNER = BladeMesh.shape(new float[]{0.4F, 0.0F, 0.0F}, DOWN, IN, Z, 4.0F, 11.5F, 18, (s, out) -> {
         out[0] = 0.2F;
         out[1] = 0.2F + 1.6F * (float)Math.pow(Math.sin(Math.PI * (double)Math.min(1.0F, s * 1.08F)), 0.7);
      }, 0.6F, 0.45F);
      SKATE = BladeMesh.shape(new float[]{0.0F, 12.0F, 3.2F}, new float[]{0.0F, 0.0F, -1.0F}, DOWN, X, 0.0F, 8.9F, 26, (s, out) -> {
         float curl = smooth((s - 0.7F) / 0.3F);
         out[0] = -0.2F - 0.7F * curl;
         out[1] = 2.1F - 1.7F * (float)Math.pow((double)smooth((s - 0.68F) / 0.32F), 1.3);
      }, 0.6F, 0.5F);
   }
}
