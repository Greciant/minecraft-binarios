package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.client.VelocityLaneTransitClient;
import com.awakenedmod.awakened.content.velocity.VelocityPalette;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

final class VelocityLaneSceneRenderer {
   private static final float FORWARD = -1.0F;
   private static final int DEEP_BLUE = 663162;
   private static final int BODY_BLUE = 1988328;
   private static final int RIM_BLUE = 7320831;
   private static final int PALE_CYAN = 12579071;
   private static final int HAZE_WHITE = 15268095;
   private static final int CORE_WHITE = 16055039;
   private static final int THROAT_BLUE = 728942;
   private static final float NEAR = 0.8F;
   private static final float FAR = 16.0F;
   private static final float RADIUS_FAR_FRACTION = 0.4F;
   private static final float VANISH_Z = 17.0F;
   private static final float[] LAYER_RADIUS = new float[]{2.0F, 2.9F, 4.2F};
   private static final float[] LAYER_SPEED = new float[]{1.0F, 0.65F, 0.4F};
   private static final float[] LAYER_ALPHA = new float[]{1.0F, 0.6F, 0.35F};
   private static final float[] LAYER_SPIN = new float[]{0.05F, -0.035F, 0.02F};
   private static final float LAYER_TWIST = 1.2F;
   private static final float SPEED_IDLE_FRACTION = 0.3F;
   private static final int SKIN_SEGS = 20;
   private static final int SKIN_STATIONS = 8;
   private static final float SKIN_ALPHA_NEAR = 0.1F;
   private static final float SKIN_ALPHA_FAR = 0.45F;
   private static final float SKIN_PULSE_TICKS = 12.0F;
   private static final float SKIN_PULSE = 0.15F;
   private static final int RINGS = 9;
   private static final int RING_SEGS = 24;
   private static final float RING_BAND = 0.16F;
   private static final float RING_ALPHA = 0.5F;
   private static final int STREAKS_BLUE = 36;
   private static final int STREAKS_RUNNER = 22;
   private static final float STREAK_LEN_MIN = 3.0F;
   private static final float STREAK_LEN_MAX = 8.0F;
   private static final float STREAK_SPEED_MIN = 4.0F;
   private static final float STREAK_SPEED_MAX = 9.0F;
   private static final float STREAK_W = 0.03F;
   private static final float STREAK_BLUE_ALPHA = 150.0F;
   private static final float STREAK_RUNNER_ALPHA = 210.0F;
   private static final float STREAK_INSET = 0.93F;
   private static final int FORKS_MIN = 2;
   private static final int FORKS_EXTRA = 3;
   private static final float FORK_DUTY = 0.7F;
   private static final int FORK_SEGS = 6;
   private static final float FORK_JITTER = 0.18F;
   private static final float FORK_ROOT_HALF = 0.05F;
   private static final float FORK_ALPHA = 220.0F;
   private static final float FORK_TIP_ALPHA = 90.0F;
   private static final int FORK_WINDOW_TICKS = 2;
   private static final int GHOSTS = 4;
   private static final float GHOST_SIDE_MIN = 1.1F;
   private static final float GHOST_SIDE_MAX = 1.7F;
   private static final float GHOST_HIP_Y = -0.75F;
   private static final float GHOST_AHEAD_MIN = 2.5F;
   private static final float GHOST_AHEAD_MAX = 5.5F;
   private static final int GHOST_WINDOW_TICKS = 6;
   private static final float GHOST_DUTY = 0.65F;
   private static final float GHOST_ALPHA = 170.0F;
   private static final float GHOST_FLICKER_FLOOR = 0.55F;
   private static final float GHOST_SMEAR_STEP = 0.45F;
   private static final float[] GHOST_SMEAR_ALPHA = new float[]{1.0F, 0.45F, 0.2F};
   private static final float GHOST_TORSO = 0.62F;
   private static final float GHOST_HEAD = 0.28F;
   private static final float GHOST_SHOULDER_HALF = 0.2F;
   private static final float GHOST_HIP_HALF = 0.12F;
   private static final float GHOST_UPPER_ARM = 0.32F;
   private static final float GHOST_FOREARM = 0.3F;
   private static final float GHOST_THIGH = 0.42F;
   private static final float GHOST_SHIN = 0.4F;
   private static final float GHOST_ARM_RAISE = 0.2F;
   private static final float GHOST_ARM_SWING = 0.7F;
   private static final float GHOST_ELBOW = 1.3F;
   private static final float GHOST_LEG_SWING = 0.8F;
   private static final float GHOST_KNEE = 1.1F;
   private static final float GHOST_LEAN = 0.25F;
   private static final float GHOST_TORSO_W = 0.09F;
   private static final float GHOST_HEAD_W = 0.12F;
   private static final float GHOST_LIMB_W = 0.06F;
   private static final int DISC_SEGS = 24;
   private static final float THROAT_ALPHA = 0.55F;
   private static final float HALO_R = 2.4F;
   private static final float HALO_GROW = 2.5F;
   private static final float HALO_ALPHA = 0.35F;
   private static final float HALO_ALPHA_EXIT = 0.6F;
   private static final float CORE_R = 0.9F;
   private static final float CORE_GROW = 1.5F;
   private static final float CORE_ALPHA = 0.75F;
   private static final float CORE_ALPHA_EXIT = 0.95F;
   private static final float BREATH = 0.12F;
   private static final float BREATH_TICKS = 30.0F;
   private static final float EXIT_RING_R = 1.5F;
   private static final float EXIT_RING_GROW = 3.0F;
   private static final float EXIT_RING_BAND = 0.35F;
   private static final float EXIT_RING_ALPHA = 150.0F;
   private static final float FLARE_Z_NEAR = 0.6F;
   private static final float FLARE_R0 = 1.4F;
   private static final float FLARE_R1 = 3.5F;
   private static final float FLARE_ALPHA = 0.95F;
   private static final float FLARE_EDGE_ALPHA = 0.7F;
   private static final Vector3f UP = new Vector3f(0.0F, 1.0F, 0.0F);
   private static final Vector3f AHEAD = new Vector3f(0.0F, 0.0F, -1.0F);
   private static final Vector3f SIDE = new Vector3f(1.0F, 0.0F, 0.0F);

