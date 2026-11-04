package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.entity.EnforcerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;

public class EnforcerRenderer extends MobRenderer<EnforcerEntity, OverseerModel<EnforcerEntity>> {
   private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("awakened", "textures/entity/overseer.png");
   private static final float SCALE = 0.55F;

   public EnforcerRenderer(Context ctx) {
      super(ctx, new OverseerModel(ctx.bakeLayer(OverseerModel.LAYER)), 0.5F);
   }

   protected void scale(EnforcerEntity entity, PoseStack poseStack, float partialTick) {
      poseStack.scale(0.55F, 0.55F, 0.55F);
   }

   public ResourceLocation getTextureLocation(EnforcerEntity entity) {
      return TEXTURE;
   }
}
