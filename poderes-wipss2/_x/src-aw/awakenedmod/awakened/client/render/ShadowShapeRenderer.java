package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.shadow.ShadowShapeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;

public final class ShadowShapeRenderer extends EntityRenderer<ShadowShapeEntity> {
   private static final int VIOLET = 10185727;
   private static final int VIOLET_HOT = 13347071;

   public ShadowShapeRenderer(Context ctx) {
      super(ctx);
   }

   public ResourceLocation getTextureLocation(ShadowShapeEntity entity) {
      return null;
   }

   public boolean shouldRender(ShadowShapeEntity entity, Frustum frustum, double camX, double camY, double camZ) {
      return true;
   }

   public void render(ShadowShapeEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
      super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
      this.draw(
         entity.shape(),
         entity.radius(),
         entity.color(),
         entity.alpha(),
         entity.height(),
         (float)entity.tickCount + partialTick,
         poseStack,
         new VfxDraw.Sink(buffers)
      );
   }

   private void draw(ShadowShapeEntity.Shape shape, float radius, int color, int alpha, float height, float age, PoseStack poseStack, VfxDraw.Sink sink) {
      if (!(radius <= 0.01F) && alpha > 1) {
         int body = Math.min(255, alpha) << 24 | color & 16777215;
         int core = Math.min(255, (int)((float)alpha * 1.2F)) << 24 | shade(color, 0.55F);
         int glow = 1520135167;
         if (shape == ShadowShapeEntity.Shape.COILS) {
            float h = Math.max(0.6F, height);
            float cinch = Math.min(1.0F, age / 10.0F);
            float rr = radius * (1.0F - 0.22F * cinch);
            float bandW = Math.max(0.05F, radius * 0.16F);
            int bands = 5;
            int noose = -1429493505;
            VfxDraw.custom(poseStack, sink, FxLayer.PAINT, (pose, vc) -> {
               for (int i = 0; i < bands; i++) {
                  float y = -h * 0.5F + h * ((float)i / (float)(bands - 1));
                  float pulse = 1.0F + 0.05F * (float)Math.sin((double)age * 0.3 + (double)i);
                  float br = rr * pulse;
                  VfxGeometry.ring(pose, vc, y, Math.max(0.02F, br - bandW), br + bandW, 28, body);
               }
            });
            VfxDraw.additive(poseStack, sink, (pose, vc) -> {
               for (int i = 0; i < bands; i++) {
                  float y = -h * 0.5F + h * ((float)i / (float)(bands - 1));
                  VfxGeometry.ring(pose, vc, y, rr - bandW * 0.4F, rr + bandW * 1.4F, 28, glow);
               }

               VfxGeometry.ring(pose, vc, h * 0.28F, rr * 0.85F, rr * 1.2F, 28, noose);
            });
         } else {
            VfxDraw.custom(poseStack, sink, FxLayer.PAINT, (pose, vc) -> {
               if (shape == ShadowShapeEntity.Shape.SPHERE) {
                  VfxGeometry.sphere(pose, vc, 0.0F, radius, 12, 18, body);
                  VfxGeometry.sphere(pose, vc, 0.0F, radius * 0.6F, 10, 14, core);
               } else {
                  VfxGeometry.disc(pose, vc, 0.02F, radius, 48, body);
                  VfxGeometry.disc(pose, vc, 0.03F, radius * 0.7F, 40, body);
                  VfxGeometry.disc(pose, vc, 0.04F, radius * 0.4F, 32, core);
               }
            });
            VfxDraw.additive(poseStack, sink, (pose, vc) -> {
               if (shape == ShadowShapeEntity.Shape.SPHERE) {
                  VfxGeometry.sphere(pose, vc, 0.0F, radius * 1.05F, 12, 16, glow);
               } else {
                  VfxGeometry.ring(pose, vc, 0.06F, radius * 0.85F, radius * 1.02F, 48, glow);
               }
            });
         }
      }
   }

   private static int shade(int rgb, float f) {
      int r = (int)((float)(rgb >> 16 & 0xFF) * f);
      int g = (int)((float)(rgb >> 8 & 0xFF) * f);
      int b = (int)((float)(rgb & 0xFF) * f);
      return r << 16 | g << 8 | b;
   }
}