   private VelocityLaneSceneRenderer() {
   }

   static void draw(PoseStack poseStack, VfxDraw.Sink sink, VelocityPalette palette, float partialTick, float env) {
      float age = VelocityLaneTransitClient.age(partialTick);
      float ramp = VelocityLaneTransitClient.ramp(partialTick);
      float exit = VelocityLaneTransitClient.exit(partialTick);
      float flare = VelocityLaneTransitClient.flare(partialTick);
      float cadence = VelocityLaneTransitClient.cadence(partialTick);
      float travel = (float)VelocityLaneTransitClient.speed() * age * (0.3F + 0.7F * ramp);
      float density = 0.35F + 0.65F * ramp;
      VfxDraw.custom(poseStack, sink, FxLayer.PAINT_THROUGH_WALLS, (pose, vc) -> {
         for (int layer = LAYER_RADIUS.length - 1; layer >= 0; layer--) {
            skin(pose, vc, layer, age, env * density);
            rings(pose, vc, layer, travel, env * density);
         }

         throat(pose, vc, age, env);
         mouth(pose, vc, age, env, exit);
         flareDisc(pose, vc, env, flare);
      });
      VfxDraw.custom(poseStack, sink, FxLayer.LIGHT_THROUGH_WALLS, (pose, vc) -> {
         streaks(pose, vc, travel, age, env * density, 36, 0, 12579071, 7320831, 150.0F);
         streaks(pose, vc, travel, age, env * density, 22, 100, palette.hot(), palette.body(), 210.0F);
         forks(pose, vc, age, ramp, env, palette);
         ghosts(pose, vc, age, cadence, ramp, env, palette);
         exitRing(pose, vc, age, env, exit);
      });
   }

   private static float radiusAt(int layer, float d) {
      float depth = Mth.clamp((d - 0.8F) / 15.2F, 0.0F, 1.0F);
      return LAYER_RADIUS[layer] * Mth.lerp(depth, 1.0F, 0.4F);
   }

