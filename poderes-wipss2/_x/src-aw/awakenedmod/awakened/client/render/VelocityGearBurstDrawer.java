package com.awakenedmod.awakened.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Vector3f;

final class VelocityGearBurstDrawer {
   private static final int STREAKS_UP = 12;
   private static final int STREAKS_DOWN = 6;
   private static final float LENGTH_MIN = 0.8F;
   private static final float LENGTH_MAX = 1.6F;
   private static final float SCALE_FOLLOW = 0.3F;
   private static final float FAN_DEG = 300.0F;
   private static final float FAN_JITTER_DEG = 12.0F;
   private static final float ELEV_MIN_DEG = 6.0F;
   private static final float ELEV_MAX_DEG = 38.0F;
   private static final float BIAS_LENGTH = 0.5F;
   private static final float GROW_TICKS = 1.5F;
   private static final float ROOT_DETACH = 0.25F;
   private static final float FADE_START = 0.3F;
   private static final float BODY_HALF = 0.07F;
   private static final float CORE_HALF = 0.025F;
   private static final float CORE_LENGTH = 0.7F;
   private static final float BODY_ALPHA = 110.0F;
   private static final float CORE_ALPHA = 180.0F;
   private static final float ROOT_Y = 0.06F;

   private VelocityGearBurstDrawer() {
   }

   static void draw(PoseStack poseStack, VfxDraw.Sink sink, VfxRenderState state, float life) {
      float age = state.ageInTicks - (float)state.delay;
      boolean shiftUp = state.secondary.y >= 0.0;
      int count = shiftUp ? 12 : 6;
      float grow = VelocityStreaks.easeOut(age / 1.5F);
      float fadeT = life <= 0.3F ? 0.0F : (life - 0.3F) / 0.7F;
      float alpha = 1.0F - VelocityStreaks.easeOut(fadeT);
      if (!(alpha <= 0.01F) && !(grow <= 0.01F)) {
         int bodyA = VfxDraw.ia(110.0F * alpha, state.intensity);
         int coreA = VfxDraw.ia(180.0F * alpha, state.intensity);
         int hot = VfxColor.core(state.color, state.color2, 0.8F);
         int bodyRoot = VfxColor.argb(bodyA, hot);
         int bodyTip = VfxColor.argb(bodyA, state.color);
         int coreRoot = VfxColor.argb(coreA, hot);
         int coreTip = VfxColor.argb(0, hot);
         float sizeScale = 0.7F + 0.3F * state.scale;
         float kickDeg = shiftUp ? 180.0F : 0.0F;
         int seed = state.seed;
         VfxDraw.additive(poseStack, sink, (pose, vc) -> {
            for (int k = 0; k < count; k++) {
               float slot = ((float)k + 0.5F) / (float)count - 0.5F;
               float azDeg = kickDeg + slot * 300.0F + VelocityStreaks.signed(k, 1, seed) * 12.0F;
               float elDeg = 6.0F + 32.0F * VelocityStreaks.noise(k, 2, seed);
               double az = Math.toRadians((double)azDeg);
               double el = Math.toRadians((double)elDeg);
               float backness = 0.5F + 0.5F * (float)Math.cos(Math.toRadians((double)(azDeg - kickDeg)));
               float len = (0.8F + 0.8F * VelocityStreaks.noise(k, 3, seed)) * sizeScale * (1.0F + 0.5F * backness) * grow;
               Vector3f dir = new Vector3f((float)(Math.sin(az) * Math.cos(el)), (float)Math.sin(el), (float)(Math.cos(az) * Math.cos(el)));
               Vector3f root = new Vector3f(dir).mul(len * 0.25F).add(0.0F, 0.06F, 0.0F);
               Vector3f tip = new Vector3f(dir).mul(len).add(0.0F, 0.06F, 0.0F);
               Vector3f coreTipPt = new Vector3f(root).lerp(tip, 0.7F);
               VelocityStreaks.streak(pose, vc, root, tip, 0.07F * sizeScale, 0.0F, bodyRoot, bodyTip);
               VelocityStreaks.streak(pose, vc, root, coreTipPt, 0.025F * sizeScale, 0.0F, coreRoot, coreTip);
            }
         });
      }
   }
}
