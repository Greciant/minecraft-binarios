package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.velocity.VelocityFx;
import com.awakenedmod.awakened.content.velocity.VelocityPalette;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

final class VelocityTimeFieldDrawer {
   static final int FADE_IN_TICKS = 6;
   static final int FADE_OUT_TICKS = 6;
   private static final float BAND_WIDTH = 1.4F;
   private static final float BAND_ALPHA = 0.42F;
   private static final float SKIRT_HEIGHT = 0.5F;
   private static final float SKIRT_ALPHA = 0.36F;
   private static final int CRAWL_WAVES = 5;
   private static final float CRAWL_DEPTH = 0.45F;
   private static final float CRAWL_RAD_PER_TICK = 0.004F;
   private static final float GRAIN = 0.15F;
   private static final int GRAIN_BUCKETS = 120;
   private static final float GROUND_Y = 0.03F;
   private static final float SEGMENT_LENGTH = 1.25F;
   private static final int SEGMENTS_MIN = 24;
   private static final int SEGMENTS_MAX = 192;
   private static final float MIN_RADIUS = 0.5F;
   private static final float HAIRLINE_WIDTH = 0.12F;
   private static final float HAIRLINE_ALPHA = 90.0F;
   private static final float LIP_HEIGHT = 0.14F;
   private static final float LIP_ALPHA = 55.0F;
   private static final int TONGUE_SLOTS = 3;
   private static final int TONGUE_PERIOD_TICKS = 60;
   private static final float TONGUE_LIFE_TICKS = 5.0F;
   private static final float TONGUE_LEN_MIN = 1.6F;
   private static final float TONGUE_LEN_MAX = 3.2F;
   private static final float TONGUE_RISE_MIN = 0.25F;
   private static final float TONGUE_RISE_MAX = 0.7F;
   private static final int TONGUE_SEGS = 5;
   private static final float TONGUE_JITTER = 0.2F;
   private static final float TONGUE_ROOT_HALF = 0.07F;
   private static final float TONGUE_ALPHA = 220.0F;
   private static final int RIB_MIN = 6;
   private static final int RIB_MAX = 9;
   private static final float RIB_TOP_ELEV_DEG = 58.0F;
   private static final int RIB_SEGS = 10;
   private static final float RIB_JITTER_FRAC = 0.03F;
   private static final float RIB_CORE_HALF = 0.05F;
   private static final float RIB_CORE_ALPHA = 95.0F;
   private static final float RIB_HALO_HALF = 0.15F;
   private static final float RIB_HALO_ALPHA = 42.0F;
   private static final int RIB_REPICK_TICKS = 14;
   private static final float RIB_EDGE_TICKS = 3.0F;
   private static final float RIB_FLICKER_FLOOR = 0.55F;
   private static final int JITTER_WINDOW = 2;

   private VelocityTimeFieldDrawer() {
   }

   static float rampedRadius(float fullRadius, float age, float span) {
      float open = VelocityFx.ringFront(age / 8.0F);
      float sinceRelease = age - (span - 6.0F);
      float close = sinceRelease > 0.0F ? VelocityFx.ringFront(1.0F - sinceRelease / 8.0F) : 1.0F;
      return fullRadius * open * close;
   }

   static void draw(PoseStack ps, VfxDraw.Sink sink, VfxRenderState state, float life) {
      float age = state.ageInTicks - (float)state.delay;
      drawAt(ps, sink, state, rampedRadius(state.scale, age, (float)(state.lifetime - state.delay)));
   }

   static void drawLive(PoseStack ps, VfxDraw.Sink sink, VfxRenderState state) {
      drawAt(ps, sink, state, state.scale);
   }

   private static void drawAt(PoseStack ps, VfxDraw.Sink sink, VfxRenderState state, float radius) {
      float age = state.ageInTicks - (float)state.delay;
      float span = (float)(state.lifetime - state.delay);
      float env = Math.min(VelocityStreaks.ramp(age, 6.0F), span > 12.0F ? VelocityStreaks.ramp(span - age, 6.0F) : 1.0F);
      if (!(env <= 0.01F)) {
         if (!(radius < 0.5F)) {
            float in = state.intensity;
            int seed = state.seed;
            VelocityPalette palette = VelocityPalette.fromBody(state.color);
            int body = palette.body();
            int hot = palette.hot();
            int segs = Mth.clamp(Math.round((float) (Math.PI * 2) * radius / 1.25F), 24, 192);
            int window = (int)(age / 2.0F);
            VfxDraw.custom(ps, sink, FxLayer.PAINT, (pose, vc) -> groundRing(pose, vc, radius, segs, age, env, in, seed, body));
            VfxDraw.additive(ps, sink, (pose, vc) -> {
               rim(pose, vc, radius, segs, age, env, in, seed, body, hot);
               tongues(pose, vc, radius, age, env, in, seed, body, hot);
               ribs(pose, vc, radius, age, env, in, seed, window, body, hot);
            });
         }
      }
   }