   private static void skin(Pose pose, VertexConsumer vc, int layer, float age, float env) {
      float spin = age * LAYER_SPIN[layer];
      Vector3f[] prev = null;
      int prevCol = 0;

      for (int s = 0; s <= 8; s++) {
         float depth = (float)s / 8.0F;
         float d = Mth.lerp(depth, 0.8F, 16.0F);
         float r = radiusAt(layer, d);
         float pulse = 1.0F + 0.15F * Mth.sin(age * (float) (Math.PI * 2) / 12.0F - depth * (float) (Math.PI * 2));
         float alpha = Mth.lerp(depth, 0.1F, 0.45F) * LAYER_ALPHA[layer] * env * pulse;
         int col = VfxColor.argb(Math.round(255.0F * alpha), VelocityPalette.mix(663162, 7320831, depth * depth));
         Vector3f[] ring = new Vector3f[21];
         float twist = spin + depth * 1.2F;

         for (int i = 0; i <= 20; i++) {
            float theta = (float)i * (float) (Math.PI * 2) / 20.0F + twist;
            ring[i] = new Vector3f(Mth.cos(theta) * r, Mth.sin(theta) * r, -1.0F * d);
         }

         if (prev != null) {
            for (int i = 0; i < 20; i++) {
               VelocityStreaks.quad(pose, vc, prev[i], prevCol, prev[i + 1], prevCol, ring[i + 1], col, ring[i], col, false);
            }
         }

         prev = ring;
         prevCol = col;
      }
   }

   private static void rings(Pose pose, VertexConsumer vc, int layer, float travel, float env) {
      float span = 15.2F;
      float spacing = span / 9.0F;
      float scroll = travel * LAYER_SPEED[layer] % spacing;

      for (int i = 0; i < 9; i++) {
         float d = 0.8F + (((float)i * spacing - scroll) % span + span) % span;
         float depth = (d - 0.8F) / span;
         float fade = Math.min(1.0F, Math.min(depth / 0.12F, (1.0F - depth) / 0.25F));
         int col = VfxColor.argb(Math.round(127.5F * LAYER_ALPHA[layer] * env * fade), VelocityPalette.mix(12579071, 1988328, depth));
         band(pose, vc, d, radiusAt(layer, d), 0.16F, col, col, false);
      }
   }

   private static void streaks(Pose pose, VertexConsumer vc, float travel, float age, float env, int count, int salt, int headRgb, int tailRgb, float alpha) {
      float span = 15.2F;
      float pace = (float)VelocityLaneTransitClient.speed();

      for (int i = 0; i < count; i++) {
         int layer = VelocityStreaks.noise(i, salt, 6) < 0.55F ? 0 : (VelocityStreaks.noise(i, salt, 6) < 0.85F ? 1 : 2);
         float theta = VelocityStreaks.noise(i, salt, 1) * (float) (Math.PI * 2) + age * LAYER_SPIN[layer];
         float len = 3.0F + 5.0F * VelocityStreaks.noise(i, salt, 2);
         float speed = 4.0F + 5.0F * VelocityStreaks.noise(i, salt, 3);
         float cycle = span + len;
         float p = ((travel * speed / pace + VelocityStreaks.noise(i, salt, 4) * cycle) % cycle + cycle) % cycle;
         float head = 16.0F - p;
         float tail = head + len;
         if (!(tail < 0.8F)) {
            float h = Math.max(head, 0.8F);
            float t = Math.min(tail, 16.0F);
            if (!(t <= h)) {
               float inset = 0.93F * (0.9F + 0.1F * VelocityStreaks.noise(i, salt, 5));
               Vector3f from = wallPoint(layer, theta, h, inset);
               Vector3f to = wallPoint(layer, theta, t, inset);
               float born = Math.min(1.0F, p / 1.5F);
               int headCol = VfxColor.argb(Math.round(alpha * env * born * LAYER_ALPHA[layer]), headRgb);
               int tailCol = VfxColor.argb(0, tailRgb);
               VelocityStreaks.streak(pose, vc, from, to, 0.03F, 0.015F, headCol, tailCol);
            }
         }
      }
   }

