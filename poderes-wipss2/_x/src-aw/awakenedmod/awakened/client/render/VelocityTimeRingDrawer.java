package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.velocity.VelocityFx;
import com.awakenedmod.awakened.content.velocity.VelocityPalette;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

final class VelocityTimeRingDrawer {
   static final int RING_TICKS = 8;
   private static final float BAND_HEIGHT = 0.6F;
   private static final float BAND_LEAN = 0.18F;
   private static final float BAND_ALPHA = 0.62F;
   private static final float BAND_TOP_ALPHA_FRAC = 0.12F;
   private static final float SKIRT_DEPTH = 1.8F;
   private static final float GRAIN = 0.22F;
   private static final int GRAIN_BUCKETS = 96;
   private static final float GROUND_Y = 0.03F;
   private static final float SEGMENT_LENGTH = 1.0F;
   private static final int SEGMENTS_MIN = 24;
   private static final int SEGMENTS_MAX = 192;
   private static final float MIN_FRONT = 0.25F;
   private static final float RIM_HEIGHT = 0.32F;
   private static final float RIM_ALPHA = 70.0F;
   private static final float HAIRLINE_WIDTH = 0.14F;
   private static final float HAIRLINE_ALPHA = 95.0F;
   private static final int SPARKS = 34;
   private static final float SPARK_LEN_MIN = 0.9F;
   private static final float SPARK_LEN_MAX = 2.1F;
   private static final float SPARK_W = 0.06F;
   private static final float SPARK_RISE = 0.32F;
   private static final float SPARK_TRAIL = 0.3F;
   private static final float SPARK_SCATTER = 0.8F;
   private static final float SPARK_ALPHA = 200.0F;
   private static final float SPARK_ALPHA_MIN_FRAC = 0.55F;
   private static final int TONGUE_MIN = 6;
   private static final int TONGUE_MAX = 10;
   private static final float TONGUE_LEN_MIN = 0.7F;
   private static final float TONGUE_LEN_MAX = 1.6F;
   private static final float TONGUE_RISE_MIN = 0.3F;
   private static final float TONGUE_RISE_MAX = 0.9F;
   private static final int TONGUE_SEGS = 4;
   private static final float TONGUE_JITTER = 0.22F;
   private static final float TONGUE_ROOT_HALF = 0.05F;
   private static final float TONGUE_ALPHA = 210.0F;
   private static final int JITTER_WINDOW = 2;
   private static final float ALPHA_IN_TICKS = 1.0F;
   private static final float FADE_OUT_TICKS = 2.5F;

   private VelocityTimeRingDrawer() {
   }

   static float front(float age, float radius, boolean opening) {
      float t = Mth.clamp(age / 8.0F, 0.0F, 1.0F);
      return radius * VelocityFx.ringFront(opening ? t : 1.0F - t);
   }

   static void draw(PoseStack ps, VfxDraw.Sink sink, VfxRenderState state, float life) {
      float age = state.ageInTicks - (float)state.delay;
      if (!(age < 0.0F)) {
         float span = Math.max(1.0F, (float)(state.lifetime - state.delay));
         boolean opening = state.secondary.y >= 0.0;
         float radius = state.scale;
         float front = front(age, radius, opening);
         if (!(front < 0.25F)) {
            float env = Math.min(VelocityStreaks.ramp(age, 1.0F), VelocityStreaks.ramp(span - age, 2.5F));
            if (!(env <= 0.01F)) {
               float in = state.intensity;
               int seed = state.seed;
               VelocityPalette palette = VelocityPalette.fromBody(state.color);
               int body = palette.body();
               int hot = palette.hot();
               int dark = palette.dark();
               float travel = opening ? 1.0F : -1.0F;
               int segs = Mth.clamp(Math.round((float) (Math.PI * 2) * front / 1.0F), 24, 192);
               int window = (int)(age / 2.0F);
               VfxDraw.custom(ps, sink, FxLayer.PAINT, (pose, vc) -> band(pose, vc, front, travel, segs, env, in, seed, dark));
               VfxDraw.additive(ps, sink, (pose, vc) -> {
                  rim(pose, vc, front, travel, segs, env, in, body, hot);
                  sparks(pose, vc, front, travel, env, in, seed, body, hot);
                  tongues(pose, vc, front, travel, env, in, seed, window, body, hot);
               });
            }
         }
      }
   }

   private static void band(Pose pose, VertexConsumer vc, float front, float travel, int segs, float env, float in, int seed, int dark) {
      float footA = 158.1F * env;
      float topA = footA * 0.12F;
      float skirtIn = Math.max(0.0F, front - travel * 1.8F);
      float lean = front + travel * 0.18F;
      Vector3f f0 = new Vector3f();
      Vector3f f1 = new Vector3f();
      Vector3f t0 = new Vector3f();
      Vector3f t1 = new Vector3f();
      Vector3f s0 = new Vector3f();
      Vector3f s1 = new Vector3f();

      for (int j = 0; j < segs; j++) {
         float a0 = (float)j / (float)segs * (float) (Math.PI * 2);
         float a1 = (float)(j + 1) / (float)segs * (float) (Math.PI * 2);
         float g0 = grain(seed, a0);
         float g1 = grain(seed, a1);
         float c0 = Mth.cos(a0);
         float z0 = Mth.sin(a0);
         float c1 = Mth.cos(a1);
         float z1 = Mth.sin(a1);
         f0.set(c0 * front, 0.03F, z0 * front);
         f1.set(c1 * front, 0.03F, z1 * front);
         t0.set(c0 * lean, 0.6F, z0 * lean);
         t1.set(c1 * lean, 0.6F, z1 * lean);
         s0.set(c0 * skirtIn, 0.03F, z0 * skirtIn);
         s1.set(c1 * skirtIn, 0.03F, z1 * skirtIn);
         int foot0 = VfxColor.argb(alpha(footA * g0, in), dark);
         int foot1 = VfxColor.argb(alpha(footA * g1, in), dark);
         int top0 = VfxColor.argb(alpha(topA * g0, in), dark);
         int top1 = VfxColor.argb(alpha(topA * g1, in), dark);
         int clear = VfxColor.argb(0, dark);
         VelocityStreaks.quad(pose, vc, f0, foot0, f1, foot1, t1, top1, t0, top0, false);
         VelocityStreaks.quad(pose, vc, s0, clear, s1, clear, f1, foot1, f0, foot0, false);
      }
   }

