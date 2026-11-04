package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.vfx.VfxEntity;
import com.awakenedmod.awakened.core.Lab;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import net.minecraft.util.Mth;

public final class RuinComposites {
   private static final Lab.Value GRIP_ALPHA = Lab.CLIENT.def("ruin.grip.alpha", 235.0, 0.0, 255.0, 1.0);
   private static final Lab.Value GRIP_WIDTH = Lab.CLIENT.def("ruin.grip.width", 0.045, 0.01, 0.15, 0.005);
   private static final int GRIP_FISSURES = 8;
   private static final float GRIP_STAIN = 0.36F;

   private RuinComposites() {
   }

   public static void register() {
      VfxComposites.register(VfxEntity.Kind.RUIN_GRIP_CRACK, RuinComposites::gripCrack);
   }

   public static void preloadLabKeys() {
   }

   private static void gripCrack(PoseStack ps, VfxDraw.Sink sink, VfxRenderState state, float life) {
      float tri = 1.0F - Math.abs(2.0F * Mth.clamp(life, 0.0F, 1.0F) - 1.0F);
      int aCrack = VfxDraw.ia(GRIP_ALPHA.getF() * tri, state.intensity);
      if (aCrack > 1) {
         int seed = (int)Math.round(state.secondary.x);
         float growth = Mth.clamp((float)state.secondary.y, 0.0F, 1.0F);
         float reach = Math.max(0.05F, Math.abs(state.scale)) * (0.35F + 0.65F * growth);
         float width = GRIP_WIDTH.getF() * (0.8F + 0.4F * growth);
         int crack = state.color;
         int ash = state.color2 == -1 ? VfxColor.lighten(crack, 0.5F) : state.color2;
         int col = VfxColor.argb(aCrack, crack);
         int stainRgb = VfxColor.shade(ash, 0.6F);
         int stainCore = VfxColor.argb((int)((float)aCrack * 0.5F), stainRgb);
         int stainRim = VfxColor.argb(0, stainRgb);
         VfxDraw.translucent(ps, sink, (pose, vc) -> {
            fanNoise(pose, vc, reach * 0.36F, seed, 10, stainCore, stainRim);

            for (int i = 0; i < 8; i++) {
               float a0 = (float)((Math.PI * 2) * (double)i / 8.0) + (noise(seed, i, 1) - 0.5F) * 0.7F;
               float len = reach * (0.55F + 0.45F * noise(seed, i, 2));
               float w0 = width * (0.7F + 0.6F * noise(seed, i, 3));
               float a1 = a0 + (noise(seed, i, 4) - 0.5F) * 0.6F;
               float a2 = a1 + (noise(seed, i, 5) - 0.5F) * 0.7F;
               float a3 = a2 + (noise(seed, i, 6) - 0.5F) * 0.7F;
               float r0 = reach * 0.06F;
               float r1 = len * 0.38F;
               float r2 = len * 0.72F;
               float x0 = Mth.cos(a0) * r0;
               float z0 = Mth.sin(a0) * r0;
               float x1 = x0 + Mth.cos(a1) * (r1 - r0);
               float z1 = z0 + Mth.sin(a1) * (r1 - r0);
               float x2 = x1 + Mth.cos(a2) * (r2 - r1);
               float z2 = z1 + Mth.sin(a2) * (r2 - r1);
               float x3 = x2 + Mth.cos(a3) * (len - r2);
               float z3 = z2 + Mth.sin(a3) * (len - r2);
               stroke(pose, vc, x0, 0.0F, z0, x1, 0.0F, z1, w0, w0 * 0.6F, col, col);
               stroke(pose, vc, x1, 0.0F, z1, x2, 0.0F, z2, w0 * 0.6F, w0 * 0.3F, col, col);
               stroke(pose, vc, x2, 0.0F, z2, x3, 0.0F, z3, w0 * 0.3F, 0.0F, col, col);
               if (noise(seed, i, 7) < 0.5F) {
                  float ta = a1 + (noise(seed, i, 8) < 0.5F ? 1.0F : -1.0F) * (0.7F + 0.5F * noise(seed, i, 9));
                  float tl = len * 0.3F;
                  stroke(pose, vc, x1, 0.0F, z1, x1 + Mth.cos(ta) * tl, 0.0F, z1 + Mth.sin(ta) * tl, w0 * 0.4F, 0.0F, col, col);
               }
            }
         });
      }
   }

   private static float noise(int a, int b, int c) {
      return VfxGeometry.noise01(a, b, c);
   }

   private static void v(Pose pose, VertexConsumer vc, float x, float y, float z, int color) {
      vc.addVertex(pose, x, y, z).setColor(color);
   }

   private static void tri(
      Pose pose, VertexConsumer vc, float ax, float ay, float az, int ca, float bx, float by, float bz, int cb, float cx, float cy, float cz, int cc
   ) {
      v(pose, vc, ax, ay, az, ca);
      v(pose, vc, bx, by, bz, cb);
      v(pose, vc, cx, cy, cz, cc);
      v(pose, vc, ax, ay, az, ca);
      v(pose, vc, cx, cy, cz, cc);
      v(pose, vc, bx, by, bz, cb);
   }

   private static void stroke(Pose pose, VertexConsumer vc, float ax, float ay, float az, float bx, float by, float bz, float wa, float wb, int ca, int cb) {
      float dx = bx - ax;
      float dz = bz - az;
      float len = Mth.sqrt(dx * dx + dz * dz);
      if (!(len < 1.0E-5F)) {
         float px = -dz / len;
         float pz = dx / len;
         tri(pose, vc, ax + px * wa, ay, az + pz * wa, ca, bx + px * wb, by, bz + pz * wb, cb, bx - px * wb, by, bz - pz * wb, cb);
         tri(pose, vc, ax + px * wa, ay, az + pz * wa, ca, bx - px * wb, by, bz - pz * wb, cb, ax - px * wa, ay, az - pz * wa, ca);
      }
   }

   private static void fanNoise(Pose pose, VertexConsumer vc, float radius, int seed, int rim, int core, int edge) {
      for (int k = 0; k < rim; k++) {
         int k1 = (k + 1) % rim;
         float j0 = noise(seed, k, 21);
         float j1 = noise(seed, k1, 21);
         float a0 = (float)((Math.PI * 2) * (double)k / (double)rim) + (j0 - 0.5F) * 0.5F;
         float a1 = (float)((Math.PI * 2) * (double)k1 / (double)rim) + (j1 - 0.5F) * 0.5F;
         float r0 = radius * (0.65F + 0.5F * j0);
         float r1 = radius * (0.65F + 0.5F * j1);
         tri(pose, vc, 0.0F, 0.0F, 0.0F, core, Mth.cos(a0) * r0, 0.0F, Mth.sin(a0) * r0, edge, Mth.cos(a1) * r1, 0.0F, Mth.sin(a1) * r1, edge);
      }
   }
}