   private static void forks(Pose pose, VertexConsumer vc, float age, float ramp, float env, VelocityPalette palette) {
      int window = (int)Math.floor((double)(age / 2.0F));
      int count = 2 + Math.round(3.0F * ramp);
      int root = VfxColor.argb(Math.round(220.0F * env), palette.hot());
      int tip = VfxColor.argb(Math.round(90.0F * env), palette.body());

      for (int i = 0; i < count; i++) {
         if (!(VelocityStreaks.noise(window, i, 7) > 0.7F)) {
            float t1 = VelocityStreaks.noise(window, i, 1) * (float) (Math.PI * 2);
            float t2 = t1 + (float) Math.PI * (0.5F + 0.5F * VelocityStreaks.noise(window, i, 2));
            float d1 = 2.3F + VelocityStreaks.noise(window, i, 3) * 12.0F;
            float d2 = d1 + VelocityStreaks.signed(window, i, 4) * 2.0F;
            int layer = VelocityStreaks.noise(window, i, 5) < 0.7F ? 0 : 1;
            Vector3f a = wallPoint(layer, t1, d1, 0.95F);
            Vector3f b = wallPoint(layer, t2, Mth.clamp(d2, 0.8F, 16.0F), 0.95F);
            VelocityStreaks.lightning(pose, vc, a, b, 0.05F, 6, 0.18F, window * 31 + i, root, tip);
         }
      }
   }

   private static void ghosts(Pose pose, VertexConsumer vc, float age, float cadence, float ramp, float env, VelocityPalette palette) {
      if (!(ramp <= 0.05F)) {
         int window = (int)Math.floor((double)(age / 6.0F));
         int flickerWindow = (int)Math.floor((double)(age / 2.0F));

         for (int k = 0; k < 4; k++) {
            if (!(VelocityStreaks.noise(k, window, 3) > 0.65F)) {
               float side = (k % 2 == 0 ? 1.0F : -1.0F) * Mth.lerp(VelocityStreaks.noise(k, window, 1), 1.1F, 1.7F);
               float ahead = Mth.lerp(VelocityStreaks.noise(k, window, 2), 2.5F, 5.5F);
               float flicker = 0.55F + 0.45F * VelocityStreaks.noise(k, flickerWindow, 5);
               float phase = cadence + (float)k * 1.7F;
               float alpha = 170.0F * env * ramp * flicker;

               for (int copy = 0; copy < GHOST_SMEAR_ALPHA.length; copy++) {
                  Vector3f hip = new Vector3f(side, -0.75F, -1.0F * (ahead - (float)copy * 0.45F));
                  figure(pose, vc, hip, phase, alpha * GHOST_SMEAR_ALPHA[copy], palette);
               }
            }
         }
      }
   }

   private static void figure(Pose pose, VertexConsumer vc, Vector3f hip, float phase, float alpha, VelocityPalette palette) {
      int core = VfxColor.argb(Math.round(alpha), palette.hot());
      int fringe = VfxColor.argb(Math.round(alpha * 0.55F), palette.body());
      Vector3f neck = lean(hip, 0.25F, 0.62F);
      Vector3f crown = lean(neck, 0.125F, 0.28F);
      VelocityStreaks.streak(pose, vc, hip, neck, 0.09F, 0.072000004F, fringe, core);
      VelocityStreaks.streak(pose, vc, neck, crown, 0.12F, 0.084F, core, fringe);

      for (int s = -1; s <= 1; s += 2) {
         float armPhase = phase + (s < 0 ? 0.0F : (float) Math.PI);
         Vector3f shoulder = new Vector3f(SIDE).mul((float)s * 0.2F).add(neck);
         float upper = 0.2F + 0.7F * Mth.sin(armPhase);
         Vector3f elbow = swing(shoulder, upper, 0.32F);
         Vector3f hand = swing(elbow, upper + 1.3F, 0.3F);
         VelocityStreaks.streak(pose, vc, shoulder, elbow, 0.06F, 0.048F, core, fringe);
         VelocityStreaks.streak(pose, vc, elbow, hand, 0.048F, 0.03F, fringe, core);
         float legPhase = armPhase + (float) Math.PI;
         float thigh = 0.8F * Mth.sin(legPhase);
         Vector3f hipJoint = new Vector3f(SIDE).mul((float)s * 0.12F).add(hip);
         Vector3f knee = swing(hipJoint, thigh, 0.42F);
         Vector3f foot = swing(knee, thigh - 1.1F * Math.max(0.0F, Mth.sin(legPhase)), 0.4F);
         VelocityStreaks.streak(pose, vc, hipJoint, knee, 0.06F, 0.048F, core, fringe);
         VelocityStreaks.streak(pose, vc, knee, foot, 0.048F, 0.03F, fringe, core);
      }
   }

   private static Vector3f lean(Vector3f from, float angle, float len) {
      return new Vector3f(UP).mul(Mth.cos(angle) * len).add(new Vector3f(AHEAD).mul(Mth.sin(angle) * len)).add(from);
   }

