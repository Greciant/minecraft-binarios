package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.velocity.VelocityPalette;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

final class VelocityChargeStatic {
   private static final float HAND_X = -0.06F;
   private static final float HAND_Y = 0.62F;
   private static final float HAND_Z = 0.0F;
   private static final int STRANDS_MIN = 1;
   private static final int STRANDS_MAX = 8;
   private static final float REACH_START = 0.5F;
   private static final float REACH_FULL = 0.32F;
   private static final float REACH_SPREAD = 0.5F;
   private static final float UP_ARM_FLOOR = -0.3F;
   private static final int STRAND_SEGS = 4;
   private static final float STRAND_JITTER = 0.2F;
   private static final float STRAND_HALF = 0.014F;
   private static final float STRAND_ALPHA_MIN = 0.18F;
   private static final float STRAND_ALPHA_MAX = 0.65F;
   private static final float STRAND_FAR_ALPHA = 0.45F;
   private static final float CORE_MIN = 0.012F;
   private static final float CORE_MAX = 0.1F;
   private static final float CORE_ALPHA_MIN = 0.3F;
   private static final float CORE_ALPHA_MAX = 0.6F;
   private static final float HALO_RADIUS_MUL = 1.8F;
   private static final float HALO_ALPHA_MIN = 0.14F;
   private static final float HALO_ALPHA_MAX = 0.45F;
   private static final int SPHERE_RINGS = 4;
   private static final int SPHERE_SEGS = 8;
   private static final float WINDOW_TICKS = 2.0F;
   private static final float FLICKER = 0.15F;
   private static final float TWO_PI = (float) (Math.PI * 2);

   private VelocityChargeStatic() {
   }

   static void palm(PoseStack ps, float side) {
      ps.translate(side * -0.06F, 0.62F, 0.0F);
   }

   static void draw(PoseStack ps, VfxDraw.Sink sink, VelocityPalette palette, float charge, float age, int seed) {
      float pour = Mth.clamp(charge, 0.0F, 1.0F);
      int window = (int)(age / 2.0F);
      float flicker = 1.0F + 0.15F * VelocityStreaks.signed(seed, window, 119);
      int strands = Math.round(Mth.lerp(pour, 1.0F, 8.0F));
      float reach = Mth.lerp(pour, 0.5F, 0.32F);
      float alphaHand = Mth.lerp(pour, 0.18F, 0.65F) * flicker;
      float core = Mth.lerp(pour, 0.012F, 0.1F);
      int handCol = col(alphaHand, palette.hot());
      int farCol = col(alphaHand * 0.45F, palette.body());
      int coreCol = col(Mth.lerp(pour, 0.3F, 0.6F) * flicker, palette.hot());
      int haloCol = col(Mth.lerp(pour, 0.14F, 0.45F) * flicker, palette.body());
      VfxDraw.additive(ps, sink, (pose, vc) -> {
         Vector3f hand = new Vector3f();

         for (int i = 0; i < strands; i++) {
            float u = Mth.lerp(VelocityStreaks.noise(seed, window, i * 3 + 1), -0.3F, 1.0F);
            float phi = VelocityStreaks.noise(seed, window, i * 3 + 2) * (float) (Math.PI * 2);
            float r = Mth.sqrt(Math.max(0.0F, 1.0F - u * u));
            float len = reach * (1.0F + 0.5F * VelocityStreaks.signed(seed, window, i * 3 + 3) * 0.5F);
            Vector3f far = new Vector3f(r * Mth.cos(phi), u, r * Mth.sin(phi)).mul(len);
            VelocityStreaks.lightning(pose, vc, hand, far, 0.014F, 4, 0.2F, seed * 31 + window * 7 + i, handCol, farCol);
         }

         VfxGeometry.sphere(pose, vc, 0.0F, core * 1.8F, 4, 8, haloCol);
         VfxGeometry.sphere(pose, vc, 0.0F, core, 4, 8, coreCol);
      });
   }

   private static int col(float alpha, int rgb) {
      return VfxColor.argb(Math.round(Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F), rgb);
   }
}
