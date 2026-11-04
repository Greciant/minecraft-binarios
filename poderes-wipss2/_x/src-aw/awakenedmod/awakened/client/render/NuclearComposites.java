package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.vfx.VfxEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.util.Mth;

public final class NuclearComposites {
   private static final int WHITE = 16774610;
   private static final int GOLD = 16762950;
   private static final int AMBER = 16744990;
   private static final int DUST = 9861210;

   private NuclearComposites() {
   }

   public static void register() {
      VfxComposites.register(VfxEntity.Kind.MUSHROOM, NuclearComposites::mushroom);
      VfxComposites.register(VfxEntity.Kind.DUST_DOME, NuclearComposites::dustDome);
   }

   private static void mushroom(PoseStack ps, VfxDraw.Sink c, VfxRenderState state, float life) {
      float s = state.scale;
      float age = state.ageInTicks;
      float in = state.intensity;
      float ease = 1.0F - (1.0F - life) * (1.0F - life);
      float stemH = s * (0.5F + 2.3F * ease);
      float fade = life < 0.78F ? 1.0F : 1.0F - (life - 0.78F) / 0.22F;
      float capLife = Mth.clamp((life - 0.15F) / 0.55F, 0.0F, 1.0F);
      float capR = s * (0.6F + 1.6F * capLife);
      int seed = (int)(age * 0.5F);
      float rough = 0.28F;
      int stem = VfxColor.argb(VfxDraw.ia(95.0F * fade, in), 16744990);
      int capGold = VfxColor.argb(VfxDraw.ia(120.0F * fade, in), 16762950);
      int capCore = VfxColor.argb(VfxDraw.ia(100.0F * fade, in), 16774610);
      int lip = VfxColor.argb(VfxDraw.ia(78.0F * fade, in), 16744990);
      float baseA = life < 0.25F ? 1.0F - life / 0.25F : 0.0F;
      int baseFlash = VfxColor.argb(VfxDraw.ia(150.0F * baseA, in), 16774610);
      VfxDraw.additive(ps, c, (pose, vc) -> {
         if (baseA > 0.0F) {
            VfxGeometry.sphere(pose, vc, s * 0.4F, s * 0.8F, 8, 12, baseFlash);
         }

         VfxGeometry.cylinder(pose, vc, 0.1F, s * 0.55F, stemH, s * 0.32F, 16, rough, seed, stem);
         VfxGeometry.dome(pose, vc, stemH - capR * 0.15F, capR, 9, 20, rough, seed + 7, capGold);
         VfxGeometry.dome(pose, vc, stemH - capR * 0.1F, capR * 0.62F, 7, 16, rough * 0.6F, seed + 13, capCore);
         VfxGeometry.dome(pose, vc, stemH - capR * 0.28F, capR * 1.05F, 5, 22, rough * 1.3F, seed + 19, lip);
         float impact = Math.max(0.0F, 1.0F - age / 9.0F);
         float shake = 1.0F + impact * 0.13F * Mth.sin(age * 2.2F);
         float ri = s * (0.4F + 1.7F * ease) * shake;
         float ro = s * (0.75F + 1.7F * ease) * shake;
         int ringA = VfxColor.argb(VfxDraw.ia((120.0F + 150.0F * impact) * (1.0F - life), in), 16762950);
         VfxGeometry.ring(pose, vc, 0.12F, ri, ro, 44, 0.3F, seed + 31, ringA);
         if (impact > 0.05F) {
            int flashA = VfxColor.argb(VfxDraw.ia(200.0F * impact, in), 16774610);
            VfxGeometry.ring(pose, vc, 0.14F, ri * 0.4F, ri * 0.85F, 40, 0.35F, seed + 53, flashA);
         }
      });
   }

   private static void dustDome(PoseStack ps, VfxDraw.Sink c, VfxRenderState state, float life) {
      float s = state.scale;
      float in = state.intensity;
      float ease = 1.0F - (1.0F - life) * (1.0F - life);
      float r = s * (0.5F + 0.9F * ease);
      int a = VfxDraw.ia(70.0F * (1.0F - life), in);
      int dust = VfxColor.argb(a, 9861210);
      int ringC = VfxColor.argb(VfxDraw.ia(150.0F * (1.0F - life), in), 16762950);
      float coreA = life < 0.4F ? 1.0F - life / 0.4F : 0.0F;
      int core = VfxColor.argb(VfxDraw.ia(150.0F * coreA, in), 16774610);
      VfxDraw.additive(ps, c, (pose, vc) -> {
         if (a > 1) {
            VfxGeometry.sphere(pose, vc, 0.0F, r, 9, 16, dust);
         }

         if (coreA > 0.0F) {
            VfxGeometry.sphere(pose, vc, 0.0F, s * 0.5F, 8, 14, core);
         }

         VfxGeometry.ring(pose, vc, 0.05F, Math.max(0.0F, r - s * 0.3F), r + s * 0.2F, 32, 0.22F, 3, ringC);
      });
   }
}
