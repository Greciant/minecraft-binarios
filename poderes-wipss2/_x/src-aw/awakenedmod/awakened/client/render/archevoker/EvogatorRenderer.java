package com.awakenedmod.awakened.client.render.archevoker;

import com.awakenedmod.awakened.content.entity.archevoker.EvogatorEntity;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;

public class EvogatorRenderer extends MobRenderer<EvogatorEntity, EvogatorModel> {
   private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("awakened", "textures/entity/evogator.png");

   public EvogatorRenderer(Context ctx) {
      super(ctx, new EvogatorModel(ctx.bakeLayer(EvogatorModel.LAYER)), 0.5F);
   }

   public ResourceLocation getTextureLocation(EvogatorEntity entity) {
      return TEXTURE;
   }
}
