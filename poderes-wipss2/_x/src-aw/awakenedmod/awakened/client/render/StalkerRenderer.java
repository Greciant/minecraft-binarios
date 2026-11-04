package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.entity.StalkerEntity;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;

public class StalkerRenderer extends MobRenderer<StalkerEntity, StalkerModel> {
   private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("awakened", "textures/entity/stalker.png");

   public StalkerRenderer(Context ctx) {
      super(ctx, new StalkerModel(ctx.bakeLayer(StalkerModel.LAYER)), 0.4F);
   }

   public ResourceLocation getTextureLocation(StalkerEntity entity) {
      return TEXTURE;
   }
}
