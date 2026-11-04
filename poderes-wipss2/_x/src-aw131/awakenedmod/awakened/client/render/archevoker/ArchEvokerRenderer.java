package com.awakenedmod.awakened.client.render.archevoker;

import com.awakenedmod.awakened.content.entity.archevoker.ArchEvokerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;

public class ArchEvokerRenderer extends MobRenderer<ArchEvokerEntity, ArchEvokerModel<ArchEvokerEntity>> {
   private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("awakened", "textures/entity/archevoker.png");
   private static final float RENDER_SCALE = 1.15F;

   public ArchEvokerRenderer(Context ctx) {
      super(ctx, new ArchEvokerModel(ctx.bakeLayer(ArchEvokerModel.LAYER)), 0.5F);
   }

   protected void scale(ArchEvokerEntity entity, PoseStack poseStack, float partialTick) {
      poseStack.scale(1.15F, 1.15F, 1.15F);
   }

   public ResourceLocation getTextureLocation(ArchEvokerEntity entity) {
      return TEXTURE;
   }
}
