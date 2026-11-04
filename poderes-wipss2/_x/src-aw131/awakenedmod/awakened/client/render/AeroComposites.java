package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.aero.AeroVfx;
import com.awakenedmod.awakened.content.vfx.VfxEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;

public final class AeroComposites {
   private static final int WALL_SEGS = 48;
   private static final int FUNNEL_SEGS = 32;
   private static final int FINE_SEGS = 48;
   private static final int VAPOUR_GREY = 10129800;
   private static final int FILAMENT_GREY = 7629923;

   private AeroComposites() {
   }

   public static void register() {
      VfxComposites.register(VfxEntity.Kind.TORNADO, AeroComposites::tornado);
      VfxComposites.register(VfxEntity.Kind.AIR_WALL, AeroComposites::airWall);
   }

   private static void tornado(PoseStack ps, VfxDraw.Sink c, VfxRenderState st, float life) {
      float s = st.scale;
      float h = st.secondary.y > 0.0 ? (float)st.secondary.y : s * 3.0F;
      float age = st.ageInTicks - (float)st.delay;
      float span = (float)(st.lifetime - st.delay);
      float openRaw = VfxEntity.portalOpen(age, span);
      float open = openRaw * openRaw * (3.0F - 2.0F * openRaw);
      if (!(open <= 0.02F)) {
         float in = st.intensity;
         int col = st.color;
         int col2 = st.color2;
         float boil = 1.0F + 0.04F * Mth.sin(age * 0.13F) + 0.02F * Mth.sin(age * 0.29F + 1.1F);
         float r0 = s * AeroVfx.funnelFrac(0.0F) * boil;
         float r1 = s * AeroVfx.funnelFrac(1.0F) * boil;
         float y0 = 0.1F * open;
         float y1 = h * open;
         float ys = y0 + (y1 - y0) * 0.16F;
         float rs = s * AeroVfx.funnelFrac(0.16F) * boil;
         int bodyA = VfxDraw.ia(135.0F * open, in);
         if (bodyA > 1) {
            int body = VfxColor.argb(bodyA, 10129800);
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees((float)Math.toDegrees((double)(age * 0.09F))));
            VfxDraw.custom(ps, c, FxLayer.SOLID, (pose, vc) -> VfxGeometry.cylinder(pose, vc, y0, r0, y1, r1, 32, 0.28F, 7, body));
            ps.popPose();
            int inner = VfxColor.argb(VfxDraw.ia(120.0F * open, in), 10129800);
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees((float)Math.toDegrees((double)(-age * 0.06F))));
            VfxDraw.custom(ps, c, FxLayer.SOLID, (pose, vc) -> VfxGeometry.cylinder(pose, vc, y0, r0 * 0.9F, y1, r1 * 0.9F, 32, 0.34F, 19, inner));
            ps.popPose();
            int fil = VfxColor.argb(VfxDraw.ia(150.0F * open, in), 7629923);
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees((float)Math.toDegrees((double)(age * 0.115F))));
            VfxDraw.custom(ps, c, FxLayer.PAINT, (pose, vc) -> {
               float w = s * 0.055F;
               VfxGeometry.helixStrip(pose, vc, y0, r0 * 1.02F, y1, r1 * 1.02F, 1.15F, 1.8F, 28, w, 0.0F, 0.3F, 3, fil);
               VfxGeometry.helixStrip(pose, vc, y0, r0 * 1.02F, y1, r1 * 1.02F, 1.15F, 2.3F, 28, w, 2.3F, 0.3F, 13, fil);
               VfxGeometry.helixStrip(pose, vc, y0, r0 * 1.02F, y1, r1 * 1.02F, 1.15F, 1.5F, 28, w * 1.3F, 4.4F, 0.34F, 27, fil);
            });
            ps.popPose();
            int skirt = VfxColor.argb(VfxDraw.ia(95.0F * open, in), 10129800);
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees((float)Math.toDegrees((double)(-age * 0.13F))));
            VfxDraw.custom(ps, c, FxLayer.PAINT, (pose, vc) -> VfxGeometry.cylinder(pose, vc, y0, r0 * 1.5F, ys, rs * 1.08F, 40, 0.45F, 29, skirt));
            ps.popPose();
         }

         int hiA = VfxDraw.ia(34.0F * open, in);
         if (hiA > 1) {
            int hi = VfxColor.argb(hiA, col);
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees((float)Math.toDegrees((double)(age * 0.11F))));
            VfxDraw.additive(ps, c, (pose, vc) -> VfxGeometry.cylinder(pose, vc, y0, r0 * 0.98F, y1, r1 * 1.03F, 32, 0.22F, 3, hi));
            ps.popPose();
            int hi2 = VfxColor.argb(VfxDraw.ia(22.0F * open, in), col);
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees((float)Math.toDegrees((double)(-age * 0.075F))));
            VfxDraw.additive(ps, c, (pose, vc) -> VfxGeometry.cylinder(pose, vc, y0, r0 * 0.98F, y1, r1 * 1.03F, 32, 0.3F, 41, hi2));
            ps.popPose();
            int grain = VfxColor.argb(VfxDraw.ia(14.0F * open, in), col);
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees((float)Math.toDegrees((double)(age * 0.165F))));
            VfxDraw.additive(ps, c, (pose, vc) -> VfxGeometry.cylinder(pose, vc, y0, r0 * 1.005F, y1, r1 * 1.015F, 48, 0.1F, 57, grain));
            ps.popPose();
            int skirtHi = VfxColor.argb(VfxDraw.ia(20.0F * open, in), col);
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees((float)Math.toDegrees((double)(age * 0.17F))));
            VfxDraw.additive(ps, c, (pose, vc) -> VfxGeometry.cylinder(pose, vc, y0, r0 * 1.55F, ys, rs * 1.12F, 40, 0.5F, 31, skirtHi));
            ps.popPose();
         }

         int rimA = VfxDraw.ia(40.0F * open, in);
         if (rimA > 1) {
            int rim = VfxColor.argb(rimA, VfxColor.core(col, col2, 0.5F));
            int crown = VfxColor.argb(VfxDraw.ia(20.0F * open, in), col);
            VfxDraw.additive(ps, c, (pose, vc) -> {
               VfxGeometry.ring(pose, vc, y0, r0 * 0.85F, r0 * 1.25F, 48, rim);
               VfxGeometry.ring(pose, vc, y1, r1 * 0.96F, r1 * 1.04F, 48, crown);
            });
         }

         int coreA = VfxDraw.ia(18.0F * open, in);
         if (coreA > 1) {
            int coreCol = VfxColor.argb(coreA, col);
            float baseR = s * 0.16F * boil;
            VfxDraw.additive(ps, c, (pose, vc) -> VfxGeometry.cylinder(pose, vc, 0.1F, baseR, y1, baseR * 1.6F, 12, 0.18F, 5, coreCol));
         }
      }
   }

   private static void airWall(PoseStack ps, VfxDraw.Sink c, VfxRenderState st, float life) {
      float s = st.scale;
      float h = st.secondary.y > 0.0 ? (float)st.secondary.y : s * 0.45F;
      float age = st.ageInTicks - (float)st.delay;
      float span = (float)(st.lifetime - st.delay);
      float openRaw = VfxEntity.portalOpen(age, span);
      float open = openRaw * openRaw * (3.0F - 2.0F * openRaw);
      if (!(open <= 0.02F)) {
         float in = st.intensity;
         int col = st.color;
         int col2 = st.color2;
         float boil = 1.0F + 0.025F * Mth.sin(age * 0.17F) + 0.015F * Mth.sin(age * 0.31F + 0.7F);
         float r = s * open * boil;
         float wh = h * open;
         int bodyA = VfxDraw.ia(46.0F * open, in);
         if (bodyA > 1) {
            int body = VfxColor.argb(bodyA, col);
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees((float)Math.toDegrees((double)(age * 0.045F))));
            VfxDraw.additive(ps, c, (pose, vc) -> VfxGeometry.cylinder(pose, vc, 0.05F, r * 0.99F, wh, r * 1.04F, 48, 0.1F, 7, body));
            ps.popPose();
         }

         int hotA = VfxDraw.ia(40.0F * open, in);
         if (hotA > 1) {
            int hot = VfxColor.argb(hotA, VfxColor.core(col, col2, 0.6F));
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees((float)Math.toDegrees((double)(-age * 0.03F))));
            VfxDraw.additive(ps, c, (pose, vc) -> VfxGeometry.cylinder(pose, vc, 0.05F, r * 0.955F, wh * 0.55F, r * 0.985F, 48, 0.14F, 11, hot));
            ps.popPose();
         }

         int rimA = VfxDraw.ia(52.0F * open, in);
         if (rimA > 1) {
            int rim = VfxColor.argb(rimA, VfxColor.core(col, col2, 0.5F));
            int crown = VfxColor.argb((int)((float)rimA * 0.55F), col);
            VfxDraw.additive(ps, c, (pose, vc) -> {
               VfxGeometry.ring(pose, vc, 0.06F, r * 0.93F, r * 1.06F, 56, rim);
               VfxGeometry.ring(pose, vc, wh, r * 0.97F, r * 1.05F, 56, crown);
            });
         }

         int filmA = VfxDraw.ia(12.0F * open, in);
         if (filmA > 1) {
            int film = VfxColor.argb(filmA, col);
            int churnIn = VfxColor.argb(VfxDraw.ia(20.0F * open, in), VfxColor.core(col, col2, 0.35F));
            int churnOut = VfxColor.argb(VfxDraw.ia(15.0F * open, in), col);

            for (int side = 0; side < 2; side++) {
               boolean top = side == 0;
               float y = top ? wh - 0.02F : 0.12F;
               float off = top ? -1.0F : 1.0F;
               VfxDraw.additive(ps, c, (pose, vc) -> VfxGeometry.disc(pose, vc, y, r * 0.99F, 48, film));
               ps.pushPose();
               ps.mulPose(Axis.YP.rotationDegrees((float)Math.toDegrees((double)((top ? 1.0F : -1.0F) * age * 0.035F))));
               VfxDraw.additive(ps, c, (pose, vc) -> VfxGeometry.ring(pose, vc, y + off * 0.05F, r * 0.12F, r * 0.55F, 48, 0.2F, top ? 17 : 23, churnIn));
               ps.popPose();
               ps.pushPose();
               ps.mulPose(Axis.YP.rotationDegrees((float)Math.toDegrees((double)((top ? -1.0F : 1.0F) * age * 0.025F))));
               VfxDraw.additive(ps, c, (pose, vc) -> VfxGeometry.ring(pose, vc, y + off * 0.09F, r * 0.5F, r * 0.96F, 48, 0.12F, top ? 37 : 43, churnOut));
               ps.popPose();
            }
         }
      }
   }
}
