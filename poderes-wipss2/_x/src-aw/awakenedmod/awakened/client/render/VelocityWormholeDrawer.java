package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.velocity.VelocityFx;
import com.awakenedmod.awakened.content.velocity.VelocityPalette;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class VelocityWormholeDrawer {
   private static final float HALF_HEIGHT = 1.4F;
   private static final float HALF_WIDTH = 1.2F;
   private static final float GROUND_Y = -1.6F;
   private static final float OPEN_TICKS = 6.0F;
   private static final float CLOSE_TICKS = 5.0F;
   private static final float SETTLE_AT = 40.0F;
   private static final float SETTLE_TICKS = 20.0F;
   private static final float HOLD_LEVEL = 0.6F;
   private static final float PAINT_IDLE_FLOOR = 0.7F;
   private static final float WINDOW_TICKS = 2.0F;
   private static final int BODY_BLUE = 1988328;
   private static final int SKY_BLUE = 3837183;
   private static final int RIM_BLUE = 7320831;
   private static final int PALE_BLUE = 9422079;
   private static final int PALE_CYAN = 12579071;
   private static final int HAZE_WHITE = 15268095;
   private static final int CORE_WHITE = 16055039;
   private static final float HEART_R = 0.6F;
   private static final float[] HEART_FRACS = new float[]{0.0F, 0.75F, 1.0F};
   private static final float[] HEART_ALPHAS = new float[]{0.92F, 0.92F, 0.7F};
   private static final int STACK_LAYERS = 5;
   private static final float[] STACK_RADII = new float[]{0.75F, 0.9F, 1.0F, 1.12F, 1.28F};
   private static final float[] STACK_ALPHAS = new float[]{0.55F, 0.5F, 0.42F, 0.28F, 0.16F};
   private static final int[] STACK_COLS = new int[]{1988328, 1988328, 3837183, 7320831, 9422079};
   private static final float[] STACK_FRACS = new float[]{0.0F, 0.72F, 1.0F};
   private static final int[][] STACK_HARMONICS = new int[][]{{2, 3, 4}, {3, 5}, {2, 5, 7}, {3, 4}, {2, 3, 6}};
   private static final float[][] STACK_AMPS = new float[][]{
      {0.06F, 0.05F, 0.04F}, {0.07F, 0.05F}, {0.06F, 0.05F, 0.04F}, {0.08F, 0.06F}, {0.09F, 0.06F, 0.05F}
   };
   private static final float[] STACK_RATES = new float[]{0.03F, -0.05F, 0.04F, -0.07F, 0.09F};
   private static final float DRIFT_AMP_MIN = 0.05F;
   private static final float DRIFT_AMP_MAX = 0.09F;
   private static final float DRIFT_TICKS_MIN = 70.0F;
   private static final float DRIFT_TICKS_MAX = 130.0F;
   private static final float STACK_Z_NEAR = -0.075F;
   private static final float STACK_Z_STEP = 0.015F;
   private static final int RIM_LAYER = 2;
   private static final float RIM_BREATH = 0.03F;
   private static final int DISC_SEGS = 28;
   private static final int RING_SEGS = 24;
   private static final float[] BLOOM_RADII = new float[]{0.08F, 0.15F, 0.25F, 0.4F, 0.6F};
   private static final float[] BLOOM_ALPHAS = new float[]{230.0F, 180.0F, 110.0F, 60.0F, 30.0F};
   private static final int[] BLOOM_COLS = new int[]{16055039, 16055039, 15268095, 12579071, 7320831};
   private static final float BLOOM_Z = 0.11F;
   private static final float UNDERLAY_R = 0.45F;
   private static final float[] UNDERLAY_FRACS = new float[]{0.0F, 0.3F, 0.62F, 1.0F};
   private static final float[] UNDERLAY_ALPHAS = new float[]{0.97F, 0.86F, 0.45F, 0.0F};
   private static final int[] UNDERLAY_COLS = new int[]{16055039, 15268095, 12579071, 12579071};
   private static final float UNDERLAY_Z = -0.085F;
   private static final float CORE_BREATH = 0.15F;
   private static final float CORE_BREATH_TICKS = 40.0F;
   private static final float CORE_MIN_R = 0.08F;
   private static final float CORE_FLARE = 1.8F;
   private static final float BLOOM_SURGE = 1.3F;
   private static final int BANDS = 3;
   private static final int BAND_SEGS = 10;
   private static final float BAND_TURN = 3.6F;
   private static final float BAND_RATE = 0.09F;
   private static final float BAND_R_MIN = 0.12F;
   private static final float BAND_R_MAX = 0.9F;
   private static final float BAND_HALF_W_MIN = 0.125F;
   private static final float BAND_HALF_W_MAX = 0.2F;
   private static final float BAND_LIGHT_W_FRAC = 0.5F;
   private static final float BAND_PAINT_ALPHA = 0.23F;
   private static final float BAND_LIGHT_ALPHA = 40.0F;
   private static final float BAND_PULSES = 2.0F;
   private static final float BAND_DRIFT = 0.004F;
   private static final float BAND_Z = -0.09F;
   private static final int PETALS = 36;
   private static final float PETAL_ROOT = 0.6F;
   private static final float PETAL_LEN_MIN = 0.6F;
   private static final float PETAL_LEN_MAX = 1.0F;
   private static final float PETAL_W_MIN = 0.4F;
   private static final float PETAL_W_MAX = 0.7F;
   private static final float PETAL_WAIST = 0.55F;
   private static final int PETAL_VERTS = 8;
   private static final float PETAL_SHAPE = 0.8F;
   private static final float PETAL_PAINT_ALPHA_MIN = 90.0F;
   private static final float PETAL_PAINT_ALPHA_MAX = 120.0F;
   private static final float PETAL_LIGHT_ALPHA_MIN = 45.0F;
   private static final float PETAL_LIGHT_ALPHA_MAX = 70.0F;
   private static final float PETAL_BREATH = 0.2F;
   private static final float PETAL_BREATH_TICKS_MIN = 50.0F;
   private static final float PETAL_BREATH_TICKS_MAX = 90.0F;
   private static final float PETAL_SLOW_BREATH = 0.12F;
   private static final float PETAL_SLOW_TICKS_MIN = 140.0F;
   private static final float PETAL_SLOW_TICKS_MAX = 220.0F;
   private static final float PETAL_DRIFT = 0.003F;
   private static final float PETAL_SCATTER = 0.5F;
   private static final int PETAL_WRAP_EVERY = 3;
   private static final float PETAL_WRAP_MIN = 0.5F;
   private static final float PETAL_WRAP_MAX = 1.2F;
   private static final float PETAL_GROUND_FADE = 0.4F;
   private static final float LICK_EVERY_MIN = 60.0F;
   private static final float LICK_EVERY_MAX = 120.0F;
   private static final float LICK_TICKS = 18.0F;
   private static final float LICK_LEN = 1.3F;
   private static final float LICK_ALPHA = 1.25F;
   private static final float PETAL_Z = -0.1F;
   private static final float[] HALO_RADII = new float[]{1.4F, 1.65F, 1.9F};
   private static final float HALO_IN = 0.66F;
   private static final float HALO_PEAK = 1.0F;
   private static final float[] HALO_LIGHT_ALPHAS = new float[]{40.0F, 24.0F, 14.0F};
   private static final int HALO_SEGS = 20;
   private static final float HALO_FACE_LO = 0.2F;
   private static final float HALO_FACE_HI = 0.65F;
   private static final float HALO_PLANE_TILT = (float)Math.toRadians(60.0);
   private static final float HALO_GROUND_FADE = 1.1F;
   public static volatile VelocityWormholeDrawer.HaloMode haloMode = VelocityWormholeDrawer.HaloMode.PLANES;
   private static final float THROAT_DEPTH = 0.95F;
   private static final float[] THROAT_RADII = new float[]{0.85F, 0.7F, 0.52F, 0.36F};
   private static final float[] THROAT_DEPTHS = new float[]{0.2F, 0.45F, 0.7F, 0.95F};
   private static final float[] THROAT_LIGHT_ALPHAS = new float[]{34.0F, 52.0F, 74.0F, 100.0F};
   private static final float[] THROAT_PAINT_ALPHAS = new float[]{0.0F, 0.0F, 0.38F, 0.58F};
   private static final int[] THROAT_COLS = new int[]{7320831, 9422079, 9422079, 12579071};
   private static final float THROAT_RING_IN = 0.62F;
   private static final float THROAT_RING_PEAK = 0.86F;
   private static final int THROAT_SEGS = 20;
   private static final float THROAT_FLOW_TICKS = 40.0F;
   private static final float THROAT_FLOW_DEPTH = 0.3F;
   private static final float THROAT_EYE_R = 0.22F;
   private static final float THROAT_EYE_AT = 0.75F;
   private static final float THROAT_EYE_ALPHA = 110.0F;
   private static final float THROAT_EDGE_HI = 0.55F;
   private static final float THROAT_EDGE_LO = 0.08F;
   private static final float THROAT_EDGE_SQUASH = 0.5F;
   private static final float THROAT_TIP_DIM = 0.85F;
   private static final float THROAT_ALT_DIM = 0.5F;
   private static final float THROAT_HEART_R = 0.3F;
   private static final float THROAT_UNDERLAY_R = 0.28F;
   private static final float[] THROAT_STACK_FRACS = new float[]{0.0F, 0.42F, 0.72F, 1.0F};
   private static final float[] THROAT_STACK_MULS = new float[]{0.16F, 0.72F, 1.0F, 0.0F};
   private static final float[] STACK_MULS = new float[]{1.0F, 1.0F, 0.0F};
   private static final float PETAL_TORUS_SPREAD = 2.2F;
   private static final float PETAL_TORUS_MINOR = 0.3F;
   private static final float PETAL_TORUS_STEP = 0.6180339F;
   private static final float TENDRIL_WRAP_TORUS = 2.0F;
   private static final int THROAT_BANDS = 4;
   private static final float BAND_DEPTH = 0.85F;
   private static final float BAND_DEPTH_POWER = 1.3F;
   private static final int TENDRILS = 18;
   private static final float TENDRIL_LIT = 0.7F;
   private static final float TENDRIL_LEN_MIN = 0.25F;
   private static final float TENDRIL_LEN_MAX = 0.7F;
   private static final float TENDRIL_CURL = 0.22F;
   private static final float TENDRIL_WRAP = 1.0F;
   private static final float TENDRIL_W = 0.035F;
   private static final float TENDRIL_ALPHA = 160.0F;
   private static final int FACE_ARCS = 3;
   private static final int EDGE_ARCS = 1;
   private static final int RIM_ARCS = 1;
   private static final float ARC_LIT = 0.6F;
   private static final int FACE_ARC_SEGS = 8;
   private static final int EDGE_ARC_SEGS = 4;
   private static final int RIM_ARC_SEGS = 6;
   private static final float ARC_JITTER = 0.12F;
   private static final float EDGE_ARC_LEN_MIN = 0.2F;
   private static final float EDGE_ARC_LEN_MAX = 0.4F;
   private static final float EDGE_ARC_WRAP = 0.9F;
   private static final float RIM_ARC_SPAN_MIN = 0.5F;
   private static final float RIM_ARC_SPAN_MAX = 1.1F;
   private static final float RIM_ARC_BULGE = 0.15F;
   private static final float GROUND_ARC_LIT = 0.15F;
   private static final float GROUND_ARC_ROOT_SPREAD = 0.6F;
   private static final float GROUND_ARC_REACH_X = 0.5F;
   private static final float GROUND_ARC_REACH_Z = 0.4F;
   private static final int GROUND_ARC_SEGS = 5;
   private static final float ARC_W = 0.025F;
   private static final float ARC_HALO_W = 0.08F;
   private static final float ARC_HALO2_W = 0.18F;
   private static final float ARC_ALPHA = 230.0F;
   private static final float ARC_PAINT_W = 0.035F;
   private static final float ARC_PAINT_ALPHA = 0.9F;
   private static final float ARC_HALO_ALPHA = 70.0F;
   private static final float ARC_HALO2_ALPHA = 18.0F;
   private static final float FORK_ODDS = 0.7F;
   private static final float FORK_SECOND_ODDS = 0.25F;
   private static final float FORK_AT_MIN = 0.4F;
   private static final float FORK_AT_MAX = 0.6F;
   private static final float FORK_LEN_MIN = 0.25F;
   private static final float FORK_LEN_MAX = 0.4F;
   private static final int FORK_SEGS_MIN = 2;
   private static final int FORK_SEGS_MAX = 3;
   private static final float FORK_ANGLE_MIN = 0.6F;
   private static final float FORK_ANGLE_MAX = 1.1F;
   private static final float FORK_W = 0.6F;
   private static final float SURGE_EVERY_MIN = 70.0F;
   private static final float SURGE_EVERY_MAX = 120.0F;
   private static final float SURGE_TICKS = 2.0F;
   private static final float ARC_Z = -0.12F;
   private static final float DETAIL_Z = 0.03F;
   private static final float POOL_RX = 2.5F;
   private static final float POOL_RZ = 1.4F;
   private static final float[] POOL_FRACS = new float[]{0.0F, 0.3F, 0.65F, 1.0F};
   private static final float[] POOL_ALPHAS = new float[]{0.36F, 0.26F, 0.09F, 0.0F};
   private static final int[] POOL_COLS = new int[]{3837183, 3837183, 7320831, 9422079};
   private static final float POOL_BREATH = 0.1F;
   private static final float POOL_FLICKER = 0.12F;
   private static final int POOL_SEGS = 48;
   private static final float POOL_HOT_RX = 1.8F;
   private static final float POOL_HOT_RZ = 1.0F;
   private static final float POOL_HOT_ALPHA = 80.0F;
   private static final float POOL_Y = -1.5400001F;
   private static final int SNAP_STREAKS = 10;
   private static final float SNAP_LEN_MIN = 0.5F;
   private static final float SNAP_LEN_MAX = 1.2F;
   private static final float SNAP_W = 0.05F;
   private static final float SNAP_ALPHA = 200.0F;
   private static final int SWALLOW_STREAKS = 14;
   private static final float SWALLOW_LIFE = 8.0F;
   private static final float SWALLOW_REACH_TICKS = 3.0F;
   private static final float SWALLOW_FROM_MIN = 0.6F;
   private static final float SWALLOW_FROM_MAX = 1.6F;
   private static final float SWALLOW_SPREAD = 0.8F;
   private static final float SWALLOW_W = 0.06F;
   private static final float SWALLOW_JITTER = 0.15F;
   private static final int SWALLOW_SEGS = 5;
   private static final int EMERGE_STREAKS = 20;
   private static final float EMERGE_LIFE = 8.0F;
   private static final float EMERGE_SNAP_TICKS = 2.0F;
   private static final float EMERGE_LEN_MIN = 1.5F;
   private static final float EMERGE_LEN_MAX = 3.5F;
   private static final float EMERGE_BIAS = 1.4F;
   private static final float EMERGE_W = 0.07F;
   private static final float EMERGE_TIP = 0.6F;
   private static final float FLASH_ALPHA = 240.0F;
   private static final float FLASH_BODY_ALPHA = 170.0F;
   private static final Vector3f X_AXIS = new Vector3f(1.0F, 0.0F, 0.0F);
   private static final Vector3f Y_AXIS = new Vector3f(0.0F, 1.0F, 0.0F);
   private static final Vector3f Z_AXIS = new Vector3f(0.0F, 0.0F, 1.0F);
   private static final VelocityWormholeDrawer.Outline ROUND = (theta, frac) -> frac;

   private VelocityWormholeDrawer() {
   }

   static void draw(PoseStack ps, VfxDraw.Sink sink, VfxRenderState state, float life) {
      float age = state.ageInTicks - (float)state.delay;
      float span = (float)(state.lifetime - state.delay);
      boolean closeOnly = state.secondary.y < 0.0;
      VelocityFx.WormholeRole role = VelocityFx.WormholeRole.of((float)state.secondary.x);
      VelocityFx.WormholeVariant variant = VelocityFx.WormholeVariant.of((float)state.secondary.z);
      float k = state.scale;
      float remaining = span > 0.0F ? (1.0F - life) * span : Float.MAX_VALUE;
      float flare = 1.0F;
      float open;
      boolean closing;
      if (closeOnly) {
         float t = Mth.clamp(age / Math.max(1.0F, span), 0.0F, 1.0F);
         open = 1.0F - t * t;
         flare = 1.8F * (1.0F - t);
         closing = true;
      } else if (remaining < 5.0F) {
         float t = 1.0F - remaining / 5.0F;
         open = 1.0F - t * t;
         flare = 1.8F * (1.0F - t);
         closing = true;
      } else {
         float t = Mth.clamp(age / 6.0F, 0.0F, 1.0F);
         open = overshoot(t);
         flare = t < 1.0F ? 1.0F + 0.79999995F * Mth.sin(t * (float) Math.PI) : 1.0F;
         closing = false;
      }

      float settle = age < 40.0F ? 1.0F : Mth.lerp(AnimEnvelopes.smooth((age - 40.0F) / 20.0F), 1.0F, 0.6F);
      float in = state.intensity * (closeOnly ? 1.0F : settle);
      VelocityWormholeDrawer.Frame f = new VelocityWormholeDrawer.Frame(
         age, k, in, 1.2F * k, 1.4F * k, open, flare, (int)(age / 2.0F), state.seed, state.color, state.color2, closing, variant
      );
      if (open > 0.02F) {
         VfxDraw.custom(ps, sink, FxLayer.SOLID, (pose, vc) -> heart(pose, vc, f));
         VfxDraw.custom(ps, sink, FxLayer.PAINT, (pose, vc) -> {
            stack(pose, vc, f);
            if (f.throat()) {
               throatRings(pose, vc, f, false);
            }

            underlay(pose, vc, f);
            bands(pose, vc, f, false);
            petals(pose, vc, f, false);
            poolPaint(pose, vc, f);
            arcs(pose, vc, f, true);
         });
      }

      VfxDraw.additive(ps, sink, (pose, vc) -> {
         if (open > 0.02F) {
            halo(pose, vc, f);
            if (f.throat()) {
               throatRings(pose, vc, f, true);
               throatEye(pose, vc, f);
            }

            bands(pose, vc, f, true);
            petals(pose, vc, f, true);
            tendrils(pose, vc, f);
            arcs(pose, vc, f, false);
            poolHotSpot(pose, vc, f);
            bloom(pose, vc, f, 0.11F);
         }

         bloom(pose, vc, f, -0.11F);
         if (closing) {
            snapStreaks(pose, vc, f);
         }

         if (role == VelocityFx.WormholeRole.ENTRY) {
            swallow(pose, vc, f, age - 5.0F);
         } else if (role == VelocityFx.WormholeRole.EXIT) {
            emerge(pose, vc, f, age - 1.0F);
         }
      });
   }

   private static float overshoot(float t) {
      float c1 = 1.70158F;
      float c3 = c1 + 1.0F;
      float u = t - 1.0F;
      return 1.0F + c3 * u * u * u + c1 * u * u;
   }

   private static void heart(Pose pose, VertexConsumer vc, VelocityWormholeDrawer.Frame f) {
      int[] cols = new int[HEART_FRACS.length];

      for (int s = 0; s < cols.length; s++) {
         cols[s] = VfxColor.argb(paintAlpha(HEART_ALPHAS[s]), 3837183);
      }

      float r = f.throat() ? 0.3F : 0.6F;
      rings(pose, vc, f, 0.0F, 0.0F, f.rx() * r, f.ry() * r, 0.0F, HEART_FRACS, cols, 24, -1);
   }

   private static void stack(Pose pose, VertexConsumer vc, VelocityWormholeDrawer.Frame f) {
      float[] fracs = f.throat() ? THROAT_STACK_FRACS : STACK_FRACS;
      float[] muls = f.throat() ? THROAT_STACK_MULS : STACK_MULS;

      for (int i = 0; i < 5; i++) {
         int[] cols = new int[fracs.length];

         for (int s = 0; s < fracs.length; s++) {
            cols[s] = VfxColor.argb(paintAlpha(STACK_ALPHAS[i] * muls[s]), STACK_COLS[i]);
         }

         float z = -0.075F + (float)(4 - i) * 0.015F;
         rings(pose, vc, f, f.drift(i, 0), f.drift(i, 1), f.rx() * STACK_RADII[i], f.ry() * STACK_RADII[i], z, fracs, cols, 28, i);
      }
   }

   private static void underlay(Pose pose, VertexConsumer vc, VelocityWormholeDrawer.Frame f) {
      int[] cols = new int[UNDERLAY_FRACS.length];

      for (int s = 0; s < cols.length; s++) {
         cols[s] = VfxColor.argb(paintAlpha(UNDERLAY_ALPHAS[s]), UNDERLAY_COLS[s]);
      }

      float r = f.throat() ? 0.28F : 0.45F;
      rings(pose, vc, f, 0.0F, 0.0F, f.rx() * r, f.ry() * r, -0.085F, UNDERLAY_FRACS, cols, 24, -1);
   }

   private static void bands(Pose pose, VertexConsumer vc, VelocityWormholeDrawer.Frame f, boolean light) {
      int count = f.throat() ? 4 : 3;

      for (int b = 0; b < count; b++) {
         float sense = f.throat() && b % 2 == 1 ? -1.0F : 1.0F;
         float spin = f.age * 0.09F * sense;
         float theta0 = (float)b * (float) (Math.PI * 2) / (float)count + VelocityStreaks.noise(f.seed, b, 1040) * 0.8F;
         float halfW = (0.125F + 0.075F * VelocityStreaks.noise(f.seed, b, 1041)) * (light ? 0.5F : 1.0F) * f.k * f.open;
         float drift = f.age * 0.004F / 0.78F * 2.0F;
         Vector3f[] centre = new Vector3f[11];
         Vector3f[] side = new Vector3f[11];
         int[] cols = new int[11];

         for (int s = 0; s <= 10; s++) {
            float t = (float)s / 10.0F;
            float r = 0.12F + 0.78F * t;
            float theta = theta0 + t * 3.6F + spin;
            float cos = Mth.cos(theta);
            float sin = Mth.sin(theta);
            centre[s] = new Vector3f(cos * f.rx() * r, sin * f.ry() * r, f.throat() ? bandZ(f, r, sense) : -0.09F);
            float tx = f.rx() * (0.78F * cos - r * 3.6F * sin);
            float ty = f.ry() * (0.78F * sin + r * 3.6F * cos);
            side[s] = new Vector3f(-ty, tx, 0.0F).normalize().mul(halfW);
            float ends = Mth.sin(t * (float) Math.PI);
            float flow = 0.55F + 0.45F * Mth.sin((float) (Math.PI * 2) * (t * 2.0F + drift));
            float env = ends * flow;
            int alpha = light ? VfxDraw.ia(40.0F * env, f.in) : paintAlpha(0.23F * env * f.paintLight());
            cols[s] = VfxColor.argb(alpha, 7320831);
         }

         softRibbon(pose, vc, centre, side, cols, light);
      }
   }

   private static float bandZ(VelocityWormholeDrawer.Frame f, float r, float side) {
      float in = Mth.clamp((0.9F - r) / 0.78F, 0.0F, 1.0F);
      return side * 0.95F * 0.85F * (float)Math.pow((double)in, 1.3F) * f.k * f.open;
   }

   private static void petals(Pose pose, VertexConsumer vc, VelocityWormholeDrawer.Frame f, boolean light) {
      float spacing = 0.17453294F;

      for (int i = 0; i < 36; i++) {
         float period = 50.0F + 40.0F * VelocityStreaks.noise(f.seed, i, 960);
         float phase = VelocityStreaks.noise(f.seed, i, 961) * (float) (Math.PI * 2);
         float slowPeriod = 140.0F + 80.0F * VelocityStreaks.noise(f.seed, i, 968);
         float slowPhase = VelocityStreaks.noise(f.seed, i, 969) * (float) (Math.PI * 2);
         float breath = 1.0F
            + 0.2F * Mth.sin(f.age * (float) (Math.PI * 2) / period + phase)
            + 0.12F * Mth.sin(f.age * (float) (Math.PI * 2) / slowPeriod + slowPhase);
         float lickEvery = 60.0F + 60.0F * VelocityStreaks.noise(f.seed, i, 970);
         float lickT = (f.age + VelocityStreaks.noise(f.seed, i, 971) * lickEvery) % lickEvery;
         float lick = lickT >= 0.0F && lickT < 18.0F ? (1.0F - lickT / 18.0F) * (1.0F - lickT / 18.0F) : 0.0F;
         float theta = (float)i * spacing + VelocityStreaks.signed(f.seed, i, 962) * 0.5F * spacing + VelocityStreaks.signed(f.seed, i, 963) * 0.003F * f.age;
         float rimR = rimPoint(f, theta, 1.0F, 0.0F).length();
         float len = (0.6F + 0.39999998F * VelocityStreaks.noise(f.seed, i, 964)) * breath * (1.0F + 0.29999995F * lick) * rimR;
         float halfW = (0.4F + 0.29999998F * VelocityStreaks.noise(f.seed, i, 965)) * f.k * f.open;
         float wrap = f.throat()
            ? ((float)i * 0.6180339F % 1.0F * 2.0F - 1.0F) * 2.2F
            : (i % 3 == 0 ? Math.signum(VelocityStreaks.signed(f.seed, i, 966)) * (0.5F + 0.70000005F * VelocityStreaks.noise(f.seed, i, 972)) : 0.0F);
         float z = light ? -0.13F : -0.1F;
         Vector3f dir = wrapDir(f, theta, wrap);
         Vector3f root = f.throat() ? new Vector3f(dir).mul(0.3F * rimR - len * 0.55F).add(rimPoint(f, theta, 1.0F, z)) : rimPoint(f, theta, 0.6F, z);
         Vector3f side = new Vector3f(-Mth.sin(theta) * f.rx(), Mth.cos(theta) * f.ry(), 0.0F).normalize();
         float glow = (0.8F + 0.2F * Mth.sin(f.age * (float) (Math.PI * 2) / period + phase)) * (1.0F + 0.25F * lick);
         int tint = VelocityPalette.mix(7320831, 9422079, VelocityStreaks.noise(f.seed, i, 967));
         float base = light ? 45.0F + 25.0F * VelocityStreaks.noise(f.seed, i, 973) : 90.0F + 30.0F * VelocityStreaks.noise(f.seed, i, 973);
         float ground = light ? 1.0F : groundFadeAt(f, root.y + dir.y * len * 0.55F, 0.4F);
         int alpha = Math.min(255, light ? VfxDraw.ia(base * glow, f.in) : (int)(base * glow * f.paintLight() * ground));
         petal(pose, vc, root, dir, side, len, halfW, VfxColor.argb(alpha, light ? 7320831 : tint), light);
      }
   }

   private static void poolPaint(Pose pose, VertexConsumer vc, VelocityWormholeDrawer.Frame f) {
      float flicker = 0.88F + 0.12F * (float)litFaceArcs(f) / 3.0F;
      float breath = 1.0F + 0.1F * Mth.sin(f.age * (float) (Math.PI * 2) / 40.0F);
      float level = breath * flicker * f.paintLight() * f.open;
      int[] cols = new int[POOL_FRACS.length];

      for (int s = 0; s < cols.length; s++) {
         cols[s] = VfxColor.argb(paintAlpha(POOL_ALPHAS[s] * level), POOL_COLS[s]);
      }

      floorRings(pose, vc, f, 2.5F * f.halfW * f.open, 1.4F * f.halfW * f.open, POOL_FRACS, cols, false);
   }

   private static void halo(Pose pose, VertexConsumer vc, VelocityWormholeDrawer.Frame f) {
      if (haloMode != VelocityWormholeDrawer.HaloMode.OFF) {
         Vector3f toCam = cameraLocal(pose);
         if (!(toCam.lengthSquared() < 1.0E-4F)) {
            toCam.normalize();
            float breath = 1.0F + 0.03F * Mth.sin(f.age * (float) (Math.PI * 2) / 40.0F);
            if (haloMode == VelocityWormholeDrawer.HaloMode.BILLBOARD) {
               Vector3f hint = Math.abs(toCam.y) > 0.9F ? new Vector3f(1.0F, 0.0F, 0.0F) : new Vector3f(0.0F, 1.0F, 0.0F);
               Vector3f right = new Vector3f(hint).cross(toCam).normalize();
               Vector3f up = new Vector3f(toCam).cross(right).normalize();
               haloRings(pose, vc, f, right, up, 1.0F, breath);
            } else {
               float[] tilts = haloMode == VelocityWormholeDrawer.HaloMode.FLAT ? new float[]{0.0F} : new float[]{0.0F, HALO_PLANE_TILT, -HALO_PLANE_TILT};
               float[] weights = new float[tilts.length];
               float sum = 0.0F;

               for (int i = 0; i < tilts.length; i++) {
                  float cos = Mth.cos(tilts[i]);
                  float sin = Mth.sin(tilts[i]);
                  float facing = Math.abs(-sin * toCam.x + cos * toCam.z);
                  float w = Mth.clamp((facing - 0.2F) / 0.45F, 0.0F, 1.0F);
                  weights[i] = w * w * (3.0F - 2.0F * w);
                  sum += weights[i];
               }

               if (!(sum <= 0.0F)) {
                  float norm = 1.0F / Math.max(1.0F, sum);

                  for (int p = 0; p < tilts.length; p++) {
                     float weight = weights[p] * norm;
                     if (!(weight <= 0.01F)) {
                        Vector3f across = new Vector3f(Mth.cos(tilts[p]), 0.0F, Mth.sin(tilts[p]));
                        haloRings(pose, vc, f, across, Y_AXIS, weight, breath);
                     }
                  }
               }
            }
         }
      }
   }

   private static void haloRings(Pose pose, VertexConsumer vc, VelocityWormholeDrawer.Frame f, Vector3f a, Vector3f b, float weight, float breath) {
      VelocityWormholeDrawer.Fade ground = groundFade(f, 1.1F);

      for (int i = 0; i < HALO_RADII.length; i++) {
         float outer = HALO_RADII[i];
         float[] fracs = new float[]{0.66F / outer, 1.0F / outer, 1.0F};
         int alpha = VfxDraw.ia(HALO_LIGHT_ALPHAS[i] * f.open * weight, f.in);
         int[] cols = new int[]{VfxColor.argb(0, 9422079), VfxColor.argb(alpha, 9422079), VfxColor.argb(0, 9422079)};
         stations(pose, vc, new Vector3f(), a, b, outer * f.rx() * breath, outer * f.ry() * breath, fracs, cols, 20, true, ROUND, ground);
      }
   }

   private static Vector3f cameraLocal(Pose pose) {
      return new Matrix4f(pose.pose()).invert().transformPosition(new Vector3f());
   }

   private static float edgeOn(Pose pose) {
      Vector3f toCam = cameraLocal(pose);
      if (toCam.lengthSquared() < 1.0E-4F) {
         return 0.0F;
      } else {
         float facing = Math.abs(toCam.normalize().z);
         return AnimEnvelopes.smooth((0.55F - facing) / 0.47000003F);
      }
   }

   private static float tipDim(float at, float edge) {
      return 1.0F - edge * 0.85F * at;
   }

   private static float throatZ(VelocityWormholeDrawer.Frame f, float at, float edge, int s) {
      return (float)s * at * 0.95F * Mth.lerp(edge, 1.0F, 0.5F) * f.k * f.open;
   }

   private static int paintAlpha(float opacity) {
      return Math.round(255.0F * Mth.clamp(opacity, 0.0F, 1.0F));
   }

   private static int scaleAlpha(int col, float mul) {
      int a = Math.round((float)(col >>> 24 & 0xFF) * Mth.clamp(mul, 0.0F, 1.0F));
      return a << 24 | col & 16777215;
   }

   private static VelocityWormholeDrawer.Fade groundFade(VelocityWormholeDrawer.Frame f, float span) {
      float floor = -1.6F * f.k;
      float depth = Math.max(1.0E-4F, span * f.k);
      return p -> AnimEnvelopes.smooth(Mth.clamp((p.y - floor) / depth, 0.0F, 1.0F));
   }

   private static float groundFadeAt(VelocityWormholeDrawer.Frame f, float y, float span) {
      float floor = -1.6F * f.k;
      float depth = Math.max(1.0E-4F, span * f.k);
      return AnimEnvelopes.smooth(Mth.clamp((y - floor) / depth, 0.0F, 1.0F));
   }

   private static void bloom(Pose pose, VertexConsumer vc, VelocityWormholeDrawer.Frame f, float z) {
      float flash = Mth.clamp((f.flare - 1.0F) / 0.79999995F, 0.0F, 1.0F);
      float surge = f.surge() ? 1.3F : 1.0F;
      Vector3f c = new Vector3f(0.0F, 0.0F, z);

      for (int i = 0; i < BLOOM_RADII.length; i++) {
         float rx = Math.max(0.08F * f.k, BLOOM_RADII[i] * f.rx()) * f.breath();
         float ry = Math.max(0.08F * f.k, BLOOM_RADII[i] * f.ry()) * f.breath();
         int centre = VfxColor.argb(VfxDraw.ia(Mth.lerp(flash, Math.min(255.0F, BLOOM_ALPHAS[i] * surge), 255.0F), f.in), BLOOM_COLS[i]);
         int edge = VfxColor.argb(0, BLOOM_COLS[i]);
         Vector3f prev = null;

         for (int s = 0; s <= 24; s++) {
            float theta = (float)s * (float) (Math.PI * 2) / 24.0F;
            Vector3f p = new Vector3f(Mth.cos(theta) * rx, Mth.sin(theta) * ry, z);
            if (prev != null) {
               VelocityStreaks.tri(pose, vc, c, centre, prev, edge, p, edge, true);
            }

            prev = p;
         }
      }
   }

   private static void throatRings(Pose pose, VertexConsumer vc, VelocityWormholeDrawer.Frame f, boolean light) {
      float[] fracs = new float[]{0.62F, 0.86F, 1.0F};
      float edge = edgeOn(pose);

      for (int d = 0; d < THROAT_RADII.length; d++) {
         float base = light ? THROAT_LIGHT_ALPHAS[d] : THROAT_PAINT_ALPHAS[d];
         if (!(base <= 0.0F)) {
            float dim = tipDim(THROAT_DEPTHS[d], edge) * (d % 2 == 1 ? 1.0F - edge * 0.5F : 1.0F);
            float flow = 1.0F + 0.3F * Mth.sin((float) (Math.PI * 2) * ((float)d / (float)THROAT_RADII.length - f.age / 40.0F));
            int alpha = light ? VfxDraw.ia(base * dim * flow * f.open, f.in) : paintAlpha(base * dim * flow * f.paintLight() * f.open);
            int col = VfxColor.argb(alpha, THROAT_COLS[d]);
            int clear = VfxColor.argb(0, THROAT_COLS[d]);
            int[] cols = new int[]{clear, col, clear};
            float rx = f.rx() * THROAT_RADII[d];
            float ry = f.ry() * THROAT_RADII[d];
            int layer = d % 5;
            VelocityWormholeDrawer.Outline outline = (theta, frac) -> frac * f.wobble(theta, layer);

            for (int s = -1; s <= 1; s += 2) {
               float z = throatZ(f, THROAT_DEPTHS[d], edge, s);
               stations(pose, vc, new Vector3f(0.0F, 0.0F, z), X_AXIS, Y_AXIS, rx, ry, fracs, cols, 20, true, outline);
            }
         }
      }
   }

   private static void throatEye(Pose pose, VertexConsumer vc, VelocityWormholeDrawer.Frame f) {
      float edge = edgeOn(pose);
      int centre = VfxColor.argb(VfxDraw.ia(110.0F * tipDim(0.75F, edge) * f.open, f.in), 16055039);
      int[] cols = new int[]{centre, VfxColor.argb(0, 12579071)};
      float rx = f.rx() * 0.22F * f.breath();
      float ry = f.ry() * 0.22F * f.breath();

      for (int s = -1; s <= 1; s += 2) {
         float z = throatZ(f, 0.75F, edge, s);
         stations(pose, vc, new Vector3f(0.0F, 0.0F, z), X_AXIS, Y_AXIS, rx, ry, new float[]{0.0F, 1.0F}, cols, 24, true, ROUND);
      }
   }

   private static void tendrils(Pose pose, VertexConsumer vc, VelocityWormholeDrawer.Frame f) {
      for (int i = 0; i < 18; i++) {
         if (!(VelocityStreaks.noise(f.seed, f.window, 600 + i) > 0.7F)) {
            float theta = VelocityStreaks.noise(f.seed, i, 601) * (float) (Math.PI * 2) + VelocityStreaks.signed(f.seed, f.window, 602 + i) * 0.15F;
            float len = (0.25F + 0.45F * VelocityStreaks.noise(f.seed, f.window, 620 + i)) * f.k * f.open;
            float wrap = VelocityStreaks.signed(f.seed, i, 603) * (f.throat() ? 2.0F : 1.0F);
            Vector3f root = rimPoint(f, theta, 1.0F, 0.0F);
            Vector3f dir = wrapDir(f, theta, wrap);
            Vector3f side = new Vector3f(-Mth.sin(theta), Mth.cos(theta), 0.0F).mul(VelocityStreaks.signed(f.seed, f.window, 640 + i) * 0.22F * len);
            Vector3f mid = new Vector3f(dir).mul(len * 0.5F).add(side).add(root);
            Vector3f tip = new Vector3f(dir).mul(len).add(side).add(side).add(root);
            int rootCol = VfxColor.argb(VfxDraw.ia(160.0F * f.open, f.in), f.body);
            int midCol = VfxColor.argb(VfxDraw.ia(80.0F * f.open, f.in), f.body);
            int tipCol = VfxColor.argb(0, f.body);
            VelocityStreaks.streak(pose, vc, root, mid, 0.035F * f.k, 0.035F * f.k * 0.6F, rootCol, midCol);
            VelocityStreaks.streak(pose, vc, mid, tip, 0.035F * f.k * 0.6F, 0.0F, midCol, tipCol);
         }
      }
   }

   private static boolean arcLit(VelocityWormholeDrawer.Frame f, int salt) {
      return f.surge() || VelocityStreaks.noise(f.seed, f.window, salt) <= 0.6F;
   }

   private static boolean faceArcLit(VelocityWormholeDrawer.Frame f, int a) {
      return arcLit(f, 700 + a);
   }

   private static int litFaceArcs(VelocityWormholeDrawer.Frame f) {
      int lit = 0;

      for (int a = 0; a < 3; a++) {
         if (faceArcLit(f, a)) {
            lit++;
         }
      }

      return lit;
   }

   private static void arcs(Pose pose, VertexConsumer vc, VelocityWormholeDrawer.Frame f, boolean paint) {
      float jitter = 0.12F * f.k;

      for (int a = 0; a < 3; a++) {
         if (faceArcLit(f, a)) {
            Vector3f from = rimPoint(f, VelocityStreaks.noise(f.seed, f.window, 710 + a) * (float) (Math.PI * 2), 0.92F, -0.12F);
            Vector3f to = rimPoint(f, VelocityStreaks.noise(f.seed, f.window, 720 + a) * (float) (Math.PI * 2), 0.92F, -0.12F);
            heavyBolt(pose, vc, f, from, to, 8, jitter, f.seed * 7 + f.window * 131 + a, 1.0F, true, paint);
         }
      }

      for (int ax = 0; ax < 1; ax++) {
         if (arcLit(f, 750 + ax)) {
            float theta = VelocityStreaks.noise(f.seed, f.window, 760 + ax) * (float) (Math.PI * 2);
            float len = (0.2F + 0.2F * VelocityStreaks.noise(f.seed, f.window, 770 + ax)) * f.k * f.open;
            float wrap = VelocityStreaks.signed(f.seed, f.window, 780 + ax) * 0.9F;
            Vector3f from = rimPoint(f, theta, 0.98F, 0.0F);
            Vector3f to = wrapDir(f, theta, wrap).mul(len).add(from);
            heavyBolt(pose, vc, f, from, to, 4, jitter * 0.7F, f.seed * 11 + f.window * 97 + ax, 1.0F, true, paint);
         }
      }

      for (int axx = 0; axx < 1; axx++) {
         if (arcLit(f, 850 + axx)) {
            float theta = VelocityStreaks.noise(f.seed, f.window, 860 + axx) * (float) (Math.PI * 2);
            float span = 0.5F + 0.6F * VelocityStreaks.noise(f.seed, f.window, 870 + axx);
            float bulgeZ = VelocityStreaks.signed(f.seed, f.window, 880 + axx) * 0.15F * f.k;
            Vector3f from = rimPoint(f, theta, 1.0F, 0.0F);
            Vector3f mid = rimPoint(f, theta + span * 0.5F, 1.075F, bulgeZ);
            Vector3f to = rimPoint(f, theta + span, 1.0F, 0.0F);
            int seed = f.seed * 17 + f.window * 89 + axx;
            heavyBolt(pose, vc, f, from, mid, 3, jitter * 0.6F, seed, 1.0F, true, paint);
            heavyBolt(pose, vc, f, mid, to, 3, jitter * 0.6F, seed + 1, 1.0F, false, paint);
         }
      }

      if (f.surge() || VelocityStreaks.noise(f.seed, f.window, 890) <= 0.15F) {
         float theta = (float) (-Math.PI / 2) + VelocityStreaks.signed(f.seed, f.window, 891) * 0.6F;
         Vector3f from = rimPoint(f, theta, 1.0F, 0.0F);
         Vector3f to = new Vector3f(
            VelocityStreaks.signed(f.seed, f.window, 892) * 0.5F * f.halfW, -1.5400001F * f.k, VelocityStreaks.signed(f.seed, f.window, 893) * 0.4F * f.halfW
         );
         heavyBolt(pose, vc, f, from, to, 5, jitter * 0.7F, f.seed * 19 + f.window * 83, 1.0F, true, paint);
      }
   }

   private static void heavyBolt(
      Pose pose,
      VertexConsumer vc,
      VelocityWormholeDrawer.Frame f,
      Vector3f from,
      Vector3f to,
      int segs,
      float jitter,
      int seed,
      float scale,
      boolean forks,
      boolean paint
   ) {
      Vector3f[] joints = VelocityStreaks.boltJoints(from, to, segs, jitter, seed);
      if (joints != null) {
         int col = VfxColor.argb(VfxDraw.ia(230.0F * f.open, f.in), f.hot);
         int halo = VfxColor.argb(VfxDraw.ia(70.0F * f.open, f.in), f.body);
         int halo2 = VfxColor.argb(VfxDraw.ia(18.0F * f.open, f.in), f.body);
         int core = VfxColor.argb(paintAlpha(0.9F * f.paintLight() * f.open), 16055039);
         Vector3f[] flat = new Vector3f[joints.length];

         for (int i = 0; i < joints.length; i++) {
            Vector3f dir = new Vector3f(joints[Math.min(i + 1, segs)]).sub(joints[Math.max(i - 1, 0)]);
            flat[i] = flatSide(dir);
         }

         if (paint) {
            haloRibbon(pose, vc, joints, flat, 0.035F * f.k * scale, core, false);
         } else {
            haloRibbon(pose, vc, joints, flat, 0.18F * f.k * scale, halo2, true);
            haloRibbon(pose, vc, joints, flat, 0.08F * f.k * scale, halo, true);
            VelocityStreaks.boltPath(pose, vc, joints, 1.0F, 0.025F * f.k * scale, col, 0.0F, 0);
         }

         if (forks && segs >= 3 && !(VelocityStreaks.noise(seed, 2, 1050) > 0.7F)) {
            float trunkLen = new Vector3f(to).sub(from).length();
            int count = 1 + (VelocityStreaks.noise(seed, 0, 1050) < 0.25F ? 1 : 0);

            for (int n = 0; n < count; n++) {
               float at = 0.4F + 0.20000002F * VelocityStreaks.noise(seed, n, 1051);
               int j = Mth.clamp(Math.round(at * (float)segs) + n, 1, segs - 1);
               Vector3f trunk = new Vector3f(joints[j + 1]).sub(joints[j - 1]).normalize();
               Vector3f p1 = VelocityStreaks.perpendicular(trunk);
               Vector3f p2 = new Vector3f(trunk).cross(p1).normalize();
               float angle = Math.signum(VelocityStreaks.signed(seed, n, 1052)) * (0.6F + 0.5F * VelocityStreaks.noise(seed, n, 1053));
               Vector3f dir = new Vector3f(trunk)
                  .mul(Mth.cos(angle))
                  .add(new Vector3f(p1).mul(Mth.sin(angle)))
                  .add(new Vector3f(p2).mul(0.3F * VelocityStreaks.signed(seed, n, 1054)))
                  .normalize();
               float len = trunkLen * (0.25F + 0.15F * VelocityStreaks.noise(seed, n, 1055));
               int forkSegs = 2 + Math.round(1.0F * VelocityStreaks.noise(seed, n, 1056));
               Vector3f tip = new Vector3f(dir).mul(len).add(joints[j]);
               heavyBolt(pose, vc, f, joints[j], tip, forkSegs, jitter * 0.6F, seed * 31 + n + 1, scale * 0.6F, false, paint);
            }
         }
      }
   }

   private static void haloRibbon(Pose pose, VertexConsumer vc, Vector3f[] joints, Vector3f[] flat, float halfW, int col, boolean twoSided) {
      Vector3f[] side = new Vector3f[joints.length];
      int[] cols = new int[joints.length];

      for (int i = 0; i < joints.length; i++) {
         side[i] = new Vector3f(flat[i]).mul(halfW);
         cols[i] = col;
      }

      softRibbon(pose, vc, joints, side, cols, twoSided);
   }

   private static Vector3f flatSide(Vector3f dir) {
      Vector3f side = new Vector3f(dir).cross(Z_AXIS);
      return side.lengthSquared() < 1.0E-6F ? VelocityStreaks.perpendicular(new Vector3f(dir).normalize()) : side.normalize();
   }

   private static void poolHotSpot(Pose pose, VertexConsumer vc, VelocityWormholeDrawer.Frame f) {
      int centre = VfxColor.argb(VfxDraw.ia(80.0F * f.open * f.breath(), f.in), 12579071);
      int[] cols = new int[]{centre, VfxColor.argb(0, 7320831)};
      floorRings(pose, vc, f, 1.8F * f.halfW * f.open, 1.0F * f.halfW * f.open, new float[]{0.0F, 1.0F}, cols, true);
   }

   private static void snapStreaks(Pose pose, VertexConsumer vc, VelocityWormholeDrawer.Frame f) {
      float t = 1.0F - f.open;
      float fade = (1.0F - t) * (1.0F - t);
      int head = VfxColor.argb(VfxDraw.ia(200.0F * fade, f.in), 7320831);
      int tail = VfxColor.argb(0, 7320831);

      for (int i = 0; i < 10; i++) {
         float theta = VelocityStreaks.noise(f.seed, i, 800) * (float) (Math.PI * 2);
         float len = (0.5F + 0.70000005F * VelocityStreaks.noise(f.seed, i, 801)) * f.k;
         Vector3f dir = new Vector3f(Mth.cos(theta) * f.halfW, Mth.sin(theta) * f.halfH, 0.0F).normalize();
         Vector3f from = new Vector3f(dir).mul(f.halfW * 0.3F + len * t);
         Vector3f to = new Vector3f(dir).mul(f.halfW * 0.3F + len * (t + 0.5F));
         VelocityStreaks.streak(pose, vc, from, to, 0.05F * f.k, 0.0F, head, tail);
      }
   }

   private static void swallow(Pose pose, VertexConsumer vc, VelocityWormholeDrawer.Frame f, float t) {
      if (!(t < 0.0F) && !(t >= 8.0F)) {
         float fade = (1.0F - t / 8.0F) * (1.0F - t / 8.0F);
         float reach = AnimEnvelopes.easeOutCubic(t / 3.0F);
         int rootCol = VfxColor.argb(VfxDraw.ia(170.0F * fade, f.in), f.body);
         int tipCol = VfxColor.argb(VfxDraw.ia(240.0F * fade, f.in), f.hot);

         for (int i = 0; i < 14; i++) {
            float back = (0.6F + 1.0F * VelocityStreaks.noise(f.seed, i, 900)) * f.k;
            Vector3f root = new Vector3f(
               VelocityStreaks.signed(f.seed, i, 901) * 0.8F * f.halfW, VelocityStreaks.signed(f.seed, i, 902) * 0.8F * f.halfH, -back
            );
            Vector3f target = new Vector3f(
               VelocityStreaks.signed(f.seed, i, 903) * 0.2F * f.halfW, VelocityStreaks.signed(f.seed, i, 904) * 0.2F * f.halfH, -0.03F
            );
            Vector3f tip = new Vector3f(root).lerp(target, reach);
            VelocityStreaks.lightning(pose, vc, root, tip, 0.06F * f.k, 5, 0.15F, f.seed * 13 + i, rootCol, tipCol);
         }
      }
   }

   private static void emerge(Pose pose, VertexConsumer vc, VelocityWormholeDrawer.Frame f, float t) {
      if (!(t < 0.0F) && !(t >= 8.0F)) {
         float fade = (1.0F - t / 8.0F) * (1.0F - t / 8.0F);
         float reach = AnimEnvelopes.easeOutCubic(t / 2.0F);
         int tipCol = VfxColor.argb(VfxDraw.ia(240.0F * fade, f.in), f.hot);
         int midCol = VfxColor.argb(VfxDraw.ia(170.0F * fade, f.in), f.body);
         int rootCol = VfxColor.argb(0, f.body);

         for (int i = 0; i < 20; i++) {
            float z = VelocityStreaks.signed(f.seed, i, 950);
            float phi = VelocityStreaks.noise(f.seed, i, 951) * (float) (Math.PI * 2);
            float ring = Mth.sqrt(Math.max(0.0F, 1.0F - z * z));
            Vector3f dir = new Vector3f(ring * Mth.cos(phi), ring * Mth.sin(phi), -(z + 1.4F)).normalize();
            float len = (1.5F + 2.0F * VelocityStreaks.noise(f.seed, i, 952)) * f.k * (0.85F + 0.15F * reach);
            float rootR = len * 0.12F * reach;
            float tipR = len * reach;
            float midR = rootR + (tipR - rootR) * 0.6F;
            Vector3f root = new Vector3f(dir).mul(rootR).add(0.0F, 0.0F, -0.03F);
            Vector3f mid = new Vector3f(dir).mul(midR).add(0.0F, 0.0F, -0.03F);
            Vector3f tip = new Vector3f(dir).mul(tipR).add(0.0F, 0.0F, -0.03F);
            VelocityStreaks.streak(pose, vc, root, mid, 0.0F, 0.07F * f.k * 0.6F, rootCol, midCol);
            VelocityStreaks.streak(pose, vc, mid, tip, 0.07F * f.k * 0.6F, 0.07F * f.k, midCol, tipCol);
         }
      }
   }

   private static Vector3f rimPoint(VelocityWormholeDrawer.Frame f, float theta, float r, float z) {
      float w = f.wobble(theta);
      return new Vector3f(Mth.cos(theta) * f.rx() * r * w + f.drift(2, 0), Mth.sin(theta) * f.ry() * r * w + f.drift(2, 1), z);
   }

   private static Vector3f wrapDir(VelocityWormholeDrawer.Frame f, float theta, float wrap) {
      Vector3f out = new Vector3f(Mth.cos(theta) * f.ry(), Mth.sin(theta) * f.rx(), 0.0F).normalize();
      return out.mul(Mth.cos(wrap)).add(0.0F, 0.0F, Mth.sin(wrap)).normalize();
   }

   private static void rings(
      Pose pose,
      VertexConsumer vc,
      VelocityWormholeDrawer.Frame f,
      float cx,
      float cy,
      float rx,
      float ry,
      float z,
      float[] fracs,
      int[] cols,
      int segs,
      int layer
   ) {
      stations(
         pose,
         vc,
         new Vector3f(cx, cy, z),
         X_AXIS,
         Y_AXIS,
         rx,
         ry,
         fracs,
         cols,
         segs,
         false,
         (theta, frac) -> frac * (1.0F + (f.wobble(theta, layer) - 1.0F) * frac)
      );
   }

   private static void floorRings(Pose pose, VertexConsumer vc, VelocityWormholeDrawer.Frame f, float rx, float rz, float[] fracs, int[] cols, boolean twoSided) {
      stations(pose, vc, new Vector3f(0.0F, -1.5400001F * f.k, 0.0F), X_AXIS, Z_AXIS, rx, rz, fracs, cols, 48, twoSided, ROUND);
   }

   private static void stations(
      Pose pose,
      VertexConsumer vc,
      Vector3f centre,
      Vector3f a,
      Vector3f b,
      float ra,
      float rb,
      float[] fracs,
      int[] cols,
      int segs,
      boolean twoSided,
      VelocityWormholeDrawer.Outline outline
   ) {
      stations(pose, vc, centre, a, b, ra, rb, fracs, cols, segs, twoSided, outline, null);
   }

   private static void stations(
      Pose pose,
      VertexConsumer vc,
      Vector3f centre,
      Vector3f a,
      Vector3f b,
      float ra,
      float rb,
      float[] fracs,
      int[] cols,
      int segs,
      boolean twoSided,
      VelocityWormholeDrawer.Outline outline,
      VelocityWormholeDrawer.Fade fade
   ) {
      int first = fracs[0] == 0.0F ? 1 : 0;
      int centreCol = fade == null ? cols[0] : scaleAlpha(cols[0], fade.at(centre));
      Vector3f[] prev = null;
      int[] prevCols = null;

      for (int i = 0; i <= segs; i++) {
         float theta = (float)i * (float) (Math.PI * 2) / (float)segs;
         Vector3f[] ring = new Vector3f[fracs.length];
         int[] ringCols = new int[fracs.length];

         for (int s = first; s < fracs.length; s++) {
            float r = outline.at(theta, fracs[s]);
            ring[s] = new Vector3f(a).mul(Mth.cos(theta) * ra * r).add(new Vector3f(b).mul(Mth.sin(theta) * rb * r)).add(centre);
            ringCols[s] = fade == null ? cols[s] : scaleAlpha(cols[s], fade.at(ring[s]));
         }

         if (prev != null) {
            if (first == 1) {
               VelocityStreaks.tri(pose, vc, centre, centreCol, prev[1], prevCols[1], ring[1], ringCols[1], twoSided);
            }

            for (int s = first + 1; s < fracs.length; s++) {
               VelocityStreaks.quad(pose, vc, prev[s - 1], prevCols[s - 1], prev[s], prevCols[s], ring[s], ringCols[s], ring[s - 1], ringCols[s - 1], twoSided);
            }
         }

         prev = ring;
         prevCols = ringCols;
      }
   }

   private static void softRibbon(Pose pose, VertexConsumer vc, Vector3f[] centre, Vector3f[] side, int[] cols, boolean twoSided) {
      for (int i = 1; i < centre.length; i++) {
         Vector3f l0 = new Vector3f(centre[i - 1]).sub(side[i - 1]);
         Vector3f r0 = new Vector3f(centre[i - 1]).add(side[i - 1]);
         Vector3f l1 = new Vector3f(centre[i]).sub(side[i]);
         Vector3f r1 = new Vector3f(centre[i]).add(side[i]);
         int c0 = cols[i - 1];
         int c1 = cols[i];
         int e0 = c0 & 16777215;
         int e1 = c1 & 16777215;
         VelocityStreaks.quad(pose, vc, l0, e0, l1, e1, centre[i], c1, centre[i - 1], c0, twoSided);
         VelocityStreaks.quad(pose, vc, centre[i - 1], c0, centre[i], c1, r1, e1, r0, e0, twoSided);
      }
   }

   private static void petal(Pose pose, VertexConsumer vc, Vector3f root, Vector3f dir, Vector3f side, float len, float halfW, int col, boolean twoSided) {
      Vector3f c = new Vector3f(dir).mul(len * 0.55F).add(root);
      int clear = col & 16777215;
      Vector3f prev = null;

      for (int k = 0; k <= 8; k++) {
         float u = (float)k * (float) (Math.PI * 2) / 8.0F;
         float along = (float)Math.pow((double)(0.5F - 0.5F * Mth.cos(u)), 0.8F) * len;
         float across = Mth.sin(u) * halfW;
         Vector3f p = new Vector3f(dir).mul(along).add(new Vector3f(side).mul(across)).add(root);
         if (prev != null) {
            VelocityStreaks.tri(pose, vc, c, col, prev, clear, p, clear, twoSided);
         }

         prev = p;
      }
   }

   @FunctionalInterface
   private interface Fade {
      float at(Vector3f var1);
   }

   private static record Frame(
      float age,
      float k,
      float in,
      float halfW,
      float halfH,
      float open,
      float flare,
      int window,
      int seed,
      int body,
      int hot,
      boolean closing,
      VelocityFx.WormholeVariant variant
   ) {
      boolean throat() {
         return this.variant == VelocityFx.WormholeVariant.THROAT;
      }

      float rx() {
         return this.halfW * this.open;
      }

      float ry() {
         return this.halfH * this.open;
      }

      float breath() {
         return 1.0F + 0.15F * Mth.sin(this.age * (float) (Math.PI * 2) / 40.0F);
      }

      float paintLight() {
         return 0.7F + 0.3F * this.in;
      }

      boolean surge() {
         float every = 70.0F + 50.0F * VelocityStreaks.noise(this.seed, 0, 1000);
         float t = (this.age + VelocityStreaks.noise(this.seed, 1, 1000) * every) % every;
         return t >= 0.0F && t < 2.0F;
      }

      float wobble(float theta, int layer) {
         float w = 1.0F + 0.03F * Mth.sin(this.age * (float) (Math.PI * 2) / 40.0F);
         if (layer < 0) {
            return w;
         } else {
            int[] harmonics = VelocityWormholeDrawer.STACK_HARMONICS[layer];
            float[] amps = VelocityWormholeDrawer.STACK_AMPS[layer];
            float spin = this.age * VelocityWormholeDrawer.STACK_RATES[layer];

            for (int n = 0; n < harmonics.length; n++) {
               float phase = VelocityStreaks.noise(this.seed, layer * 8 + n, 990) * (float) (Math.PI * 2);
               w += amps[n] * Mth.sin((float)harmonics[n] * (theta - spin) + phase);
            }

            return w;
         }
      }

      float wobble(float theta) {
         return this.wobble(theta, 2);
      }

      float drift(int layer, int axis) {
         float amp = 0.05F + 0.040000003F * VelocityStreaks.noise(this.seed, layer, 1010 + axis);
         float period = 70.0F + 60.0F * VelocityStreaks.noise(this.seed, layer, 1020 + axis);
         float phase = VelocityStreaks.noise(this.seed, layer, 1030 + axis) * (float) (Math.PI * 2);
         return amp * Mth.sin(this.age * (float) (Math.PI * 2) / period + phase) * (axis == 0 ? this.rx() : this.ry());
      }
   }

   public static enum HaloMode {
      BILLBOARD,
      PLANES,
      FLAT,
      OFF;
   }

   @FunctionalInterface
   private interface Outline {
      float at(float var1, float var2);
   }
}
