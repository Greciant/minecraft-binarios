package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.shadow.ShadowBolt;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public final class ShadowBoltRenderer extends EntityRenderer<ShadowBolt> {
   private static final int DARK = 788500;
   private static final int EDGE = 10185727;
   private static final float ARC_DEG = 160.0F;
   private static final float ARC_CENTROID = (float)(Math.sin(Math.toRadians(80.0)) / Math.toRadians(80.0));
   private static final int SLASH_BODY = 2168613;
   private static final int SLASH_EDGE = 3353919;
   private static final float OPEN_TICKS = 3.0F;
   private static final int AFTERIMAGES = 2;
   private static final float AFTERIMAGE_GAP = 0.45F;
   private static final float[] AFTERIMAGE_ALPHA = new float[]{1.0F, 0.45F, 0.2F};
   private static final float WIDTH = 0.38F;
   private static final float DEPTH = 0.07F;
   private static final float ROLL = -60.0F;
   private static final float LEAN = 30.0F;

   public ShadowBoltRenderer(Context context) {
      super(context);
   }

   public ResourceLocation getTextureLocation(ShadowBolt entity) {
      return null;
   }

   public boolean shouldRender(ShadowBolt entity, Frustum frustum, double camX, double camY, double camZ) {
      return true;
   }

   public void render(ShadowBolt entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
      super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
      this.draw(entity.isCrescent(), entity.size(), (float)entity.tickCount + partialTick, entity.getDeltaMovement(), poseStack, new VfxDraw.Sink(buffers));
   }

   private void draw(boolean crescent, float size, float age, Vec3 motion, PoseStack poseStack, VfxDraw.Sink sink) {
      if (!crescent) {
         float r = Math.max(0.3F, size);
         int orbBody = -519305196;
         int orbGlow = 1520135167;
         VfxDraw.custom(poseStack, sink, FxLayer.PAINT, (pose, vc) -> VfxGeometry.sphere(pose, vc, 0.0F, r, 12, 16, orbBody));
         VfxDraw.additive(poseStack, sink, (pose, vc) -> VfxGeometry.sphere(pose, vc, 0.0F, r * 1.06F, 10, 14, orbGlow));
      } else {
         float open = Math.min(1.0F, 0.3F + 0.7F * age / 3.0F);
         float radius = ShadowBolt.crescentRadius(size) * open;
         double speed = motion.length();
         if (!(speed < 0.001)) {
            float yaw = (float)Math.toDegrees(Math.atan2(motion.x, motion.z));
            float pitch = (float)Math.toDegrees(Math.atan2(motion.y, motion.horizontalDistance()));
            int images = Math.min(2, (int)(speed * (double)age / (double)(radius * 0.45F)));
            Vec3 back = images == 0 ? Vec3.ZERO : motion.scale((double)(-radius * 0.45F) / speed);

            for (int i = images; i >= 0; i--) {
               poseStack.pushPose();
               poseStack.translate(back.x * (double)i, back.y * (double)i, back.z * (double)i);
               slash(poseStack, sink, yaw, pitch, radius * (1.0F - 0.1F * (float)i), AFTERIMAGE_ALPHA[i], i == 0);
               poseStack.popPose();
            }
         }
      }
   }

   private static void slash(PoseStack poseStack, VfxDraw.Sink sink, float yaw, float pitch, float radius, float alpha, boolean full) {
      float width = radius * 0.38F;
      float depth = radius * 0.07F;
      poseStack.pushPose();
      poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
      poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
      poseStack.mulPose(Axis.ZP.rotationDegrees(-60.0F));
      poseStack.mulPose(Axis.XP.rotationDegrees(60.0F));
      poseStack.translate(0.0F, -radius * ARC_CENTROID, 0.0F);
      if (full) {
         VfxDraw.custom(
            poseStack,
            sink,
            FxLayer.SOLID,
            (pose, vc) -> VfxGeometry.crescentBlade(pose, vc, radius, width, depth, 160.0F, 24, argb(250.0F, 3353919), argb(250.0F, 2168613))
         );
      } else {
         VfxDraw.custom(
            poseStack,
            sink,
            FxLayer.PAINT,
            (pose, vc) -> VfxGeometry.crescentBlade(pose, vc, radius, width, depth, 160.0F, 24, argb(200.0F * alpha, 2168613), argb(200.0F * alpha, 2168613))
         );
      }

      poseStack.popPose();
   }

   private static int argb(float alpha, int rgb) {
      return (int)alpha << 24 | rgb & 16777215;
   }
}
