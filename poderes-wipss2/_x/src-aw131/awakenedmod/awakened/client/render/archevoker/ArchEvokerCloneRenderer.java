package com.awakenedmod.awakened.client.render.archevoker;

import com.awakenedmod.awakened.content.entity.archevoker.ArchEvokerCloneEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;

public class ArchEvokerCloneRenderer extends MobRenderer<ArchEvokerCloneEntity, ArchEvokerModel<ArchEvokerCloneEntity>> {
   private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("awakened", "textures/entity/archevoker.png");
   private static final float RENDER_SCALE = 1.05F;

   public ArchEvokerCloneRenderer(Context ctx) {
      super(ctx, new ArchEvokerModel(ctx.bakeLayer(ArchEvokerModel.LAYER)), 0.5F);
   }

   protected void scale(ArchEvokerCloneEntity entity, PoseStack poseStack, float partialTick) {
      poseStack.scale(1.05F, 1.05F, 1.05F);
   }

   public ResourceLocation getTextureLocation(ArchEvokerCloneEntity entity) {
      return TEXTURE;
   }
}