   private static Vector3f swing(Vector3f joint, float angle, float len) {
      return new Vector3f(UP).mul(-Mth.cos(angle) * len).add(new Vector3f(AHEAD).mul(Mth.sin(angle) * len)).add(joint);
   }

   private static float breath(float age) {
      return 1.0F + 0.12F * Mth.sin(age * (float) (Math.PI * 2) / 30.0F);
   }

   private static void throat(Pose pose, VertexConsumer vc, float age, float env) {
      int col = VfxColor.argb(Math.round(140.25F * env), 728942);
      disc(pose, vc, 17.5F, LAYER_RADIUS[2] * 0.4F * breath(age), col, col);
   }

   private static void mouth(Pose pose, VertexConsumer vc, float age, float env, float exit) {
      float b = breath(age);
      float haloAlpha = Mth.lerp(exit, 0.35F, 0.6F) * env;
      float coreAlpha = Mth.lerp(exit, 0.75F, 0.95F) * env;
      int haloRgb = VelocityPalette.mix(7320831, 15268095, exit);
      int halo = VfxColor.argb(Math.round(255.0F * haloAlpha), haloRgb);
      int core = VfxColor.argb(Math.round(255.0F * coreAlpha), 16055039);
      int clear = VfxColor.argb(0, haloRgb);
      disc(pose, vc, 17.0F, 2.4F * (1.0F + 2.5F * exit) * b, halo, clear);
      disc(pose, vc, 17.0F, 0.9F * (1.0F + 1.5F * exit) * b, core, clear);
   }

   private static void exitRing(Pose pose, VertexConsumer vc, float age, float env, float exit) {
      if (!(exit <= 0.0F)) {
         float r = (1.5F + 3.0F * exit) * breath(age);
         int col = VfxColor.argb(Math.round(150.0F * exit * env), 12579071);
         int edge = VfxColor.argb(0, 12579071);
         band(pose, vc, 16.5F, r, 0.35F, col, edge, true);
      }
   }

   private static void flareDisc(Pose pose, VertexConsumer vc, float env, float flare) {
      if (!(flare <= 0.0F)) {
         float s = AnimEnvelopes.smooth(flare);
         float z = Mth.lerp(s, 17.0F, 0.6F);
         float r = Mth.lerp(flare, 1.4F, 3.5F);
         int centre = VfxColor.argb(Math.round(242.25F * s * env), 16055039);
         int edge = VfxColor.argb(Math.round(178.5F * s * env), 15268095);
         disc(pose, vc, z, r, centre, edge);
      }
   }

   private static Vector3f wallPoint(int layer, float theta, float d, float inset) {
      float r = radiusAt(layer, d) * inset;
      return new Vector3f(Mth.cos(theta) * r, Mth.sin(theta) * r, -1.0F * d);
   }

   private static void band(Pose pose, VertexConsumer vc, float d, float r, float thick, int col, int farCol, boolean twoSided) {
      float zNear = -1.0F * (d - thick * 0.5F);
      float zFar = -1.0F * (d + thick * 0.5F);
      Vector3f prevNear = null;
      Vector3f prevFar = null;

      for (int i = 0; i <= 24; i++) {
         float theta = (float)i * (float) (Math.PI * 2) / 24.0F;
         Vector3f near = new Vector3f(Mth.cos(theta) * r, Mth.sin(theta) * r, zNear);
         Vector3f far = new Vector3f(Mth.cos(theta) * r, Mth.sin(theta) * r, zFar);
         if (prevNear != null) {
            VelocityStreaks.quad(pose, vc, prevNear, col, near, col, far, farCol, prevFar, farCol, twoSided);
         }

         prevNear = near;
         prevFar = far;
      }
   }

   private static void disc(Pose pose, VertexConsumer vc, float d, float r, int centreCol, int edgeCol) {
      Vector3f c = new Vector3f(0.0F, 0.0F, -1.0F * d);
      Vector3f prev = null;

      for (int i = 0; i <= 24; i++) {
         float theta = (float)i * (float) (Math.PI * 2) / 24.0F;
         Vector3f p = new Vector3f(Mth.cos(theta) * r, Mth.sin(theta) * r, -1.0F * d);
         if (prev != null) {
            VelocityStreaks.tri(pose, vc, c, centreCol, prev, edgeCol, p, edgeCol, false);
         }

         prev = p;
      }
   }
}
