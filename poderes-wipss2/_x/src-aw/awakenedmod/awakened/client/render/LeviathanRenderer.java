package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.entity.LeviathanEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;

public class LeviathanRenderer extends MobRenderer<LeviathanEntity, LeviathanModel> {
   private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("awakened", "textures/entity/leviathan.png");
   private static final float SCALE = 1.5F;

   public LeviathanRenderer(Context ctx) {
      super(ctx, new LeviathanModel(ctx.bakeLayer(LeviathanModel.LAYER)), 2.1F);
   }

   protected void scale(LeviathanEntity entity, PoseStack poseStack, float partialTick) {
      poseStack.scale(1.5F, 1.5F, 1.5F);
   }

   protected float getFlipDegrees(LeviathanEntity entity) {
      return 0.0F;
   }

   public ResourceLocation getTextureLocation(LeviathanEntity entity) {
      return TEXTURE;
   }
}
