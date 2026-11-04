package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.entity.SuppressorEntity;
import com.awakenedmod.awakened.core.AwId;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public class SuppressorRenderer extends MobRenderer<SuppressorEntity, SuppressorModel> {
   private static final ResourceLocation TEXTURE = AwId.of("textures/entity/suppressor.png");

   public SuppressorRenderer(Context ctx) {
      super(ctx, new SuppressorModel(ctx.bakeLayer(SuppressorModel.LAYER)), 0.4F);
   }

   protected int getBlockLightLevel(SuppressorEntity entity, BlockPos pos) {
      return 15;
   }

   public ResourceLocation getTextureLocation(SuppressorEntity entity) {
      return TEXTURE;
   }
}
