package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.entity.sentinel.SentinelEntity;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;

public class SentinelRenderer extends MobRenderer<SentinelEntity, SentinelModel> {
   static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("awakened", "textures/entity/sentinel.png");

   public SentinelRenderer(Context ctx) {
      super(ctx, new SentinelModel(ctx.bakeLayer(SentinelModel.LAYER)), 1.4F);
      this.addLayer(new SentinelGlowLayer(this));
   }

   public ResourceLocation getTextureLocation(SentinelEntity entity) {
      return TEXTURE;
   }
}
