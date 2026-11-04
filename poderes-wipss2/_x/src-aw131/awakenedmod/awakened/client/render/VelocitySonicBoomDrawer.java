package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.velocity.VelocityPalette;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

final class VelocitySonicBoomDrawer {
   private static final float CONE_LEN_START = 2.0F;
   private static final float CONE_LEN_END = 3.2F;
   private static final float CONE_HALF_ANGLE_DEG = 37.0F;
   private static final float SHELL_GAP = 0.15F;
   private static final float INNER_SHELL_ALPHA_FRAC = 0.7F;
   private static final int CONE_SEGMENTS = 32;
   private static final int CONE_RINGS = 7;
   private static final float CONE_ALPHA = 0.55F;
   private static final float CONE_APEX_ALPHA_FRAC = 0.25F;
   private static final float RIM_BAND_START = 0.82F;
   private static final float RIM_BAND_BOOST = 1.35F;
   private static final float GRAIN = 0.35F;
   private static final float RIM_WOBBLE = 0.04F;
   private static final float FORM_TICKS = 2.0F;
   private static final float FORM_START_FRAC = 0.35F;
   private static final float ALPHA_IN_TICKS = 1.0F;
   private static final float HOLD_TICKS = 4.0F;
   private static final float RIM_GLOW_HALF_W = 0.07F;
   private static final float RIM_GLOW_ALPHA = 70.0F;
   private static final float DISC_ALPHA = 0.2F;
   private static final float DISC_CENTRE_ALPHA_FRAC = 0.5F;
   private static final float DISC_LIFE = 7.0F;
   private static final int BURST_STREAKS = 10;
   private static final float BURST_LIFE = 6.0F;
   private static final float SNAP_TICKS = 2.0F;
   private static final float BURST_LEN_MIN = 0.6F;
   private static final float BURST_LEN_MAX = 1.2F;
   private static final float BURST_BACK_LEAN = 0.45F;
   private static final float BURST_W = 0.07F;
   private static final float TIP_FRACTION = 0.45F;
   private static final float BURST_JITTER = 0.6F;
   private static final int FORK_MIN = 2;
   private static final int FORK_MAX = 3;
   private static final float FORK_WINDOW_TICKS = 2.0F;
   private static final float FORK_LIFE = 6.0F;
   private static final float FORK_LEN_MIN = 0.6F;
   private static final float FORK_LEN_MAX = 1.0F;
   private static final float FORK_BACK_LEAN = 0.5F;
   private static final int FORK_SEGS = 5;
   private static final float FORK_JITTER = 0.16F;
   private static final float FORK_ROOT_HALF = 0.06F;
   private static final Vector3f ORIGIN = new Vector3f();
   private static final float CONE_TAN = (float)Math.tan(Math.toRadians(37.0));

   private VelocitySonicBoomDrawer() {
   }

   static void draw(PoseStack ps, VfxDraw.Sink sink, VfxRenderState state, float life) {
      float age = state.ageInTicks - (float)state.delay;
      if (!(age < 0.0F)) {
         float span = Math.max(1.0F, (float)(state.lifetime - state.delay));
         float k = state.scale;
         float in = state.intensity;
         int seed = state.seed;
         int hot = state.color2;
         int body = state.color;
         int vapourRgb = VelocityPalette.fromBody(body).vapour();
         float form = Mth.lerp(AnimEnvelopes.easeOutCubic(age / 2.0F), 0.35F, 1.0F);
         float length = Mth.lerp(Mth.clamp(age / span, 0.0F, 1.0F), 2.0F, 3.2F) * k * form;
         float mouth = length * CONE_TAN;
         float fade = fadeAt(age, span);
         if (fade > 0.0F) {
            float alpha = 140.25F * fade;
            VfxDraw.custom(ps, sink, FxLayer.PAINT, (pose, vc) -> {
               shell(pose, vc, length, mouth, 0.0F, alpha, in, seed, hot, vapourRgb);
               shell(pose, vc, length, mouth, 0.15F * k, alpha * 0.7F, in, seed + 97, hot, vapourRgb);
               disc(pose, vc, length, mouth, age, in, seed, vapourRgb);
            });
         }

         VfxDraw.additive(ps, sink, (pose, vc) -> {
            if (fade > 0.0F) {
               rimGlow(pose, vc, length, mouth, 0.07F * k, 70.0F * fade, in, hot);
            }

            starburst(pose, vc, age, 0.6F * k, 1.2F * k, 0.07F * k, in, seed, hot, body);
            forks(pose, vc, age, k, in, seed, hot, body);
         });
      }
   }

