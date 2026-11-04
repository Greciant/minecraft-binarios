package com.awakenedmod.awakened.client.render.archevoker;

import com.awakenedmod.awakened.content.entity.archevoker.VexplodeEntity;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;

public class VexplodeRenderer extends MobRenderer<VexplodeEntity, VexplodeModel> {
   private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("awakened", "textures/entity/vexplode.png");

   public VexplodeRenderer(Context ctx) {
      super(ctx, new VexplodeModel(ctx.bakeLayer(VexplodeModel.LAYER)), 0.3F);
   }

   public ResourceLocation getTextureLocation(VexplodeEntity entity) {
      return TEXTURE;
   }
}
