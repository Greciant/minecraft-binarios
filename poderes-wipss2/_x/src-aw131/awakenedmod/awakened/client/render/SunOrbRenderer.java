package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.sun.SunOrbEntity;
import com.awakenedmod.awakened.core.AwId;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

public final class SunOrbRenderer extends EntityRenderer<SunOrbEntity> {
   private static final ResourceLocation TEXTURE = AwId.of("textures/entity/sun_orb.png");
   private static final int FULL_BRIGHT = 15728880;
   private static final int RINGS = 16;
   private static final int SEGS = 24;

   public SunOrbRenderer(Context ctx) {
      super(ctx);
   }

   public ResourceLocation getTextureLocation(SunOrbEntity entity) {
      return TEXTURE;
   }

   public boolean shouldRender(SunOrbEntity entity, Frustum frustum, double camX, double camY, double camZ) {
      return true;
   }

   public void render(SunOrbEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
      super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
      this.draw(entity.radius(), (float)entity.tickCount + partialTick, poseStack, new VfxDraw.Sink(buffers));
   }

   private void draw(float radius, float age, PoseStack poseStack, VfxDraw.Sink sink) {
      poseStack.pushPose();
      poseStack.mulPose(Axis.YP.rotationDegrees(age * 0.4F));
      VfxDraw.vanillaLayer(poseStack, sink, RenderType.entityCutoutNoCull(TEXTURE), (pose, vc) -> sphere(pose, vc, radius));
      poseStack.popPose();
   }

   private static void sphere(Pose pose, VertexConsumer vc, float r) {
      for (int i = 0; i < 16; i++) {
         double t0 = Math.PI * (double)i / 16.0;
         double t1 = Math.PI * (double)(i + 1) / 16.0;
         float v0 = (float)i / 16.0F;
         float v1 = (float)(i + 1) / 16.0F;

         for (int j = 0; j < 24; j++) {
            double p0 = (Math.PI * 2) * (double)j / 24.0;
            double p1 = (Math.PI * 2) * (double)(j + 1) / 24.0;
            float u0 = (float)j / 24.0F;
            float u1 = (float)(j + 1) / 24.0F;
            Vector3f a = onSphere(t0, p0, r);
            Vector3f b = onSphere(t1, p0, r);
            Vector3f c = onSphere(t1, p1, r);
            Vector3f d = onSphere(t0, p1, r);
            vert(pose, vc, a, u0, v0);
            vert(pose, vc, b, u0, v1);
            vert(pose, vc, c, u1, v1);
            vert(pose, vc, d, u1, v0);
         }
      }
   }

   private static Vector3f onSphere(double theta, double phi, float r) {
      return new Vector3f(
         (float)((double)r * Math.sin(theta) * Math.cos(phi)), (float)((double)r * Math.cos(theta)), (float)((double)r * Math.sin(theta) * Math.sin(phi))
      );
   }

   private static void vert(Pose pose, VertexConsumer vc, Vector3f p, float u, float v) {
      float len = Math.max(1.0E-4F, p.length());
      vc.addVertex(pose, p.x, p.y, p.z)
         .setColor(-1)
         .setUv(u, v)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(15728880)
         .setNormal(pose, p.x / len, p.y / len, p.z / len);
   }
}