   private static float fadeAt(float age, float span) {
      float holdEnd = 6.0F;
      if (age < holdEnd) {
         return Mth.clamp(age / 1.0F, 0.0F, 1.0F);
      } else {
         float t = (age - holdEnd) / Math.max(1.0F, span - holdEnd);
         return t >= 1.0F ? 0.0F : (1.0F - t) * (1.0F - t);
      }
   }

   private static void shell(Pose pose, VertexConsumer vc, float length, float mouth, float inset, float alpha, float in, int seed, int rimRgb, int vapourRgb) {
      Vector3f[] prev = ring(0, length, mouth, inset, seed);
      int[] prevCol = ringColors(0, alpha, in, seed, rimRgb, vapourRgb);

      for (int i = 1; i <= 7; i++) {
         Vector3f[] cur = ring(i, length, mouth, inset, seed);
         int[] curCol = ringColors(i, alpha, in, seed, rimRgb, vapourRgb);

         for (int j = 0; j < 32; j++) {
            int j1 = (j + 1) % 32;
            VelocityStreaks.quad(pose, vc, prev[j], prevCol[j], cur[j], curCol[j], cur[j1], curCol[j1], prev[j1], prevCol[j1], false);
         }

         prev = cur;
         prevCol = curCol;
      }
   }

   private static Vector3f[] ring(int i, float length, float mouth, float inset, int seed) {
      float u = (float)i / 7.0F;
      float r = Math.max(0.0F, mouth * u - inset);
      float z = -length * u;
      Vector3f[] pts = new Vector3f[32];

      for (int j = 0; j < 32; j++) {
         float a = (float)j / 32.0F * (float) (Math.PI * 2);
         float wobble = 1.0F + 0.04F * (VelocityStreaks.noise(seed, j, 91) * 2.0F - 1.0F);
         pts[j] = new Vector3f(Mth.cos(a) * r * wobble, Mth.sin(a) * r * wobble, z);
      }

      return pts;
   }

   private static int[] ringColors(int i, float alpha, float in, int seed, int rimRgb, int vapourRgb) {
      float u = (float)i / 7.0F;
      float along = Mth.lerp(u, 0.25F, 1.0F);
      float band = Mth.clamp((u - 0.82F) / 0.18F, 0.0F, 1.0F);
      along *= Mth.lerp(band, 1.0F, 1.35F);
      int rgb = VelocityStreaks.lerpRgb(vapourRgb, rimRgb, band);
      int[] cols = new int[32];

      for (int j = 0; j < 32; j++) {
         float grain = 1.0F + 0.35F * (VelocityStreaks.noise(seed, i, j) * 2.0F - 1.0F);
         cols[j] = VfxColor.argb(clampAlpha(VfxDraw.ia(alpha * along * grain, in)), rgb);
      }

      return cols;
   }

   private static int clampAlpha(int a) {
      return Mth.clamp(a, 0, 255);
   }

   private static void disc(Pose pose, VertexConsumer vc, float length, float mouth, float age, float in, int seed, int vapourRgb) {
      if (!(age >= 7.0F)) {
         float t = age / 7.0F;
         float fade = Mth.clamp(age / 1.0F, 0.0F, 1.0F) * (1.0F - t) * (1.0F - t);
         float alpha = 51.0F * fade;
         Vector3f centre = new Vector3f(0.0F, 0.0F, -length);
         int centreCol = VfxColor.argb(VfxDraw.ia(alpha * 0.5F, in), vapourRgb);
         Vector3f[] rim = ring(7, length, mouth, 0.0F, seed);

         for (int j = 0; j < 32; j++) {
            int j1 = (j + 1) % 32;
            float g0 = 1.0F + 0.35F * (VelocityStreaks.noise(seed, 5, j) * 2.0F - 1.0F);
            float g1 = 1.0F + 0.35F * (VelocityStreaks.noise(seed, 5, j1) * 2.0F - 1.0F);
            VelocityStreaks.tri(
               pose,
               vc,
               centre,
               centreCol,
               rim[j],
               VfxColor.argb(clampAlpha(VfxDraw.ia(alpha * g0, in)), vapourRgb),
               rim[j1],
               VfxColor.argb(clampAlpha(VfxDraw.ia(alpha * g1, in)), vapourRgb),
               false
            );
         }
      }
   }

