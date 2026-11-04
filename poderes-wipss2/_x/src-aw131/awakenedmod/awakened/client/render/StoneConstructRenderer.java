package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.earth.StoneConstructEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class StoneConstructRenderer extends EntityRenderer<StoneConstructEntity> {
   private final BlockRenderDispatcher blockRenderer;
   private static final float RISE_TICKS = 6.0F;
   private static final float OVERSHOOT = 0.06F;

   public StoneConstructRenderer(Context ctx) {
      super(ctx);
      this.blockRenderer = ctx.getBlockRenderDispatcher();
   }

   public ResourceLocation getTextureLocation(StoneConstructEntity entity) {
      return null;
   }

   public boolean shouldRender(StoneConstructEntity entity, Frustum frustum, double camX, double camY, double camZ) {
      return true;
   }

   public void render(StoneConstructEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
      super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
      Block block = (Block)BuiltInRegistries.BLOCK.byId(entity.blockId());
      BlockState blockState = (block == null ? Blocks.STONE : block).defaultBlockState();
      if (!blockState.isAir()) {
         int lifetime = entity.lifetime();
         float ageInTicks = (float)entity.tickCount + partialTick;
         int light = LevelRenderer.getLightColor(entity.level(), entity.blockPosition());
         float life = lifetime <= 0 ? 0.0F : Mth.clamp(ageInTicks / (float)lifetime, 0.0F, 1.0F);
         this.draw(
            poseStack,
            life,
            ageInTicks,
            entity.yawDeg(),
            entity.width(),
            entity.height(),
            (pose, l) -> this.blockRenderer.renderSingleBlock(blockState, pose, buffers, l, OverlayTexture.NO_OVERLAY),
            light
         );
      }
   }

   private void draw(
      PoseStack poseStack, float life, float ageInTicks, float yawDeg, float width, float height, StoneConstructRenderer.BlockSubmit submit, int light
   ) {
      float widthK = Mth.clamp(ageInTicks / 1.5F, 0.4F, 1.0F);
      float rise = Mth.clamp(ageInTicks / 6.0F, 0.0F, 1.0F);
      float ease = 1.0F - (1.0F - rise) * (1.0F - rise);
      float settle = 1.0F + 0.06F * Mth.sin(rise * (float) Math.PI);
      float heightK = ease * settle;
      float melt = life < 0.8F ? 1.0F : 1.0F - (life - 0.8F) / 0.2F;
      widthK *= melt;
      heightK *= melt;
      if (!(widthK <= 0.01F) && !(heightK <= 0.01F)) {
         poseStack.pushPose();
         if (yawDeg != 0.0F) {
            poseStack.mulPose(Axis.YP.rotationDegrees(yawDeg));
         }

         float side = width * widthK;
         int segments = Math.max(1, Mth.ceil(height * heightK / Math.max(0.001F, side)));
         poseStack.scale(side, side, side);
         poseStack.translate(-0.5, 0.0, -0.5);

         for (int i = 0; i < segments; i++) {
            submit.submit(poseStack, light);
            poseStack.translate(0.0, 1.0, 0.0);
         }

         poseStack.popPose();
      }
   }

   @FunctionalInterface
   private interface BlockSubmit {
      void submit(PoseStack var1, int var2);
   }
}
