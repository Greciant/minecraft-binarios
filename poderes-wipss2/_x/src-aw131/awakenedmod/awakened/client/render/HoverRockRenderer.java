package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.earth.HoverRockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class HoverRockRenderer extends EntityRenderer<HoverRockEntity> {
   private final BlockRenderDispatcher blockRenderer;
   private static final Vector3f TUMBLE_AXIS = new Vector3f(0.4F, 0.25F, 0.88F).normalize();

   public HoverRockRenderer(Context ctx) {
      super(ctx);
      this.blockRenderer = ctx.getBlockRenderDispatcher();
   }

   public ResourceLocation getTextureLocation(HoverRockEntity entity) {
      return null;
   }

   public boolean shouldRender(HoverRockEntity entity, Frustum frustum, double camX, double camY, double camZ) {
      return true;
   }

   public void render(HoverRockEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
      super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
      Block block = (Block)BuiltInRegistries.BLOCK.byId(entity.blockId());
      BlockState blockState = (block == null ? Blocks.STONE : block).defaultBlockState();
      int light = LevelRenderer.getLightColor(entity.level(), entity.blockPosition());
      this.draw(
         poseStack,
         entity.size(),
         entity.renderSpin(partialTick),
         (pose, l) -> this.blockRenderer.renderSingleBlock(blockState, pose, buffers, l, OverlayTexture.NO_OVERLAY),
         light
      );
   }

   private void draw(PoseStack poseStack, float size, float spinDeg, HoverRockRenderer.BlockSubmit submit, int light) {
      poseStack.pushPose();
      poseStack.scale(size, size, size);
      poseStack.mulPose(new Quaternionf().rotationAxis((float)Math.toRadians((double)spinDeg), TUMBLE_AXIS.x, TUMBLE_AXIS.y, TUMBLE_AXIS.z));
      poseStack.translate(-0.5, -0.5, -0.5);
      submit.submit(poseStack, light);
      poseStack.popPose();
   }

   @FunctionalInterface
   private interface BlockSubmit {
      void submit(PoseStack var1, int var2);
   }
}
