package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.pyrokinetic.FlameTone;
import com.awakenedmod.awakened.content.vfx.VfxEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class VoxelSpriteFireDrawer {
   private static final int ENV_STEPS = 4;
   private static final float CUBE_ALPHA = 135.0F;
   private static final float DISC_FRAC = 0.5F;
   private static final float SCROLL = 0.05F;
   private static final float POP_AT = 0.97F;
   private static final float WIGGLE = 0.5F;
   private static final float DISC_R = 0.85F;
   private static final float GRAVITY = 0.011F;
   private static final int BURST_DISCS = 5;
   private static final float BURST_CUBES_PER_AREA = 0.95F;
   private static final int BURST_CUBES_MIN = 8;
   private static final int BURST_CUBES_MAX = 64;
   private static final float BURST_HALF_CAP = 0.6F;
   private static final float BURST_LIFT_BASE = 1.5F;
   private static final float TRAVEL_CAP_MUL = 1.5F;
   private static final float INRUSH_STAGGER = 0.6F;
   private static final int INRUSH_CUBES_MIN = 6;
   private static final int INRUSH_CUBES_MAX = 16;
   private static final float INRUSH_SPREAD = 0.45F;
   private static final float INRUSH_SWIRL = 0.3F;
   private static final float INRUSH_FADE_FRAC = 0.12F;

   private VoxelSpriteFireDrawer() {
   }

   public static void register() {
      register(
         VfxEntity.Kind.FIRE_VOXEL_SPRITE,
         VfxEntity.Kind.FIRE_VOXEL_INRUSH,
         state -> VoxelFlameDraw.blend(FlameTone.blueFractionFromWire(state.color, state.color2))
      );
   }

   public static void register(VfxEntity.Kind sprite, VfxEntity.Kind inrush, VoxelSpriteFireDrawer.PaletteSource palette) {
      VfxComposites.register(sprite, (ps, sink, state, life) -> draw(ps, sink, state, life, palette));
      VfxComposites.register(inrush, (ps, sink, state, life) -> drawInrush(ps, sink, state, life, palette));
   }

   private static void draw(PoseStack ps, VfxDraw.Sink sink, VfxRenderState state, float life, VoxelSpriteFireDrawer.PaletteSource palette) {
      float lifeC = Mth.clamp(life, 0.0F, 1.0F);
      float age = state.ageInTicks - (float)state.delay;
      boolean burst = state.scale < 0.0F || state.secondary.lengthSqr() < 1.0E-6;
      float cubeEnvRaw = burst ? Math.min(1.0F, lifeC * 5.0F) * (1.0F - lifeC) : (float)Math.sin(Math.PI * (double)lifeC);
      float cubeEnv = (float)Math.round(cubeEnvRaw * 4.0F) / 4.0F;
      float discEnv = burst
         ? Math.min(1.0F, lifeC * 4.0F) * (float)Math.pow((double)(1.0F - lifeC), 1.15)
         : (float)Math.pow(Math.sin(Math.PI * (double)lifeC), 0.75);
      float breathe = VoxelFlameDraw.breathe(age, state.seed & 0xFF);
      int aCube = VfxDraw.ia(135.0F * cubeEnv, state.intensity);
      int aDisc = VfxDraw.ia(Math.min(255.0F, 67.5F * discEnv * breathe), state.intensity);
      if (aCube > 1 || aDisc > 1) {
         Quaternionf q = VfxCam.orientation();
         Vector3f right = q.transform(new Vector3f(1.0F, 0.0F, 0.0F));
         Vector3f up = q.transform(new Vector3f(0.0F, 1.0F, 0.0F));
         VoxelFlameDraw.Palette ladders = palette.palette(state);
         int[] tiers = ladders.base();
         int seed = state.seed;
         float s = Math.abs(state.scale);
         int lifetime = state.lifetime;
         float kx = (float)state.secondary.x;
         float ky = (float)state.secondary.y;
         float kz = (float)state.secondary.z;
         int cubes = burst ? Mth.clamp(Math.max(6 + Math.round(s * 4.0F), Math.round(s * s * 0.95F)), 8, 64) : Mth.clamp(Math.round(s * 14.0F), 3, 12);
         int discs = burst ? 5 : Mth.clamp(Math.round((float)cubes * 0.75F), 3, 6);
         Vector3f n = burst ? (state.secondary.lengthSqr() < 1.0E-6 ? new Vector3f(0.0F, 1.0F, 0.0F) : new Vector3f(kx, ky, kz).normalize()) : null;
         boolean floorBurst = burst && n.y > 0.9F;
         Vector3f t1 = null;
         Vector3f t2 = null;
         if (burst && !floorBurst) {
            Vector3f ref = Math.abs(n.y) > 0.95F ? new Vector3f(1.0F, 0.0F, 0.0F) : new Vector3f(0.0F, 1.0F, 0.0F);
            t1 = new Vector3f(n).cross(ref).normalize();
            t2 = new Vector3f(n).cross(t1).normalize();
         }

         Vector3f ft1 = t1;
         Vector3f ft2 = t2;
         VfxDraw.flame(ps, sink, (pose, vc) -> {
            if (burst) {
               for (int c = 0; c < cubes; c++) {
                  if (floorBurst) {
                     burstCube(pose, vc, s, seed, c, lifeC, lifeC * (float)lifetime, age, aCube, ladders);
                  } else {
                     orientedBurstCube(pose, vc, s, seed, c, lifeC, lifeC * (float)lifetime, age, aCube, ladders, n);
                  }
               }

               if (aDisc > 1) {
                  float lag = Mth.clamp((lifeC - 0.08F) / 0.92F, 0.0F, 1.0F);
                  float ringR = s * 1.1F * (1.0F - (1.0F - lag) * (1.0F - lag));
                  float discR = s * 0.48F * (0.6F + 0.8F * lag);

                  for (int m = 0; m < 5; m++) {
                     float am = (float)((Math.PI * 2) * (double)m / 5.0) + (float)(seed & 31) * 0.05F;
                     float lift = 0.3F + 0.5F * VoxelFlameDraw.noise01(seed, m, 40) * lag;
                     int rgb = VoxelFlameDraw.gradient(tiers, 1.0F - lag);
                     float wx;
                     float wy;
                     float wz;
                     if (floorBurst) {
                        wx = Mth.cos(am) * ringR;
                        wy = lift;
                        wz = Mth.sin(am) * ringR;
                     } else {
                        float ca = Mth.cos(am) * ringR;
                        float sa = Mth.sin(am) * ringR;
                        wx = ft1.x * ca + ft2.x * sa + n.x * lift;
                        wy = ft1.y * ca + ft2.y * sa + n.y * lift;
                        wz = ft1.z * ca + ft2.z * sa + n.z * lift;
                     }

                     VoxelFlameDraw.disc(pose, vc, wx, wy, wz, right, up, discR, aDisc, rgb);
                  }
               }
            } else {
               float amp = s * 0.5F;
               float swayPhase = (float)(seed & 0xFF) * 0.19F;

               for (int cx = 0; cx < cubes; cx++) {
                  columnCube(pose, vc, s, seed, cx, age, aCube, ladders, kx, ky, kz, amp, swayPhase);
               }

               if (aDisc > 1) {
                  for (int k = 0; k < discs; k++) {
                     float u = (float)k / (float)discs + age * 0.05F;
                     u -= (float)Mth.floor(u);
                     int a = (int)((float)aDisc * (float)Math.pow(Math.sin(Math.PI * (double)u), 0.6));
                     if (a > 1) {
                        float wx = amp * u * Mth.sin(u * 6.8F + swayPhase + age * 0.12F);
                        float wz = amp * u * Mth.sin(u * 9.1F + 1.7F + swayPhase * 0.7F + age * 0.09F);
                        int rgb = VoxelFlameDraw.gradient(tiers, u);
                        VoxelFlameDraw.disc(pose, vc, kx * u + wx, ky * u, kz * u + wz, right, up, s * 0.85F * (1.0F - 0.25F * u) * breathe, a, rgb);
                     }
                  }
               }
            }
         });
      }
   }

   private static void columnCube(
      Pose pose,
      VertexConsumer vc,
      float s,
      int seed,
      int c,
      float age,
      int aPeak,
      VoxelFlameDraw.Palette ladders,
      float kx,
      float ky,
      float kz,
      float amp,
      float swayPhase
   ) {
      int[] tiers = ladders.pick(seed + c);
      float u = VoxelFlameDraw.noise01(seed, c, 0) + age * 0.05F;
      u -= (float)Mth.floor(u);
      if (!(u > 0.97F)) {
         int tier = Math.min(3, (int)(u * 4.0F));
         int a = (int)((float)aPeak * (1.0F - 0.2F * (float)tier));
         if (a > 1) {
            float wx = amp * u * Mth.sin(u * 6.8F + swayPhase + age * 0.12F);
            float wz = amp * u * Mth.sin(u * 9.1F + 1.7F + swayPhase * 0.7F + age * 0.09F);
            float spread = s * 0.5F * (1.0F - 0.5F * u);
            float bx = (VoxelFlameDraw.noise01(seed, c, 1) - 0.5F) * 2.0F * spread;
            float bz = (VoxelFlameDraw.noise01(seed, c, 2) - 0.5F) * 2.0F * spread;
            float half = s * (0.16F + 0.12F * VoxelFlameDraw.noise01(seed, c, 3)) * (1.0F - 0.16F * (float)tier);
            int axis = (int)(VoxelFlameDraw.noise01(seed, c, 4) * 3.0F) % 3;
            float spin = (0.05F + 0.11F * VoxelFlameDraw.noise01(seed, c, 5)) * (VoxelFlameDraw.noise01(seed, c, 6) > 0.5F ? 1.0F : -1.0F);
            float ang = VoxelFlameDraw.noise01(seed, c, 7) * 6.2832F + age * spin;
            VoxelFlameDraw.cube(pose, vc, kx * u + wx + bx, ky * u, kz * u + wz + bz, half, axis, ang, a, tiers[tier]);
         }
      }
   }

   private static float travelCap(float s) {
      return s * 1.5F;
   }

   private static float burstHalf(float s, int seed, int c, int tier) {
      return Math.min(0.6F, s * (0.14F + 0.1F * VoxelFlameDraw.noise01(seed, c, 23))) * (1.0F - 0.18F * (float)tier);
   }

   private static void burstCube(
      Pose pose, VertexConsumer vc, float s, int seed, int c, float lifeC, float t, float age, int aPeak, VoxelFlameDraw.Palette ladders
   ) {
      int[] tiers = ladders.pick(seed + c);
      int tier = Math.min(3, (int)(lifeC * 4.0F));
      int a = (int)((float)aPeak * (1.0F - 0.22F * (float)tier));
      if (a > 1) {
         float yaw = VoxelFlameDraw.noise01(seed, c, 20) * 6.2832F;
         float sp = s * (0.075F + 0.055F * VoxelFlameDraw.noise01(seed, c, 21));
         float lift = Math.max(1.0F, Mth.sqrt(s / 1.5F));
         float vy0 = (0.1F + 0.12F * VoxelFlameDraw.noise01(seed, c, 22)) * lift;
         float half = burstHalf(s, seed, c, tier);
         float cy = Math.max(0.12F + half, 0.3F + vy0 * t - 0.011F * t * t);
         float spin = 0.25F * (VoxelFlameDraw.noise01(seed, c, 25) > 0.5F ? 1.0F : -1.0F);
         float ang = VoxelFlameDraw.noise01(seed, c, 24) * 6.2832F + age * spin;
         int axis = (int)(VoxelFlameDraw.noise01(seed, c, 26) * 3.0F) % 3;
         float out = Math.min(sp * t, travelCap(s));
         VoxelFlameDraw.cube(pose, vc, Mth.cos(yaw) * out, cy, Mth.sin(yaw) * out, half, axis, ang, a, tiers[3 - tier]);
      }
   }

   private static void orientedBurstCube(
      Pose pose, VertexConsumer vc, float s, int seed, int c, float lifeC, float t, float age, int aPeak, VoxelFlameDraw.Palette ladders, Vector3f n
   ) {
      int[] tiers = ladders.pick(seed + c);
      int tier = Math.min(3, (int)(lifeC * 4.0F));
      int a = (int)((float)aPeak * (1.0F - 0.22F * (float)tier));
      if (a > 1) {
         float dx = (VoxelFlameDraw.noise01(seed, c, 20) - 0.5F) * 2.0F;
         float dy = (VoxelFlameDraw.noise01(seed, c, 27) - 0.5F) * 2.0F;
         float dz = (VoxelFlameDraw.noise01(seed, c, 28) - 0.5F) * 2.0F;
         Vector3f dir = new Vector3f(dx, dy, dz).mul(0.8F).add(new Vector3f(n).mul(1.2F));
         if (dir.lengthSquared() < 1.0E-4F) {
            dir.set(n);
         }

         dir.normalize();
         float sp = s * (0.075F + 0.055F * VoxelFlameDraw.noise01(seed, c, 21));
         float half = burstHalf(s, seed, c, tier);
         float out = Math.min(sp * t, travelCap(s));
         float droop = 0.0055F * t * t;
         float spin = 0.25F * (VoxelFlameDraw.noise01(seed, c, 25) > 0.5F ? 1.0F : -1.0F);
         float ang = VoxelFlameDraw.noise01(seed, c, 24) * 6.2832F + age * spin;
         int axis = (int)(VoxelFlameDraw.noise01(seed, c, 26) * 3.0F) % 3;
         VoxelFlameDraw.cube(
            pose, vc, dir.x * out + n.x * 0.1F, dir.y * out + n.y * 0.1F - droop, dir.z * out + n.z * 0.1F, half, axis, ang, a, tiers[3 - tier]
         );
      }
   }

   private static void drawInrush(PoseStack ps, VfxDraw.Sink sink, VfxRenderState state, float life, VoxelSpriteFireDrawer.PaletteSource palette) {
      float lifeC = Mth.clamp(life, 0.0F, 1.0F);
      float age = state.ageInTicks - (float)state.delay;
      float env = lifeC > 0.88F ? (1.0F - lifeC) / 0.12F : 1.0F;
      float breathe = VoxelFlameDraw.breathe(age, state.seed & 0xFF);
      int aCube = VfxDraw.ia(135.0F * env, state.intensity);
      int aDisc = VfxDraw.ia(Math.min(255.0F, 67.5F * env * breathe), state.intensity);
      if (aCube > 1 || aDisc > 1) {
         Quaternionf q = VfxCam.orientation();
         Vector3f right = q.transform(new Vector3f(1.0F, 0.0F, 0.0F));
         Vector3f up = q.transform(new Vector3f(0.0F, 1.0F, 0.0F));
         VoxelFlameDraw.Palette ladders = palette.palette(state);
         int[] tiers = ladders.base();
         int seed = state.seed;
         float s = Math.abs(state.scale);
         float kx = (float)state.secondary.x;
         float ky = (float)state.secondary.y;
         float kz = (float)state.secondary.z;
         int cubes = Mth.clamp(Math.round(s * 12.0F), 6, 16);
         int discs = Mth.clamp(Math.round((float)cubes * 0.5F), 3, 6);
         Vector3f k = new Vector3f(kx, ky, kz);
         if (!(k.lengthSquared() < 1.0E-6F)) {
            k.normalize();
            Vector3f ref = Math.abs(k.y) > 0.95F ? new Vector3f(1.0F, 0.0F, 0.0F) : new Vector3f(0.0F, 1.0F, 0.0F);
            Vector3f t1 = new Vector3f(k).cross(ref).normalize();
            Vector3f t2 = new Vector3f(k).cross(t1).normalize();
            float lead = lifeC * 1.6F;
            VfxDraw.flame(ps, sink, (pose, vc) -> {
               for (int c = 0; c < cubes; c++) {
                  float u = lead - 0.6F * ((float)c / (float)cubes);
                  if (!(u > 1.0F)) {
                     boolean pooled = u < 0.0F;
                     u = Math.max(0.0F, u);
                     int[] cube = ladders.pick(seed + c);
                     int tier = Math.min(3, (int)(u * 4.0F));
                     int a = (int)((float)aCube * (0.7F + 0.3F * u));
                     if (a > 1) {
                        float spread = s * 0.45F * (1.0F - u) * (0.5F + VoxelFlameDraw.noise01(seed, c, 1));
                        float phi = VoxelFlameDraw.noise01(seed, c, 2) * 6.2832F + age * 0.3F * (1.0F + u) * (pooled ? 0.4F : 1.0F);
                        float ca = Mth.cos(phi) * spread;
                        float sa = Mth.sin(phi) * spread;
                        float px = kx * u + t1.x * ca + t2.x * sa;
                        float py = ky * u + t1.y * ca + t2.y * sa;
                        float pz = kz * u + t1.z * ca + t2.z * sa;
                        float half = s * (0.16F + 0.12F * VoxelFlameDraw.noise01(seed, c, 3));
                        int axis = (int)(VoxelFlameDraw.noise01(seed, c, 4) * 3.0F) % 3;
                        float spin = (0.1F + 0.16F * VoxelFlameDraw.noise01(seed, c, 5)) * (VoxelFlameDraw.noise01(seed, c, 6) > 0.5F ? 1.0F : -1.0F);
                        float ang = VoxelFlameDraw.noise01(seed, c, 7) * 6.2832F + age * spin;
                        VoxelFlameDraw.cube(pose, vc, px, py, pz, half, axis, ang, a, cube[tier]);
                     }
                  }
               }

               if (aDisc > 1) {
                  for (int m = 0; m < discs; m++) {
                     float u = Mth.clamp(lead - 0.6F * (((float)m + 0.5F) / (float)discs), 0.0F, 1.0F);
                     int rgb = VoxelFlameDraw.gradient(tiers, u);
                     VoxelFlameDraw.disc(pose, vc, kx * u, ky * u, kz * u, right, up, s * 0.85F * (1.0F - 0.4F * u) * breathe, aDisc, rgb);
                  }
               }
            });
         }
      }
   }

   @FunctionalInterface
   public interface PaletteSource {
      VoxelFlameDraw.Palette palette(VfxRenderState var1);
   }
}