   private static float crawl(int seed, float angle, float age) {
      float wave = 0.5F + 0.5F * Mth.sin(angle * 5.0F - age * 0.004F * 5.0F);
      int bucket = (int)(angle / (float) (Math.PI * 2) * 120.0F) % 120;
      return (1.0F - 0.45F * wave) * (1.0F + 0.15F * VelocityStreaks.signed(seed, bucket, 71));
   }

   private static void groundRing(Pose pose, VertexConsumer vc, float radius, int segs, float age, float env, float in, int seed, int body) {
      float bandA = 107.1F * env;
      float skirtA = 91.8F * env;
      float inner = Math.max(0.0F, radius - 1.4F);
      Vector3f o0 = new Vector3f();
      Vector3f o1 = new Vector3f();
      Vector3f i0 = new Vector3f();
      Vector3f i1 = new Vector3f();
      Vector3f t0 = new Vector3f();
      Vector3f t1 = new Vector3f();
      int clear = VfxColor.argb(0, body);

      for (int j = 0; j < segs; j++) {
         float a0 = (float)j / (float)segs * (float) (Math.PI * 2);
         float a1 = (float)(j + 1) / (float)segs * (float) (Math.PI * 2);
         float b0 = crawl(seed, a0, age);
         float b1 = crawl(seed, a1, age);
         float c0 = Mth.cos(a0);
         float z0 = Mth.sin(a0);
         float c1 = Mth.cos(a1);
         float z1 = Mth.sin(a1);
         o0.set(c0 * radius, 0.03F, z0 * radius);
         o1.set(c1 * radius, 0.03F, z1 * radius);
         i0.set(c0 * inner, 0.03F, z0 * inner);
         i1.set(c1 * inner, 0.03F, z1 * inner);
         t0.set(c0 * radius, 0.5F, z0 * radius);
         t1.set(c1 * radius, 0.5F, z1 * radius);
         int band0 = VfxColor.argb(alpha(bandA * b0, in), body);
         int band1 = VfxColor.argb(alpha(bandA * b1, in), body);
         int foot0 = VfxColor.argb(alpha(skirtA * b0, in), body);
         int foot1 = VfxColor.argb(alpha(skirtA * b1, in), body);
         VelocityStreaks.quad(pose, vc, i0, clear, i1, clear, o1, band1, o0, band0, false);
         VelocityStreaks.quad(pose, vc, o0, foot0, o1, foot1, t1, clear, t0, clear, false);
      }
   }

   private static void rim(Pose pose, VertexConsumer vc, float radius, int segs, float age, float env, float in, int seed, int body, int hot) {
      float outer = radius + 0.12F;
      Vector3f o0 = new Vector3f();
      Vector3f o1 = new Vector3f();
      Vector3f h0 = new Vector3f();
      Vector3f h1 = new Vector3f();
      Vector3f l0 = new Vector3f();
      Vector3f l1 = new Vector3f();
      int hotClear = VfxColor.argb(0, hot);
      int bodyClear = VfxColor.argb(0, body);

      for (int j = 0; j < segs; j++) {
         float a0 = (float)j / (float)segs * (float) (Math.PI * 2);
         float a1 = (float)(j + 1) / (float)segs * (float) (Math.PI * 2);
         float b0 = crawl(seed, a0, age);
         float b1 = crawl(seed, a1, age);
         float c0 = Mth.cos(a0);
         float z0 = Mth.sin(a0);
         float c1 = Mth.cos(a1);
         float z1 = Mth.sin(a1);
         o0.set(c0 * radius, 0.03F, z0 * radius);
         o1.set(c1 * radius, 0.03F, z1 * radius);
         h0.set(c0 * outer, 0.03F, z0 * outer);
         h1.set(c1 * outer, 0.03F, z1 * outer);
         l0.set(c0 * radius, 0.14F, z0 * radius);
         l1.set(c1 * radius, 0.14F, z1 * radius);
         int line0 = VfxColor.argb(alpha(90.0F * env * b0, in), hot);
         int line1 = VfxColor.argb(alpha(90.0F * env * b1, in), hot);
         int lip0 = VfxColor.argb(alpha(55.0F * env * b0, in), body);
         int lip1 = VfxColor.argb(alpha(55.0F * env * b1, in), body);
         VelocityStreaks.quad(pose, vc, o0, line0, o1, line1, h1, hotClear, h0, hotClear, true);
         VelocityStreaks.quad(pose, vc, o0, lip0, o1, lip1, l1, bodyClear, l0, bodyClear, true);
      }
   }

