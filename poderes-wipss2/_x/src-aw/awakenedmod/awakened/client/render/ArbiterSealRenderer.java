package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.entity.ArbiterSealEntity;
import com.awakenedmod.awakened.core.AwId;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class ArbiterSealRenderer extends EntityRenderer<ArbiterSealEntity> {
   private static final ResourceLocation TEXTURE = AwId.of("textures/entity/arbiter_seal.png");
   private static final ResourceLocation CRACKS = AwId.of("textures/entity/arbiter_seal_cracks.png");
   private static final float SPIN_PER_TICK = 1.6F;
   private static final float TUMBLE_DEG = 14.0F;
   private final ModelPart cage;
   private final ModelPart core;

   public ArbiterSealRenderer(Context ctx) {
      super(ctx);
      ModelPart root = ctx.bakeLayer(ArbiterSealModel.LAYER);
      this.cage = root.getChild("cage");
      this.core = root.getChild("core");
   }

   public ResourceLocation getTextureLocation(ArbiterSealEntity entity) {
      return TEXTURE;
   }

   public boolean shouldRender(ArbiterSealEntity entity, Frustum frustum, double camX, double camY, double camZ) {
      return super.shouldRender(entity, frustum, camX, camY, camZ);
   }

   public void render(ArbiterSealEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
      super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
      float ageInTicks = (float)entity.tickCount + partialTick;
      this.draw(
         poseStack,
         ageInTicks,
         entity.hitsTaken(),
         packedLight,
         (pose, light, tint) -> this.cage.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, OverlayTexture.NO_OVERLAY, tint),
         (pose, light, tint) -> this.core.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, OverlayTexture.NO_OVERLAY, tint),
         (pose, light, tint) -> this.cage.render(pose, buffers.getBuffer(RenderType.entityTranslucentEmissive(CRACKS)), light, OverlayTexture.NO_OVERLAY, tint)
      );
   }

   private void draw(
      PoseStack poseStack,
      float ageInTicks,
      int hitsTaken,
      int light,
      ArbiterSealRenderer.PartSubmit cageSubmit,
      ArbiterSealRenderer.PartSubmit coreSubmit,
      ArbiterSealRenderer.PartSubmit crackSubmit
   ) {
      float wear = Mth.clamp((float)hitsTaken / (float)Math.max(1, 2), 0.0F, 1.0F);
      float beat = 0.5F + 0.5F * Mth.sin(ageInTicks * (0.12F + 0.34F * wear));
      poseStack.pushPose();
      poseStack.translate(0.0F, 0.35F, 0.0F);
      poseStack.mulPose(Axis.YP.rotationDegrees(ageInTicks * 1.6F));
      poseStack.mulPose(Axis.ZP.rotationDegrees(14.0F));
      int cageShade = (int)(255.0F - wear * 45.0F);
      cageSubmit.submit(poseStack, light, 0xFF000000 | cageShade << 16 | cageShade << 8 | cageShade);
      if (wear > 0.0F) {
         float glow = Mth.clamp(0.35F + 0.45F * wear + (0.1F + 0.22F * wear) * beat, 0.0F, 1.0F);
         crackSubmit.submit(poseStack, 15728880, (int)(glow * 255.0F) << 24 | 16777215);
      }

      float amp = 0.1F + 0.16F * wear;
      int coreShade = (int)(255.0F * (1.0F - amp + amp * beat));
      poseStack.pushPose();
      float swell = 1.0F + wear * (0.05F + 0.13F * beat);
      poseStack.scale(swell, swell, swell);
      coreSubmit.submit(poseStack, 15728880, 0xFF000000 | coreShade << 16 | coreShade << 8 | coreShade);
      poseStack.popPose();
      poseStack.popPose();
   }

   @FunctionalInterface
   private interface PartSubmit {
      void submit(PoseStack var1, int var2, int var3);
   }
}