   private static void rim(Pose pose, VertexConsumer vc, float front, float travel, int segs, float env, float in, int body, int hot) {
      int rimBase = VfxColor.argb(alpha(70.0F * env, in), body);
      int rimTop = VfxColor.argb(0, body);
      int lineIn = VfxColor.argb(alpha(95.0F * env, in), hot);
      int lineOut = VfxColor.argb(0, hot);
      float ahead = front + travel * 0.14F;
      Vector3f f0 = new Vector3f();
      Vector3f f1 = new Vector3f();
      Vector3f t0 = new Vector3f();
      Vector3f t1 = new Vector3f();
      Vector3f h0 = new Vector3f();
      Vector3f h1 = new Vector3f();

      for (int j = 0; j < segs; j++) {
         float a0 = (float)j / (float)segs * (float) (Math.PI * 2);
         float a1 = (float)(j + 1) / (float)segs * (float) (Math.PI * 2);
         float c0 = Mth.cos(a0);
         float z0 = Mth.sin(a0);
         float c1 = Mth.cos(a1);
         float z1 = Mth.sin(a1);
         f0.set(c0 * front, 0.03F, z0 * front);
         f1.set(c1 * front, 0.03F, z1 * front);
         t0.set(c0 * front, 0.32F, z0 * front);
         t1.set(c1 * front, 0.32F, z1 * front);
         h0.set(c0 * ahead, 0.03F, z0 * ahead);
         h1.set(c1 * ahead, 0.03F, z1 * ahead);
         VelocityStreaks.quad(pose, vc, f0, rimBase, f1, rimBase, t1, rimTop, t0, rimTop, true);
         VelocityStreaks.quad(pose, vc, f0, lineIn, f1, lineIn, h1, lineOut, h0, lineOut, true);
      }
   }

   private static void sparks(Pose pose, VertexConsumer vc, float front, float travel, float env, float in, int seed, int body, int hot) {
      Vector3f root = new Vector3f();
      Vector3f tip = new Vector3f();

      for (int i = 0; i < 34; i++) {
         float a = ((float)i + 0.8F * (VelocityStreaks.noise(seed, i, 41) - 0.5F)) / 34.0F * (float) (Math.PI * 2);
         float len = 0.9F + 1.1999999F * VelocityStreaks.noise(seed, i, 42);
         float strength = env * (0.55F + 0.45F * VelocityStreaks.noise(seed, i, 43));
         float c = Mth.cos(a);
         float s = Mth.sin(a);
         float r0 = front - travel * len * 0.3F;
         float r1 = front + travel * len * 0.7F;
         root.set(c * r0, 0.03F, s * r0);
         tip.set(c * r1, 0.03F + len * 0.32F, s * r1);
         VelocityStreaks.streak(pose, vc, root, tip, 0.06F, 0.0F, VfxColor.argb(alpha(200.0F * strength, in), hot), VfxColor.argb(0, body));
      }
   }

   private static void tongues(Pose pose, VertexConsumer vc, float front, float travel, float env, float in, int seed, int window, int body, int hot) {
      int w = seed * 31 + window;
      int count = 6 + (int)(VelocityStreaks.noise(w, 0, 51) * 5.0F);
      int rootCol = VfxColor.argb(alpha(210.0F * env, in), hot);
      int tipCol = VfxColor.argb(alpha(84.0F * env, in), body);
      Vector3f root = new Vector3f();
      Vector3f tip = new Vector3f();

      for (int f = 0; f < count; f++) {
         float a = VelocityStreaks.noise(w, f, 52) * (float) (Math.PI * 2);
         float len = 0.7F + 0.90000004F * VelocityStreaks.noise(w, f, 53);
         float rise = 0.3F + 0.59999996F * VelocityStreaks.noise(w, f, 54);
         float c = Mth.cos(a);
         float s = Mth.sin(a);
         float r1 = front + travel * len;
         root.set(c * front, 0.03F, s * front);
         tip.set(c * r1, 0.03F + len * rise, s * r1);
         VelocityStreaks.lightning(pose, vc, root, tip, 0.05F, 4, 0.22F, w * 7919 + f, rootCol, tipCol);
      }
   }

   private static float grain(int seed, float angle) {
      int bucket = (int)(angle / (float) (Math.PI * 2) * 96.0F) % 96;
      return 1.0F + 0.22F * VelocityStreaks.signed(seed, bucket, 61);
   }

   private static int alpha(float a, float in) {
      return Mth.clamp(VfxDraw.ia(a, in), 0, 255);
   }
}