   private static void tongues(Pose pose, VertexConsumer vc, float radius, float age, float env, float in, int seed, int body, int hot) {
      Vector3f root = new Vector3f();
      Vector3f tip = new Vector3f();

      for (int slot = 0; slot < 3; slot++) {
         float phase = age + (float)slot * 20.0F;
         int cycle = (int)(phase / 60.0F);
         int c = seed * 17 + cycle * 131 + slot;
         float fireAt = VelocityStreaks.noise(c, 0, 81) * 55.0F;
         float t = (phase - (float)(cycle * 60) - fireAt) / 5.0F;
         if (!(t < 0.0F) && !(t >= 1.0F)) {
            float reach = VelocityStreaks.easeOut(t * 3.0F);
            float fade = (1.0F - t) * (1.0F - t) * env;
            float a = VelocityStreaks.noise(c, 1, 82) * (float) (Math.PI * 2);
            float len = (1.6F + 1.6F * VelocityStreaks.noise(c, 2, 83)) * reach;
            float rise = 0.25F + 0.45F * VelocityStreaks.noise(c, 3, 84);
            float cs = Mth.cos(a);
            float sn = Mth.sin(a);
            root.set(cs * radius, 0.03F, sn * radius);
            tip.set(cs * (radius + len), 0.03F + len * rise, sn * (radius + len));
            int window = (int)(age / 2.0F);
            VelocityStreaks.lightning(
               pose,
               vc,
               root,
               tip,
               0.07F,
               5,
               0.2F,
               c * 7919 + window,
               VfxColor.argb(alpha(220.0F * fade, in), hot),
               VfxColor.argb(alpha(77.0F * fade, in), body)
            );
         }
      }
   }

   private static void ribs(Pose pose, VertexConsumer vc, float radius, float age, float env, float in, int seed, int window, int body, int hot) {
      int gen = (int)(age / 14.0F);
      float inGen = age - (float)(gen * 14);
      float genEnv = Math.min(VelocityStreaks.ramp(inGen, 3.0F), VelocityStreaks.ramp(14.0F - inGen, 3.0F)) * env;
      if (!(genEnv <= 0.01F)) {
         int g = seed * 23 + gen * 977;
         int count = 6 + (int)(VelocityStreaks.noise(g, 0, 91) * 4.0F);
         float topElev = 1.012291F;
         float amp = radius * 0.03F;
         Vector3f prev = new Vector3f();
         Vector3f cur = new Vector3f();

         for (int r = 0; r < count; r++) {
            float a = VelocityStreaks.noise(g, r, 92) * (float) (Math.PI * 2);
            float flicker = 0.55F + 0.45F * VelocityStreaks.noise(g, r, 100 + window);
            float strength = genEnv * flicker;
            float cs = Mth.cos(a);
            float sn = Mth.sin(a);
            int jitterSeed = g * 31 + r * 7 + window * 3;
            ribPoint(prev, 0, cs, sn, radius, topElev, amp, jitterSeed);

            for (int i = 1; i <= 10; i++) {
               ribPoint(cur, i, cs, sn, radius, topElev, amp, jitterSeed);
               float f0 = ribFade((float)(i - 1) / 10.0F);
               float f1 = ribFade((float)i / 10.0F);
               VelocityStreaks.streak(
                  pose,
                  vc,
                  prev,
                  cur,
                  0.15F,
                  0.15F,
                  VfxColor.argb(alpha(42.0F * strength * f0, in), body),
                  VfxColor.argb(alpha(42.0F * strength * f1, in), body)
               );
               VelocityStreaks.streak(
                  pose, vc, prev, cur, 0.05F, 0.05F, VfxColor.argb(alpha(95.0F * strength * f0, in), hot), VfxColor.argb(alpha(95.0F * strength * f1, in), hot)
               );
               prev.set(cur);
            }
         }
      }
   }

   private static float ribFade(float u) {
      float d = 1.0F - u;
      return d * d;
   }

   private static void ribPoint(Vector3f out, int i, float cs, float sn, float radius, float topElev, float amp, int seed) {
      float u = (float)i / 10.0F;
      float elev = u * topElev;
      float ringR = radius * Mth.cos(elev);
      float y = radius * Mth.sin(elev);
      float envelope = i == 0 ? 0.0F : Mth.sin(u * (float) Math.PI * 0.5F + 0.4F);
      float side = VelocityStreaks.signed(seed, i, 11) * amp * envelope;
      float radial = VelocityStreaks.signed(seed, i, 23) * amp * envelope;
      float rr = ringR + radial;
      out.set(cs * rr - sn * side, y, sn * rr + cs * side);
   }

   private static int alpha(float a, float in) {
      return Mth.clamp(VfxDraw.ia(a, in), 0, 255);
   }
}