   private static void rimGlow(Pose pose, VertexConsumer vc, float length, float mouth, float halfW, float alpha, float in, int rgb) {
      int bright = VfxColor.argb(VfxDraw.ia(alpha, in), rgb);
      int clear = VfxColor.argb(0, rgb);
      float z = -length;
      VfxGeometry.annulusXY(pose, vc, z, Math.max(0.0F, mouth - halfW), mouth, 32, 1.0F, 0.0F, 0.0F, clear, bright);
      VfxGeometry.annulusXY(pose, vc, z, mouth, mouth + halfW, 32, 1.0F, 0.0F, 0.0F, bright, clear);
   }

   private static void starburst(Pose pose, VertexConsumer vc, float age, float lenMin, float lenMax, float width, float in, int seed, int hotRgb, int bodyRgb) {
      if (!(age < 0.0F) && !(age >= 6.0F)) {
         float t = age / 6.0F;
         float fade = (1.0F - t) * (1.0F - t);
         float reach = AnimEnvelopes.easeOutCubic(age / 2.0F);
         int rootCol = VfxColor.argb(VfxDraw.ia(230.0F * fade, in), hotRgb);
         int midCol = VfxColor.argb(VfxDraw.ia(170.0F * fade, in), bodyRgb);
         int tipCol = VfxColor.argb(0, bodyRgb);

         for (int i = 0; i < 10; i++) {
            float a = ((float)i + 0.6F * (VelocityStreaks.noise(seed, i, 71) - 0.5F)) / 10.0F * (float) (Math.PI * 2);
            Vector3f out = new Vector3f(Mth.cos(a), Mth.sin(a), -0.45F).normalize();
            float len = (lenMin + (lenMax - lenMin) * VelocityStreaks.noise(seed, i, 72)) * reach;
            Vector3f tip = new Vector3f(out).mul(len);
            Vector3f mid = new Vector3f(tip).mul(0.45F);
            VelocityStreaks.streak(pose, vc, ORIGIN, mid, width, width * 0.7F, rootCol, midCol);
            VelocityStreaks.streak(pose, vc, mid, tip, width * 0.7F, 0.0F, midCol, tipCol);
         }
      }
   }

   private static void forks(Pose pose, VertexConsumer vc, float age, float k, float in, int seed, int hotRgb, int bodyRgb) {
      if (!(age >= 6.0F)) {
         float t = age / 6.0F;
         float fade = (1.0F - t) * (1.0F - t);
         int window = seed * 31 + (int)(age / 2.0F);
         int count = 2 + (int)(VelocityStreaks.noise(window, 0, 81) * 2.0F);
         int rootCol = VfxColor.argb(VfxDraw.ia(235.0F * fade, in), hotRgb);
         int tipCol = VfxColor.argb(VfxDraw.ia(120.0F * fade, in), bodyRgb);

         for (int f = 0; f < count; f++) {
            float a = VelocityStreaks.noise(window, f, 82) * (float) (Math.PI * 2);
            float len = (0.6F + 0.39999998F * VelocityStreaks.noise(window, f, 83)) * k;
            Vector3f tip = new Vector3f(Mth.cos(a) * len, Mth.sin(a) * len, -len * 0.5F);
            VelocityStreaks.lightning(pose, vc, ORIGIN, tip, 0.06F * k, 5, 0.16F, window * 7919 + f, rootCol, tipCol);
         }
      }
   }
}
