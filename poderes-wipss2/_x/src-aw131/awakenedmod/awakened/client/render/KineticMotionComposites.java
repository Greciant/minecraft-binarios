package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.vfx.VfxEntity;
import com.awakenedmod.awakened.core.Lab;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class KineticMotionComposites {
   private static final int ENV_STEPS = 4;
   private static final float CUBE_ALPHA = 150.0F;
   private static final float WAKE_CORE_PER_BLOCK = 7.0F;
   private static final int WAKE_CORE_MIN = 5;
   private static final int WAKE_CORE_MAX = 40;
   private static final float WAKE_CORE_HALF = 0.042F;
   private static final float WAKE_SHELL_PER_BLOCK = 3.2F;
   private static final int WAKE_SHELL_MIN = 3;
   private static final int WAKE_SHELL_MAX = 16;
   private static final float WAKE_SHELL_HALF_MIN = 0.065F;
   private static final float WAKE_SHELL_HALF_SPAN = 0.06F;
   private static final float WAKE_SHELL_R_MIN = 0.05F;
   private static final float WAKE_SHELL_R_SPAN = 0.11F;
   private static final float WAKE_SPREAD = 1.6F;
   private static final float WAKE_DRIFT = 0.12F;
   private static final int WAKE_HEAD_CUBES = 3;
   private static final float WAKE_HEAD_HALF = 0.075F;
   private static final int WAKE_DISCS = 3;
   private static final float WAKE_DISC_R = 0.24F;
   private static final float WAKE_DISC_FRAC = 0.4F;
   private static final Lab.Value RIPPLE_SEGS = Lab.CLIENT.def("kinetic.ripple.segs", 48.0, 6.0, 128.0, 1.0);
   private static final Lab.Value RIPPLE_TUBE_SEGS = Lab.CLIENT.def("kinetic.ripple.tube_segs", 10.0, 3.0, 24.0, 1.0);
   private static final Lab.Value RIPPLE_SHELLS = Lab.CLIENT.def("kinetic.ripple.shells", 4.0, 1.0, 8.0, 1.0);
   private static final Lab.Value RIPPLE_BODY_ALPHA = Lab.CLIENT.def("kinetic.ripple.body_alpha", 60.0, 0.0, 255.0, 1.0);
   private static final Lab.Value RIPPLE_GLOW_ALPHA = Lab.CLIENT.def("kinetic.ripple.glow_alpha", 95.0, 0.0, 255.0, 1.0);
   private static final float DERIVED_GLOW = 1.0F;
   private static final Lab.Value RIPPLE_POP_FROM = Lab.CLIENT.def("kinetic.ripple.pop_from", 0.55, 0.0, 1.0, 0.01);
   private static final Lab.Value RIPPLE_FADE_SWELL = Lab.CLIENT.def("kinetic.ripple.fade_swell", 1.1, 1.0, 3.0, 0.01);
   private static final Lab.Value RIPPLE_WOBBLE = Lab.CLIENT.def("kinetic.ripple.wobble", 0.035, 0.0, 0.5, 0.005);
   private static final Lab.Value RIPPLE_WOBBLE_DRIFT = Lab.CLIENT.def("kinetic.ripple.wobble_drift", 0.12, 0.0, 2.0, 0.01);
   private static final Lab.Value RIPPLE_WISP = Lab.CLIENT.def("kinetic.ripple.wisp", 0.35, 0.0, 1.0, 0.01);
   private static final Lab.Value RIPPLE_WISP_DRIFT = Lab.CLIENT.def("kinetic.ripple.wisp_drift", 0.3, 0.0, 2.0, 0.01);
   private static float[] rippleCos = new float[0];
   private static float[] rippleSin = new float[0];
   private static float[] tubeCos = new float[0];
   private static float[] tubeSin = new float[0];
   private static float[] shellScale = new float[0];
   private static float[] shellAlpha = new float[0];
   private static final float RUPTURE_ROW_STEP = 0.9F;
   private static final float RUPTURE_COL_STEP = 1.0F;
   private static final float RUPTURE_SCATTER = 0.26F;
   private static final float RUPTURE_CROSS_SHARE = 0.55F;
   private static final float RUPTURE_CROSS_LAG = 0.35F;
   private static final float RUPTURE_CRACK_SPACING = 0.26F;
   private static final float RUPTURE_CORE_HALF = 0.035F;
   private static final float RUPTURE_CORE_ALPHA = 200.0F;
   private static final float RUPTURE_HALO_HALF = 0.26F;
   private static final float RUPTURE_HALO_ALPHA = 60.0F;
   private static final float RUPTURE_LIGHT_FRESH = 1.7F;
   private static final float RUPTURE_LIGHT_PULSE = 0.25F;
   private static final float RUPTURE_LIGHT_PULSE_RATE = 0.28F;
   private static final float RUPTURE_LIGHT_LIFT = 0.025F;
   private static final float RUPTURE_CORE_WHITE = 0.55F;
   private static final float RUPTURE_EMBER_SHARE = 0.22F;
   private static final float RUPTURE_EMBER_STAGGER = 6.0F;
   private static final float RUPTURE_EMBER_RISE = 0.035F;
   private static final float RUPTURE_EMBER_HALF = 0.05F;
   private static final int RUPTURE_LANE_EMBER = 8000;
   private static final float RUPTURE_CRACK_HALF = 0.07F;
   private static final float RUPTURE_LIFT = 0.04F;
   private static final int RUPTURE_SEAM_RGB = 1312810;
   private static final float RUPTURE_LIT_SHARE = 0.35F;
   private static final float RUPTURE_FRESH = 1.2F;
   private static final float RUPTURE_FRESH_FAT = 1.35F;
   private static final int RUPTURE_FRONT_BASE = 3;
   private static final float RUPTURE_FRONT_PER_BLOCK = 3.5F;
   private static final int RUPTURE_FRONT_MAX = 16;
   private static final float RUPTURE_FRONT_HALF = 0.11F;
   private static final float RUPTURE_FRONT_SCATTER = 0.2F;
   private static final float RUPTURE_FRONT_DISC_R = 0.6F;
   private static final float RUPTURE_FRONT_DISC_FRAC = 0.4F;
   private static final float RUPTURE_FRONT_DISC_EVERY = 1.1F;
   private static final float RUPTURE_FLUNG_SECOND = 0.5F;
   private static final float RUPTURE_FLUNG_UP_MIN = 0.14F;
   private static final float RUPTURE_FLUNG_UP_SPAN = 0.14F;
   private static final float RUPTURE_FLUNG_GRAVITY = 0.018F;
   private static final float RUPTURE_FLUNG_DRIFT = 0.035F;
   private static final int RUPTURE_FLUNG_LIFE = 10;
   private static final float RUPTURE_FLUNG_HALF_MIN = 0.06F;
   private static final float RUPTURE_FLUNG_HALF_SPAN = 0.05F;
   private static final int RUPTURE_DISC_EVERY = 2;
   private static final float RUPTURE_DISC_R = 0.6F;
   private static final float RUPTURE_DISC_ALPHA = 55.0F;
   private static final float RUPTURE_DISC_LIFT = 0.03F;
   private static final int RUPTURE_FAN_SEGS = 10;
   private static final int RUPTURE_LANE_FRONT = 6000;
   private static final int RUPTURE_LANE_FLUNG = 7000;
   private static final float WEDGE_SAMPLE = 0.75F;
   private static final float WEDGE_CORE_HALF = 0.03F;
   private static final float WEDGE_CORE_ALPHA = 150.0F;
   private static final float WEDGE_HALO_HALF = 0.16F;
   private static final float WEDGE_HALO_ALPHA = 45.0F;
   private static final float WEDGE_LIFT = 0.03F;
   private static final float WEDGE_SPECK_EVERY = 1.4F;
   private static final float WEDGE_SPECK_SHARE = 0.55F;
   private static final float WEDGE_SPECK_HALF = 0.04F;
   private static final float WEDGE_SPECK_ALPHA = 120.0F;
   private static final float WEDGE_CORE_WHITE = 0.45F;
   private static final int WEDGE_SEATS_MAX = 160;

   private static void rippleTables(int segs, int tubeSegs, int shells) {
      if (rippleCos.length != segs) {
         rippleCos = new float[segs];
         rippleSin = new float[segs];
         unitCircle(rippleCos, rippleSin);
      }

      if (tubeCos.length != tubeSegs) {
         tubeCos = new float[tubeSegs];
         tubeSin = new float[tubeSegs];
         unitCircle(tubeCos, tubeSin);
      }

      if (shellScale.length != shells) {
         shellScale = new float[shells];
         shellAlpha = new float[shells];

         for (int k = 0; k < shells; k++) {
            float in = (float)k / (float)shells;
            float out = (float)(k + 1) / (float)shells;
            shellScale[k] = out;
            shellAlpha[k] = 0.5F * (bell(in) - bell(out));
         }
      }
   }

   private static float bell(float u) {
      float b = 1.0F - u * u;
      return b * b;
   }

   private static void unitCircle(float[] cos, float[] sin) {
      for (int i = 0; i < cos.length; i++) {
         float phi = (float)i * 6.2832F / (float)cos.length;
         cos[i] = Mth.cos(phi);
         sin[i] = Mth.sin(phi);
      }
   }

   private KineticMotionComposites() {
   }

   public static void register() {
      VfxComposites.register(VfxEntity.Kind.KINETIC_WAKE, KineticMotionComposites::wake);
      VfxComposites.register(VfxEntity.Kind.KINETIC_AIR_TORUS, KineticMotionComposites::airRipple);
      VfxComposites.register(VfxEntity.Kind.KINETIC_RUPTURE, KineticMotionComposites::rupture);
      VfxComposites.register(VfxEntity.Kind.KINETIC_WEDGE, KineticMotionComposites::wedge);
   }

   private static void wake(PoseStack ps, VfxDraw.Sink sink, VfxRenderState state, float life) {
      float lifeC = Mth.clamp(life, 0.0F, 1.0F);
      float age = state.ageInTicks - (float)state.delay;
      Vector3f back = new Vector3f((float)state.secondary.x, (float)state.secondary.y, (float)state.secondary.z);
      float len = back.length();
      if (!(len < 0.05F)) {
         float envRaw = (float)Math.pow((double)(1.0F - lifeC), 0.75);
         float env = (float)Math.round(envRaw * 4.0F) / 4.0F;
         int aCube = VfxDraw.ia(150.0F * env, state.intensity);
         int aDisc = VfxDraw.ia(60.0F * (1.0F - lifeC) * (1.0F - lifeC), state.intensity);
         if (aCube > 1 || aDisc > 1) {
            float w = Math.max(0.2F, state.scale);
            int seed = state.seed;
            Vector3f k = new Vector3f(back).div(len);
            Vector3f t1 = VelocityStreaks.perpendicular(k);
            Vector3f t2 = new Vector3f(k).cross(t1).normalize();
            Quaternionf q = VfxCam.orientation();
            Vector3f right = q.transform(new Vector3f(1.0F, 0.0F, 0.0F));
            Vector3f up = q.transform(new Vector3f(0.0F, 1.0F, 0.0F));
            VoxelFlameDraw.Palette ladders = KineticComposites.ladders(state.color, state.color2);
            int[] tiers = ladders.base();
            int coreCubes = Mth.clamp(Math.round(len * 7.0F), 5, 40);
            int shellCubes = Mth.clamp(Math.round(len * 3.2F * w), 3, 16);
            float drift = len * 0.12F * lifeC;
            float shrink = 1.0F - 0.35F * lifeC;
            VfxDraw.flame(
               ps,
               sink,
               (pose, vc) -> {
                  for (int c = 0; c < coreCubes; c++) {
                     float u = ((float)c + 0.5F) / (float)coreCubes;
                     float d = u * len + drift;
                     float jit = (VoxelFlameDraw.noise01(seed, c, 1) - 0.5F) * 0.05F * w;
                     float jit2 = (VoxelFlameDraw.noise01(seed, c, 2) - 0.5F) * 0.05F * w;
                     int tier = u < 0.35F ? 3 : 2;
                     int a = (int)((float)aCube * (1.0F - 0.25F * u));
                     if (a > 1) {
                        float half = 0.042F * w * (1.15F - 0.4F * u) * shrink;
                        float ang = VoxelFlameDraw.noise01(seed, c, 3) * 6.2832F + age * 0.25F;
                        VoxelFlameDraw.cube(
                           pose,
                           vc,
                           k.x * d + t1.x * jit + t2.x * jit2,
                           k.y * d + t1.y * jit + t2.y * jit2,
                           k.z * d + t1.z * jit + t2.z * jit2,
                           half,
                           c % 3,
                           ang,
                           a,
                           ladders.pick(seed + c)[tier]
                        );
                     }
                  }

                  for (int cx = 0; cx < shellCubes; cx++) {
                     int lane = 100 + cx;
                     float u = VoxelFlameDraw.noise01(seed, lane, 1);
                     float d = u * len + drift;
                     float r = (0.05F + 0.11F * VoxelFlameDraw.noise01(seed, lane, 2)) * w * (1.0F + 1.6F * lifeC);
                     float phi = VoxelFlameDraw.noise01(seed, lane, 3) * 6.2832F + age * 0.08F;
                     float ca = Mth.cos(phi) * r;
                     float sa = Mth.sin(phi) * r;
                     int tier = u < 0.3F ? 2 : (u < 0.7F ? 1 : 0);
                     int a = (int)((float)aCube * 0.85F * (1.0F - 0.3F * u));
                     if (a > 1) {
                        float half = (0.065F + 0.06F * VoxelFlameDraw.noise01(seed, lane, 4)) * w * shrink;
                        int axis = (int)(VoxelFlameDraw.noise01(seed, lane, 5) * 3.0F) % 3;
                        float spin = (0.15F + 0.2F * VoxelFlameDraw.noise01(seed, lane, 6)) * (VoxelFlameDraw.noise01(seed, lane, 7) > 0.5F ? 1.0F : -1.0F);
                        float ang = VoxelFlameDraw.noise01(seed, lane, 8) * 6.2832F + age * spin;
                        VoxelFlameDraw.cube(
                           pose,
                           vc,
                           k.x * d + t1.x * ca + t2.x * sa,
                           k.y * d + t1.y * ca + t2.y * sa,
                           k.z * d + t1.z * ca + t2.z * sa,
                           half,
                           axis,
                           ang,
                           a,
                           ladders.pick(seed + lane)[tier]
                        );
                     }
                  }

                  for (int cxx = 0; cxx < 3; cxx++) {
                     int lane = 200 + cxx;
                     float ox = (VoxelFlameDraw.noise01(seed, lane, 1) - 0.5F) * 0.12F * w;
                     float oy = (VoxelFlameDraw.noise01(seed, lane, 2) - 0.5F) * 0.12F * w;
                     float oz = (VoxelFlameDraw.noise01(seed, lane, 3) - 0.5F) * 0.12F * w;
                     float ang = VoxelFlameDraw.noise01(seed, lane, 4) * 6.2832F + age * 0.35F;
                     VoxelFlameDraw.cube(
                        pose, vc, k.x * drift + ox, k.y * drift + oy, k.z * drift + oz, 0.075F * w * shrink, cxx % 3, ang, aCube, ladders.pick(seed + lane)[3]
                     );
                  }

                  if (aDisc > 1) {
                     for (int m = 0; m < 3; m++) {
                        float u = ((float)m + 0.5F) / 3.0F;
                        float d = u * len + drift;
                        int rgb = VoxelFlameDraw.gradient(tiers, 1.0F - u);
                        VoxelFlameDraw.disc(
                           pose, vc, k.x * d, k.y * d, k.z * d, right, up, 0.24F * w * (1.1F - 0.4F * u), (int)((float)aDisc * (1.0F - 0.4F * u)), rgb
                        );
                     }
                  }
               }
            );
         }
      }
   }

   private static void airRipple(PoseStack ps, VfxDraw.Sink sink, VfxRenderState state, float life) {
      float age = state.ageInTicks - (float)state.delay;
      if (!(age < 0.0F)) {
         float popTicks = (float)VfxEntity.airTorusPop(state.secondary.y);
         float holdTicks;
         float fadeTicks;
         if (popTicks <= 0.0F) {
            popTicks = 1.0F;
            holdTicks = 6.0F;
            fadeTicks = 6.0F;
         } else {
            holdTicks = (float)VfxEntity.airTorusHold(state.secondary.y);
            fadeTicks = Math.max(1.0F, (float)state.lifetime - holdTicks);
         }

         int segs = Mth.clamp(RIPPLE_SEGS.getI(), 6, 128);
         int tubeSegs = Mth.clamp(RIPPLE_TUBE_SEGS.getI(), 3, 24);
         int shells = Mth.clamp(RIPPLE_SHELLS.getI(), 1, 8);
         rippleTables(segs, tubeSegs, shells);
         float popT = Mth.clamp(age / popTicks, 0.0F, 1.0F);
         float pop = 1.0F - (1.0F - popT) * (1.0F - popT);
         float past = age - holdTicks;
         float fade = past <= 0.0F ? 1.0F : Math.max(0.0F, 1.0F - past / fadeTicks);
         int aBody = VfxDraw.ia(RIPPLE_BODY_ALPHA.getF() * fade, state.intensity);
         int aGlow = VfxDraw.ia(RIPPLE_GLOW_ALPHA.getF() * fade, state.intensity);
         if (aBody > 1 || aGlow > 1) {
            float size = Mth.lerp(pop, RIPPLE_POP_FROM.getF(), 1.0F) * Mth.lerp(1.0F - fade, 1.0F, RIPPLE_FADE_SWELL.getF());
            float radius = Math.max(0.05F, state.scale) * size;
            float halfBand = Math.max(0.01F, (float)state.secondary.x * 0.5F) * size;
            float depth = state.secondary.z > 0.0 ? Math.max(0.01F, (float)state.secondary.z) * size : halfBand;
            int bodyRgb = state.color;
            int glowRgb = VfxColor.core(state.color, state.color2, 1.0F);
            float[] radii = rippleRadii(state.seed, age, radius, segs);
            float[] breath = rippleBreath(state.seed, age, segs);
            VfxDraw.bodyThenGlow(
               ps,
               sink,
               (pose, vc) -> tube(pose, vc, radii, breath, halfBand, depth, aBody, bodyRgb, false, segs, tubeSegs, shells),
               (pose, vc) -> tube(pose, vc, radii, breath, halfBand, depth, aGlow, glowRgb, true, segs, tubeSegs, shells)
            );
         }
      }
   }

   private static float[] rippleRadii(int seed, float age, float radius, int segs) {
      float[] out = new float[segs];
      float drift = RIPPLE_WOBBLE_DRIFT.getF();
      float wobble = RIPPLE_WOBBLE.getF();
      float p2 = VoxelFlameDraw.noise01(seed, 1, 7) * 6.2832F + age * drift;
      float p3 = VoxelFlameDraw.noise01(seed, 2, 7) * 6.2832F - age * drift;
      float p5 = VoxelFlameDraw.noise01(seed, 3, 7) * 6.2832F + age * drift * 1.5F;

      for (int i = 0; i < segs; i++) {
         float phi = (float)i * 6.2832F / (float)segs;
         float wob = 0.5F * Mth.sin(2.0F * phi + p2) + 0.3F * Mth.sin(3.0F * phi + p3) + 0.2F * Mth.sin(5.0F * phi + p5);
         out[i] = radius * (1.0F + wobble * wob);
      }

      return out;
   }

   private static float[] rippleBreath(int seed, float age, int segs) {
      float[] out = new float[segs];
      float wisp = RIPPLE_WISP.getF();
      float p7 = VoxelFlameDraw.noise01(seed, 4, 7) * 6.2832F + age * RIPPLE_WISP_DRIFT.getF();

      for (int i = 0; i < segs; i++) {
         float phi = (float)i * 6.2832F / (float)segs;
         out[i] = 1.0F - wisp * (0.5F + 0.5F * Mth.sin(7.0F * phi + p7));
      }

      return out;
   }

   private static void tube(
      Pose pose,
      VertexConsumer vc,
      float[] radii,
      float[] breath,
      float halfBand,
      float depth,
      int peak,
      int rgb,
      boolean twoSided,
      int segs,
      int tubeSegs,
      int shells
   ) {
      Vector3f a = new Vector3f();
      Vector3f b = new Vector3f();
      Vector3f c = new Vector3f();
      Vector3f d = new Vector3f();

      for (int k = 0; k < shells; k++) {
         float across = halfBand * shellScale[k];
         float along = depth * shellScale[k];
         float alpha = (float)peak * shellAlpha[k];

         for (int i = 0; i < segs; i++) {
            int i1 = (i + 1) % segs;
            int ca = VfxColor.argb((int)(alpha * breath[i]), rgb);
            int cb = VfxColor.argb((int)(alpha * breath[i1]), rgb);

            for (int j = 0; j < tubeSegs; j++) {
               int j1 = (j + 1) % tubeSegs;
               tubePoint(a, i, j, radii[i], across, along);
               tubePoint(b, i1, j, radii[i1], across, along);
               tubePoint(c, i1, j1, radii[i1], across, along);
               tubePoint(d, i, j1, radii[i], across, along);
               VelocityStreaks.quad(pose, vc, a, ca, b, cb, c, cb, d, ca, twoSided);
            }
         }
      }
   }

   private static void tubePoint(Vector3f out, int i, int j, float r, float across, float along) {
      float rr = Math.max(0.0F, r + across * tubeCos[j]);
      out.set(rippleCos[i] * rr, along * tubeSin[j], rippleSin[i] * rr);
   }

   private static void rupture(PoseStack ps, VfxDraw.Sink sink, VfxRenderState state, float life) {
      float age = state.ageInTicks - (float)state.delay;
      if (!(age < 0.0F)) {
         float ax = (float)state.secondary.x;
         float az = (float)state.secondary.z;
         float length = (float)Math.sqrt((double)(ax * ax + az * az));
         if (!(length < 0.2F)) {
            float kx = ax / length;
            float kz = az / length;
            float halfNear = Math.max(0.05F, VfxEntity.ruptureHalfNear(state.secondary.y));
            float glow = VfxEntity.ruptureGlow(state.secondary.y);
            float halfFar = Math.max(halfNear, Math.abs(state.scale));
            int travel = Math.max(1, state.lifetime - 14);
            float speed = length / (float)travel;
            float front = Math.min(length, age * speed);
            boolean running = age < (float)travel;
            float past = age - (float)travel;
            float fadeRaw = past <= 0.0F ? 1.0F : Math.max(0.0F, 1.0F - past / 14.0F);
            float fade = (float)Math.round(fadeRaw * 4.0F) / 4.0F;
            int aCube = VfxDraw.ia(150.0F * fade, state.intensity);
            if (aCube > 1) {
               ClientLevel level = Minecraft.getInstance().level;
               KineticMotionComposites.RuptureShape shape = shapeOf(level, state.seed, kx, kz, length, halfNear, halfFar);
               double wx = state.worldPos.x;
               double wy = state.worldPos.y;
               double wz = state.worldPos.z;
               int points = shape.jx.length;
               float[] jy = new float[points];

               for (int i = 0; i < points; i++) {
                  float seat = level == null ? 0.0F : shape.seats.seat(level, i, wx + (double)shape.jx[i], wy, wz + (double)shape.jz[i]) - (float)wy;
                  jy[i] = seat + 0.04F;
               }

               float frontHalf = shape.halfAt(front);
               int frontCubes = running ? Math.min(16, 3 + Math.round(frontHalf * 3.5F)) : 0;
               float[] fx = new float[frontCubes];
               float[] fy = new float[frontCubes];
               float[] fz = new float[frontCubes];

               for (int c = 0; c < frontCubes; c++) {
                  int l = 6000 + c;
                  float u = (-1.0F + 2.0F * ((float)c + 0.5F) / (float)frontCubes) * frontHalf * 0.9F
                     + (VoxelFlameDraw.noise01(state.seed, l, 1) - 0.5F) * 2.0F * 0.2F;
                  float along = (VoxelFlameDraw.noise01(state.seed, l, 2) - 0.5F) * 2.0F * 0.2F;
                  fx[c] = kx * (front + along) + shape.sx * u;
                  fz[c] = kz * (front + along) + shape.sz * u;
                  float seat = level == null ? 0.0F : shape.seats.seat(level, points + c, wx + (double)fx[c], wy, wz + (double)fz[c]) - (float)wy;
                  fy[c] = seat + 0.04F;
               }

               VoxelFlameDraw.Palette ladders = KineticComposites.ladders(state.color, state.color2);
               int seed = state.seed;
               VfxDraw.flame(
                  ps,
                  sink,
                  (pose, vc) -> {
                     int lane = 0;

                     for (int s = 0; s < shape.segA.length; s++) {
                        lane = crackSegment(
                           pose, vc, shape, shape.segA[s], shape.segB[s], jy, front, shape.segCross[s] ? 0.35F : 0.0F, aCube, ladders, seed, age, lane
                        );
                     }

                     for (int c = 0; c < frontCubes; c++) {
                        int l = 6000 + c;
                        float ang = VoxelFlameDraw.noise01(seed, l, 4) * 6.2832F + age * 0.45F;
                        VoxelFlameDraw.cube(
                           pose,
                           vc,
                           fx[c],
                           fy[c] + 0.11F + VoxelFlameDraw.noise01(seed, l, 3) * 0.15F,
                           fz[c],
                           0.11F,
                           c % 3,
                           ang,
                           aCube,
                           ladders.pick(seed + l)[3]
                        );
                     }

                     if (past > 0.0F) {
                        for (int i = 1; i < points; i++) {
                           if (!(VoxelFlameDraw.noise01(seed, i, 51) >= 0.22F)) {
                              int l = 8000 + i;
                              float a = past - VoxelFlameDraw.noise01(seed, l, 1) * 6.0F;
                              if (!(a < 0.0F)) {
                                 int alpha = (int)((float)aCube * 0.7F);
                                 if (alpha > 1) {
                                    float ang = VoxelFlameDraw.noise01(seed, l, 2) * 6.2832F + a * 0.2F;
                                    float ox = (VoxelFlameDraw.noise01(seed, l, 3) - 0.5F) * 0.4F;
                                    float oz = (VoxelFlameDraw.noise01(seed, l, 4) - 0.5F) * 0.4F;
                                    VoxelFlameDraw.cube(
                                       pose,
                                       vc,
                                       shape.jx[i] + ox,
                                       jy[i] + 0.05F + a * 0.035F,
                                       shape.jz[i] + oz,
                                       0.05F,
                                       i % 3,
                                       ang,
                                       alpha,
                                       ladders.pick(seed + l)[2]
                                    );
                                 }
                              }
                           }
                        }
                     }

                     for (int ix = 1; ix < points; ix++) {
                        float t0 = shape.jd[ix] / speed;
                        float a = age - t0;
                        if (!(a < 0.0F) && !(a >= 10.0F)) {
                           float envRaw = 1.0F - a / 10.0F * (a / 10.0F);
                           int alpha = (int)((float)aCube * ((float)Math.round(envRaw * 4.0F) / 4.0F));
                           if (alpha > 1) {
                              int tier = a < 3.0F ? 3 : (a < 6.0F ? 2 : 1);
                              int count = VoxelFlameDraw.noise01(seed, ix, 41) < 0.5F ? 2 : 1;

                              for (int c = 0; c < count; c++) {
                                 int l = 7000 + ix * 4 + c;
                                 float upSpeed = 0.14F + 0.14F * VoxelFlameDraw.noise01(seed, l, 1);
                                 float y = jy[ix] + upSpeed * a - 0.009F * a * a;
                                 if (!(y < jy[ix])) {
                                    float ox = (VoxelFlameDraw.noise01(seed, l, 2) - 0.5F) * 1.0F * 0.6F;
                                    float oz = (VoxelFlameDraw.noise01(seed, l, 8) - 0.5F) * 1.0F * 0.6F;
                                    float dx = (VoxelFlameDraw.noise01(seed, l, 3) - 0.5F) * 2.0F * 0.035F * a;
                                    float dz = (VoxelFlameDraw.noise01(seed, l, 4) - 0.5F) * 2.0F * 0.035F * a;
                                    float h = 0.06F + 0.05F * VoxelFlameDraw.noise01(seed, l, 5);
                                    int axis = (int)(VoxelFlameDraw.noise01(seed, l, 6) * 3.0F) % 3;
                                    float ang = VoxelFlameDraw.noise01(seed, l, 7) * 6.2832F + a * 0.4F;
                                    VoxelFlameDraw.cube(
                                       pose, vc, shape.jx[ix] + ox + dx, y, shape.jz[ix] + oz + dz, h, axis, ang, alpha, ladders.pick(seed + l)[tier]
                                    );
                                 }
                              }
                           }
                        }
                     }
                  }
               );
               float light = glow * fade * state.intensity;
               if (!(light <= 0.01F)) {
                  float pulse = past > 0.0F ? 1.0F - 0.25F * (0.5F + 0.5F * Mth.sin(age * 0.28F)) : 1.0F;
                  float strength = light * pulse;
                  int heart = state.color2 == -1 ? VfxColor.lighten(state.color, 0.35F) : state.color2;
                  int core = VfxColor.lighten(heart, 0.55F);
                  VfxDraw.additive(ps, sink, (pose, vc) -> {
                     for (int s = 0; s < shape.segA.length; s++) {
                        crackLight(pose, vc, shape, shape.segA[s], shape.segB[s], jy, front, shape.segCross[s] ? 0.35F : 0.0F, strength, core, heart, s);
                     }

                     for (int i = 0; i < points; i += 2) {
                        float d = shape.jd[i];
                        if (!(d > front)) {
                           float fresh = front - d < 1.2F ? 1.7F : 1.0F;
                           groundFan(pose, vc, shape.jx[i], jy[i] + 0.03F, shape.jz[i], 0.6F, 55.0F * strength * fresh, heart);
                        }
                     }

                     if (frontCubes > 0) {
                        int fans = Math.max(1, Math.round(2.0F * frontHalf / 1.1F));

                        for (int m = 0; m < fans; m++) {
                           float u = fans == 1 ? 0.0F : (-1.0F + 2.0F * ((float)m + 0.5F) / (float)fans) * frontHalf * 0.85F;
                           int c = Math.min(frontCubes - 1, Math.round(((float)m + 0.5F) / (float)fans * (float)frontCubes));
                           groundFan(pose, vc, kx * front + shape.sx * u, fy[c] + 0.03F, kz * front + shape.sz * u, 0.6F, 88.0F * strength, heart);
                        }
                     }
                  });
               }
            }
         }
      }
   }

   private static void crackLight(
      Pose pose,
      VertexConsumer vc,
      KineticMotionComposites.RuptureShape shape,
      int a,
      int b,
      float[] jy,
      float front,
      float lag,
      float strength,
      int core,
      int heart,
      int seg
   ) {
      float dA = shape.jd[a] + lag;
      float dB = shape.jd[b] + lag;
      if (!(Math.min(dA, dB) > front)) {
         float t1 = 1.0F;
         if (Math.max(dA, dB) > front && Math.abs(dB - dA) > 1.0E-4F) {
            t1 = Mth.clamp((front - dA) / (dB - dA), 0.0F, 1.0F);
            if (t1 <= 0.01F) {
               return;
            }
         }

         float fresh = front - Math.min(dA, dB) < 1.2F ? 1.7F : 1.0F;
         float ax = shape.jx[a];
         float az = shape.jz[a];
         float bx = Mth.lerp(t1, ax, shape.jx[b]);
         float bz = Mth.lerp(t1, az, shape.jz[b]);
         float ay = jy[a] + 0.025F;
         float by = Mth.lerp(t1, jy[a], jy[b]) + 0.025F;
         float dx = bx - ax;
         float dz = bz - az;
         float len = (float)Math.sqrt((double)(dx * dx + dz * dz));
         if (!(len < 0.001F)) {
            float px = -dz / len;
            float pz = dx / len;
            int coreA = VfxColor.argb(Math.min(255, Math.round(200.0F * strength * fresh)), core);
            int haloA = VfxColor.argb(Math.min(255, Math.round(60.0F * strength * fresh)), heart);
            int haloRim = VfxColor.argb(0, heart);
            strip(pose, vc, ax, ay, az, bx, by, bz, px, pz, 0.26F, haloA, haloRim);
            strip(pose, vc, ax, ay + 0.005F, az, bx, by + 0.005F, bz, px, pz, 0.035F, coreA, coreA);
         }
      }
   }

   private static void strip(
      Pose pose, VertexConsumer vc, float ax, float ay, float az, float bx, float by, float bz, float px, float pz, float half, int centre, int edge
   ) {
      Vector3f a0 = new Vector3f(ax, ay, az);
      Vector3f b0 = new Vector3f(bx, by, bz);
      Vector3f aL = new Vector3f(ax - px * half, ay, az - pz * half);
      Vector3f bL = new Vector3f(bx - px * half, by, bz - pz * half);
      Vector3f aR = new Vector3f(ax + px * half, ay, az + pz * half);
      Vector3f bR = new Vector3f(bx + px * half, by, bz + pz * half);
      VelocityStreaks.quad(pose, vc, aL, edge, bL, edge, b0, centre, a0, centre, true);
      VelocityStreaks.quad(pose, vc, a0, centre, b0, centre, bR, edge, aR, edge, true);
   }

   private static void wedge(PoseStack ps, VfxDraw.Sink sink, VfxRenderState state, float life) {
      float age = state.ageInTicks - (float)state.delay;
      if (!(age < 0.0F)) {
         float ax = (float)state.secondary.x;
         float az = (float)state.secondary.z;
         float length = (float)Math.sqrt((double)(ax * ax + az * az));
         if (!(length < 0.2F)) {
            float kx = ax / length;
            float kz = az / length;
            float sz = -kx;
            float halfNear = Math.max(0.05F, VfxEntity.ruptureHalfNear(state.secondary.y));
            float bright = VfxEntity.ruptureGlow(state.secondary.y);
            float halfFar = Math.max(halfNear, Math.abs(state.scale));
            float halfLife = Math.max(1.0F, (float)state.lifetime) * 0.5F;
            float env = 1.0F - Math.abs(age - halfLife) / halfLife;
            float strength = Math.max(0.0F, env) * bright * state.intensity;
            if (!(strength <= 0.01F)) {
               ClientLevel level = Minecraft.getInstance().level;
               double wx = state.worldPos.x;
               double wy = state.worldPos.y;
               double wz = state.worldPos.z;
               KineticComposites.FloorSeats seats = KineticComposites.floorSeats(level, state.seed, 160);
               float[] nl = new float[]{-kz * halfNear, -sz * halfNear};
               float[] nr = new float[]{kz * halfNear, sz * halfNear};
               float[] fl = new float[]{kx * length - kz * halfFar, kz * length - sz * halfFar};
               float[] fr = new float[]{kx * length + kz * halfFar, kz * length + sz * halfFar};
               float[][] edges = new float[][]{nl, nr, nr, fr, fr, fl, fl, nl};
               List<float[]> lines = new ArrayList<>(4);
               int slot = 0;

               for (int e = 0; e < 4; e++) {
                  float[] a = edges[e * 2];
                  float[] b = edges[e * 2 + 1];
                  float dx = b[0] - a[0];
                  float dz = b[1] - a[1];
                  float len = (float)Math.sqrt((double)(dx * dx + dz * dz));
                  int n = Math.max(1, Mth.ceil(len / 0.75F));
                  float[] pts = new float[(n + 1) * 3];

                  for (int i = 0; i <= n; i++) {
                     float t = (float)i / (float)n;
                     float x = a[0] + dx * t;
                     float z = a[1] + dz * t;
                     float y = level == null ? 0.0F : seats.seat(level, slot++, wx + (double)x, wy, wz + (double)z) - (float)wy;
                     pts[i * 3] = x;
                     pts[i * 3 + 1] = y + 0.03F;
                     pts[i * 3 + 2] = z;
                  }

                  lines.add(pts);
               }

               int heart = state.color2 == -1 ? VfxColor.lighten(state.color, 0.35F) : state.color2;
               int core = VfxColor.lighten(heart, 0.45F);
               int coreA = VfxColor.argb(Math.min(255, Math.round(150.0F * strength)), core);
               int haloA = VfxColor.argb(Math.min(255, Math.round(45.0F * strength)), heart);
               int haloRim = VfxColor.argb(0, heart);
               VoxelFlameDraw.Palette ladders = KineticComposites.ladders(state.color, state.color2);
               int seed = state.seed;
               int aSpeck = Math.round(120.0F * strength);
               VfxDraw.additive(
                  ps,
                  sink,
                  (pose, vc) -> {
                     for (float[] pts : lines) {
                        for (int ix = 0; ix + 3 < pts.length; ix += 3) {
                           float dx = pts[ix + 3] - pts[ix];
                           float dz = pts[ix + 5] - pts[ix + 2];
                           float len = (float)Math.sqrt((double)(dx * dx + dz * dz));
                           if (!(len < 0.001F)) {
                              float px = -dz / len;
                              float pz = dx / len;
                              strip(pose, vc, pts[ix], pts[ix + 1], pts[ix + 2], pts[ix + 3], pts[ix + 4], pts[ix + 5], px, pz, 0.16F, haloA, haloRim);
                              strip(
                                 pose,
                                 vc,
                                 pts[ix],
                                 pts[ix + 1] + 0.005F,
                                 pts[ix + 2],
                                 pts[ix + 3],
                                 pts[ix + 4] + 0.005F,
                                 pts[ix + 5],
                                 px,
                                 pz,
                                 0.03F,
                                 coreA,
                                 coreA
                              );
                           }
                        }
                     }
                  }
               );
               if (aSpeck > 2) {
                  VfxDraw.flame(
                     ps,
                     sink,
                     (pose, vc) -> {
                        for (int e = 1; e <= 2; e++) {
                           float[] pts = lines.get(e);
                           int segs = pts.length / 3 - 1;
                           float edgeLen = (float)segs * 0.75F;
                           int slots = Math.max(1, Mth.floor(edgeLen / 1.4F));

                           for (int s = 0; s < slots; s++) {
                              int lane = e * 100 + s;
                              if (!(VoxelFlameDraw.noise01(seed, lane, 61) > 0.55F)) {
                                 float tx = ((float)s + VoxelFlameDraw.noise01(seed, lane, 62)) / (float)slots;
                                 float f = tx * (float)segs;
                                 int ix = Math.min(segs - 1, (int)f);
                                 float u = f - (float)ix;
                                 float xx = Mth.lerp(u, pts[ix * 3], pts[ix * 3 + 3]);
                                 float yx = Mth.lerp(u, pts[ix * 3 + 1], pts[ix * 3 + 4]) + 0.04F + VoxelFlameDraw.noise01(seed, lane, 63) * 0.12F;
                                 float zx = Mth.lerp(u, pts[ix * 3 + 2], pts[ix * 3 + 5]);
                                 float ang = VoxelFlameDraw.noise01(seed, lane, 64) * 6.2832F + age * 0.3F;
                                 VoxelFlameDraw.cube(
                                    pose,
                                    vc,
                                    xx,
                                    yx,
                                    zx,
                                    0.04F,
                                    s % 3,
                                    ang,
                                    aSpeck,
                                    ladders.pick(seed + lane)[VoxelFlameDraw.noise01(seed, lane, 65) < 0.5F ? 2 : 3]
                                 );
                              }
                           }
                        }
                     }
                  );
               }
            }
         }
      }
   }

   private static void groundFan(Pose pose, VertexConsumer vc, float x, float y, float z, float r, float alpha, int rgb) {
      int a = Math.min(255, Math.round(alpha));
      if (a > 2) {
         int centre = VfxColor.argb(a, rgb);
         int rim = VfxColor.argb(0, rgb);

         for (int i = 0; i < 10; i++) {
            float a0 = (float)i * 6.2832F / 10.0F;
            float a1 = (float)(i + 1) * 6.2832F / 10.0F;
            VelocityStreaks.tri(
               pose, vc, x, y, z, centre, x + Mth.cos(a0) * r, y, z + Mth.sin(a0) * r, rim, x + Mth.cos(a1) * r, y, z + Mth.sin(a1) * r, rim, true
            );
         }
      }
   }

   private static int crackSegment(
      Pose pose,
      VertexConsumer vc,
      KineticMotionComposites.RuptureShape shape,
      int a,
      int b,
      float[] jy,
      float front,
      float lag,
      int aCube,
      VoxelFlameDraw.Palette ladders,
      int seed,
      float age,
      int lane
   ) {
      float dxs = shape.jx[b] - shape.jx[a];
      float dzs = shape.jz[b] - shape.jz[a];
      float segLen = (float)Math.sqrt((double)(dxs * dxs + dzs * dzs));
      int cubes = Math.max(1, Mth.ceil(segLen / 0.26F));
      int k = 0;

      while (k < cubes) {
         float t = ((float)k + 0.5F) / (float)cubes;
         float d = Mth.lerp(t, shape.jd[a], shape.jd[b]) + lag;
         if (!(d > front)) {
            boolean fresh = front - d < 1.2F;
            boolean lit = fresh || VoxelFlameDraw.noise01(seed, lane, 31) < 0.35F;
            int rgb;
            if (lit) {
               int tier = fresh ? 3 : (VoxelFlameDraw.noise01(seed, lane, 32) < 0.5F ? 1 : 2);
               rgb = ladders.pick(seed + lane)[tier];
            } else {
               rgb = 1312810;
            }

            float half = 0.07F * (0.75F + 0.5F * VoxelFlameDraw.noise01(seed, lane, 33)) * (fresh ? 1.35F : 1.0F);
            int alpha = lit && !fresh ? (int)((float)aCube * (0.8F + 0.2F * Mth.sin(age * 0.6F + (float)lane))) : aCube;
            int axis = (int)(VoxelFlameDraw.noise01(seed, lane, 34) * 3.0F) % 3;
            float ang = VoxelFlameDraw.noise01(seed, lane, 35) * 6.2832F + (lit ? age * 0.12F : 0.0F);
            VoxelFlameDraw.cube(
               pose,
               vc,
               Mth.lerp(t, shape.jx[a], shape.jx[b]),
               Mth.lerp(t, jy[a], jy[b]) + half,
               Mth.lerp(t, shape.jz[a], shape.jz[b]),
               half,
               axis,
               ang,
               alpha,
               rgb
            );
         }

         k++;
         lane++;
      }

      return lane;
   }

   private static KineticMotionComposites.RuptureShape shapeOf(ClientLevel level, int entityId, float kx, float kz, float length, float halfNear, float halfFar) {
      return level != null && level.getEntity(entityId) instanceof VfxEntity shell
         ? shell.drawerMemo(
            KineticMotionComposites.RuptureShape.class, () -> new KineticMotionComposites.RuptureShape(entityId, kx, kz, length, halfNear, halfFar)
         )
         : new KineticMotionComposites.RuptureShape(entityId, kx, kz, length, halfNear, halfFar);
   }

   private static final class RuptureShape {
      final float kx;
      final float kz;
      final float sx;
      final float sz;
      final float length;
      final float halfNear;
      final float halfFar;
      final float[] jx;
      final float[] jz;
      final float[] jd;
      final int[] segA;
      final int[] segB;
      final boolean[] segCross;
      final KineticComposites.FloorSeats seats;

      RuptureShape(int seed, float kx, float kz, float length, float halfNear, float halfFar) {
         this.kx = kx;
         this.kz = kz;
         this.sx = kz;
         this.sz = -kx;
         this.length = length;
         this.halfNear = halfNear;
         this.halfFar = halfFar;
         List<float[]> pts = new ArrayList<>();
         List<int[]> rows = new ArrayList<>();
         int rowCount = Math.max(2, Mth.ceil(length / 0.9F) + 1);

         for (int r = 0; r < rowCount; r++) {
            float d = Math.min(length, (float)r * 0.9F);
            float half = this.halfAt(d);
            int cols = r == 0 ? 1 : Math.max(1, Mth.ceil(2.0F * half / 1.0F));
            int[] row = new int[cols];

            for (int c = 0; c < cols; c++) {
               int id = r * 64 + c;
               float u = cols == 1 ? 0.0F : (-1.0F + 2.0F * ((float)c + 0.5F) / (float)cols) * half * 0.92F;
               float su = r == 0 ? 0.0F : (VoxelFlameDraw.noise01(seed, id, 21) - 0.5F) * 2.0F * 0.26F;
               float sd = r == 0 ? 0.0F : (VoxelFlameDraw.noise01(seed, id, 22) - 0.5F) * 2.0F * 0.26F;
               float dd = Mth.clamp(d + sd, 0.0F, length);
               row[c] = pts.size();
               pts.add(new float[]{kx * dd + this.sx * (u + su), kz * dd + this.sz * (u + su), dd, u + su});
            }

            rows.add(row);
         }

         List<int[]> segs = new ArrayList<>();

         for (int r = 0; r + 1 < rows.size(); r++) {
            int[] row = rows.get(r);
            int[] next = rows.get(r + 1);

            for (int c = 0; c < row.length; c++) {
               float u = pts.get(row[c])[3];
               int best = next[0];
               float bestGap = Float.MAX_VALUE;

               for (int n : next) {
                  float gap = Math.abs(pts.get(n)[3] - u);
                  if (gap < bestGap) {
                     bestGap = gap;
                     best = n;
                  }
               }

               segs.add(new int[]{row[c], best, 0});
               if (c + 1 < row.length && VoxelFlameDraw.noise01(seed, r * 64 + c, 23) < 0.55F) {
                  segs.add(new int[]{row[c], row[c + 1], 1});
               }
            }

            for (int nx : next) {
               boolean reached = false;

               for (int[] s : segs) {
                  if (s[1] == nx) {
                     reached = true;
                     break;
                  }
               }

               if (!reached) {
                  float u = pts.get(nx)[3];
                  int best = row[0];
                  float bestGap = Float.MAX_VALUE;

                  for (int p : row) {
                     float gap = Math.abs(pts.get(p)[3] - u);
                     if (gap < bestGap) {
                        bestGap = gap;
                        best = p;
                     }
                  }

                  segs.add(new int[]{best, nx, 0});
               }
            }
         }

         int nx = pts.size();
         this.jx = new float[nx];
         this.jz = new float[nx];
         this.jd = new float[nx];

         for (int i = 0; i < nx; i++) {
            float[] px = pts.get(i);
            this.jx[i] = px[0];
            this.jz[i] = px[1];
            this.jd[i] = px[2];
         }

         this.segA = new int[segs.size()];
         this.segB = new int[segs.size()];
         this.segCross = new boolean[segs.size()];

         for (int i = 0; i < this.segA.length; i++) {
            int[] sx = segs.get(i);
            this.segA[i] = sx[0];
            this.segB[i] = sx[1];
            this.segCross[i] = sx[2] == 1;
         }

         this.seats = new KineticComposites.FloorSeats(nx + 16);
      }

      float halfAt(float d) {
         return Mth.lerp(Mth.clamp(d / this.length, 0.0F, 1.0F), this.halfNear, this.halfFar);
      }
   }
}
