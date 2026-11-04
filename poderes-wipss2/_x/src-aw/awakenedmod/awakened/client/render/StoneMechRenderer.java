package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.earth.StoneMechEntity;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;

public class StoneMechRenderer extends MobRenderer<StoneMechEntity, StoneMechModel> {
   static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("awakened", "textures/entity/stone_mech.png");

   public StoneMechRenderer(Context ctx) {
      super(ctx, new StoneMechModel(ctx.bakeLayer(StoneMechModel.LAYER)), 0.9F);
   }

   public ResourceLocation getTextureLocation(StoneMechEntity entity) {
      return TEXTURE;
   }
}
