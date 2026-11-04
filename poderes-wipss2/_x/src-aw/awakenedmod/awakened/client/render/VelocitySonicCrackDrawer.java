package com.awakenedmod.awakened.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Vector3f;

final class VelocitySonicCrackDrawer {
   private static final float[] CHEVRON_WIDTHS = new float[]{0.6F, 1.0F, 1.4F};
   private static final float[] CHEVRON_START_Z = new float[]{0.0F, -0.22F, -0.44F};
   private static final float SHOOT_DISTANCE = 1.3F;
   private static final float SHOOT_TICKS = 4.0F;
   private static final float ARM_DEG = 32.0F;
   private static final float ARM_HALF = 0.05F;
   private static final float ARM_THIN_PER_STEP = 0.15F;
   private static final int BOLTS_MIN = 4;
   private static final int BOLTS_MAX = 6;
   private static final float BOLT_LEN_MIN = 0.5F;
   private static final float BOLT_LEN_MAX = 0.9F;
   private static final int BOLT_SEGS_MIN = 3;
   private static final int BOLT_SEGS_MAX = 4;
   private static final float BOLT_HALF = 0.015F;
   private static final float BOLT_JITTER = 0.18F;
   private static final float BOLT_BACK_LEAN = 0.6F;
   private static final float BOLT_TIP_LIGHTEN = 0.15F;
   private static final float JITTER_WINDOW_TICKS = 2.0F;
   private static final float FADE_START = 0.25F;
   private static final float CHEVRON_ALPHA = 140.0F;
   private static final float BOLT_ALPHA = 150.0F;

   private VelocitySonicCrackDrawer() {
   }

   static void draw(PoseStack poseStack, VfxDraw.Sink sink, VfxRenderState state, float life) {
      float age = state.ageInTicks - (float)state.delay;
      float fadeT = life <= 0.25F ? 0.0F : (life - 0.25F) / 0.75F;
      float alpha = 1.0F - VelocityStreaks.easeOut(fadeT);
      if (!(alpha <= 0.01F)) {
         float s = state.scale;
         float shoot = 1.3F * s * VelocityStreaks.easeOut(age / 4.0F);
         int chevA = VfxDraw.ia(140.0F * alpha, state.intensity);
         int boltA = VfxDraw.ia(150.0F * alpha, state.intensity);
         int hot = VfxColor.core(state.color, state.color2, 0.8F);
         int apexCol = VfxColor.argb(chevA, hot);
         int tipCol = VfxColor.argb(chevA, state.color);
         int boltRoot = VfxColor.argb(boltA, hot);
         int boltTip = VfxColor.argb(boltA, VfxColor.lighten(state.color, 0.15F));
         int window = state.seed * 31 + (int)(age / 2.0F);
         int seed = state.seed;
         double sinA = Math.sin(Math.toRadians(32.0));
         double cosA = Math.cos(Math.toRadians(32.0));
         VfxDraw.additive(poseStack, sink, (pose, vc) -> {
            for (int i = 0; i < CHEVRON_WIDTHS.length; i++) {
               float halfW = CHEVRON_WIDTHS[i] * 0.5F * s;
               float armLen = (float)((double)halfW / sinA);
               float armHalf = 0.05F * s * (1.0F - 0.15F * (float)i);
               Vector3f apex = new Vector3f(0.0F, 0.0F, CHEVRON_START_Z[i] * s + shoot);
               float dz = (float)(-cosA * (double)armLen);
               float dx = (float)(sinA * (double)armLen);
               VelocityStreaks.streak(pose, vc, apex, new Vector3f(apex).add(dx, 0.0F, dz), armHalf, 0.0F, apexCol, tipCol);
               VelocityStreaks.streak(pose, vc, apex, new Vector3f(apex).add(-dx, 0.0F, dz), armHalf, 0.0F, apexCol, tipCol);
               VelocityStreaks.streak(pose, vc, apex, new Vector3f(apex).add(0.0F, dx, dz), armHalf, 0.0F, apexCol, tipCol);
               VelocityStreaks.streak(pose, vc, apex, new Vector3f(apex).add(0.0F, -dx, dz), armHalf, 0.0F, apexCol, tipCol);
            }

            Vector3f apex = new Vector3f(0.0F, 0.0F, shoot);
            int bolts = 4 + (int)(VelocityStreaks.noise(0, 0, seed) * 3.0F);
            bolts = Math.min(6, bolts);

            for (int b = 0; b < bolts; b++) {
               double phi = (double)VelocityStreaks.noise(b, 1, window) * Math.PI * 2.0;
               float reach = (0.5F + 0.39999998F * VelocityStreaks.noise(b, 2, window)) * s;
               float back = 0.6F * VelocityStreaks.noise(b, 3, window);
               Vector3f dir = new Vector3f((float)Math.cos(phi), (float)Math.sin(phi), -back).normalize();
               Vector3f tip = new Vector3f(dir).mul(reach).add(apex);
               int segs = 3 + (int)(VelocityStreaks.noise(b, 4, window) * 2.0F);
               VelocityStreaks.lightning(pose, vc, apex, tip, 0.015F * s, Math.min(4, segs), 0.18F, window * 7919 + b, boltRoot, boltTip);
            }
         });
      }
   }
}
