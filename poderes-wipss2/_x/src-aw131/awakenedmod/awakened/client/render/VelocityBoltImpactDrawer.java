package com.awakenedmod.awakened.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

final class VelocityBoltImpactDrawer {
   private static final int STREAKS_BASE = 14;
   private static final int TOP_GEAR = 5;
   private static final int CHARGE_STEPS = 2;
   private static final float LEN_MIN = 0.8F;
   private static final float LEN_MAX = 2.0F;
   private static final float SNAP_TICKS = 2.0F;
   private static final float BURST_LIFE = 8.0F;
   private static final float BIAS = 0.9F;
   private static final float ROOT_HALF = 0.06F;
   private static final float ROOT_DETACH = 0.1F;
   private static final float ROOT_ALPHA = 165.0F;
   private static final float TIP_ALPHA = 150.0F;
   private static final int STRANDS_MIN = 3;
   private static final int STRANDS_MAX = 5;
   private static final float STRAND_LIFE = 6.0F;
   private static final float STRAND_SPAWN_RADIUS = 0.35F;
   private static final float STRAND_LEN_MIN = 0.4F;
   private static final float STRAND_LEN_MAX = 0.9F;
   private static final int STRAND_SEGS_MIN = 3;
   private static final int STRAND_SEGS_MAX = 4;
   private static final float STRAND_HALF = 0.012F;
   private static final float STRAND_JITTER = 0.25F;
   private static final float STRAND_ALPHA = 150.0F;
   private static final float STRAND_LIFT = 0.05F;
   private static final float JITTER_WINDOW_TICKS = 2.0F;

   private VelocityBoltImpactDrawer() {
   }

   static void draw(PoseStack ps, VfxDraw.Sink sink, VfxRenderState state, float life) {
      float age = state.ageInTicks - (float)state.delay;
      if (!(age < 0.0F)) {
         int steps = Math.max(0, Math.min(7, Math.round(state.scale)));
         float in = state.intensity;
         int seed = state.seed;
         int hot = VfxColor.core(state.color, state.color2, 0.8F);
         int body = state.color;
         VfxDraw.additive(ps, sink, (pose, vc) -> {
            starburst(pose, vc, age, 14 + steps, in, seed, hot, body);
            strands(pose, vc, age, in, seed, hot, body);
         });
      }
   }

   private static void starburst(Pose pose, VertexConsumer vc, float age, int count, float in, int seed, int hotRgb, int bodyRgb) {
      if (!(age >= 8.0F)) {
         float t = age / 8.0F;
         float fade = (1.0F - t) * (1.0F - t);
         float reach = AnimEnvelopes.easeOutCubic(age / 2.0F);
         int rootCol = VfxColor.argb(VfxDraw.ia(165.0F * fade, in), hotRgb);
         int tipCol = VfxColor.argb(VfxDraw.ia(150.0F * fade, in), bodyRgb);

         for (int i = 0; i < count; i++) {
            float z = VelocityStreaks.signed(seed, i, 61);
            float phi = VelocityStreaks.noise(seed, i, 62) * (float) (Math.PI * 2);
            float ring = Mth.sqrt(Math.max(0.0F, 1.0F - z * z));
            Vector3f dir = new Vector3f(ring * Mth.cos(phi), ring * Mth.sin(phi), z + 0.9F).normalize();
            float len = Mth.lerp(VelocityStreaks.noise(seed, i, 63), 0.8F, 2.0F) * reach;
            Vector3f root = new Vector3f(dir).mul(len * 0.1F);
            Vector3f tip = new Vector3f(dir).mul(len);
            VelocityStreaks.streak(pose, vc, root, tip, 0.06F, 0.0F, rootCol, tipCol);
         }
      }
   }

   private static void strands(Pose pose, VertexConsumer vc, float age, float in, int seed, int hotRgb, int bodyRgb) {
      if (!(age >= 6.0F)) {
         float fade = 1.0F - age / 6.0F;
         int window = seed * 31 + (int)(age / 2.0F);
         int count = 3 + (int)(VelocityStreaks.noise(seed, 0, 71) * 3.0F);
         count = Math.min(5, count);
         int rootCol = VfxColor.argb(VfxDraw.ia(150.0F * fade, in), hotRgb);
         int tipCol = VfxColor.argb(0, bodyRgb);

         for (int s = 0; s < count; s++) {
            float startAng = VelocityStreaks.noise(window, s, 72) * (float) (Math.PI * 2);
            float startR = VelocityStreaks.noise(window, s, 73) * 0.35F;
            float runAng = VelocityStreaks.noise(window, s, 74) * (float) (Math.PI * 2);
            float len = Mth.lerp(VelocityStreaks.noise(window, s, 75), 0.4F, 0.9F);
            Vector3f root = new Vector3f(Mth.cos(startAng) * startR, Mth.sin(startAng) * startR, -0.05F);
            Vector3f tip = new Vector3f(root).add(Mth.cos(runAng) * len, Mth.sin(runAng) * len, 0.0F);
            int segs = 3 + (int)(VelocityStreaks.noise(window, s, 76) * 2.0F);
            VelocityStreaks.lightning(pose, vc, root, tip, 0.012F, Math.min(4, segs), 0.25F, window * 7919 + s, rootCol, tipCol);
         }
      }
   }
}
