package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.entity.OverseerEntity;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;

public class OverseerRenderer extends MobRenderer<OverseerEntity, OverseerModel<OverseerEntity>> {
   private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("awakened", "textures/entity/overseer.png");

   public OverseerRenderer(Context ctx) {
      super(ctx, new OverseerModel(ctx.bakeLayer(OverseerModel.LAYER)), 0.9F);
   }

   public ResourceLocation getTextureLocation(OverseerEntity entity) {
      return TEXTURE;
   }
}
