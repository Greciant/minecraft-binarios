package com.devilfruits.client.render;

import com.devilfruits.core.world.DebrisEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

public class DebrisRenderer extends EntityRenderer<DebrisEntity> {
   private final BlockRenderDispatcher blocks;

   public DebrisRenderer(Context context) {
      super(context);
      this.shadowRadius = 0.35F;
      this.blocks = context.getBlockRenderDispatcher();
   }

   public void render(DebrisEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      BlockState state = entity.getBlockState();
      if (state.getRenderShape() == RenderShape.MODEL) {
         float t = (float)entity.tickCount + partialTick;
         poseStack.pushPose();
         poseStack.translate(0.0, 0.5, 0.0);
         poseStack.mulPose(Axis.XP.rotationDegrees(t * entity.spinX));
         poseStack.mulPose(Axis.ZP.rotationDegrees(t * entity.spinZ));
         poseStack.scale(0.92F, 0.92F, 0.92F);
         poseStack.translate(-0.5, -0.5, -0.5);
         this.blocks.renderSingleBlock(state, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
         poseStack.popPose();
         super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
      }
   }

   public ResourceLocation getTextureLocation(DebrisEntity entity) {
      return InventoryMenu.BLOCK_ATLAS;
   }
}
