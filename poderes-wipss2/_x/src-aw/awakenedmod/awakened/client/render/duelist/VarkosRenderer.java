package com.awakenedmod.awakened.client.render.duelist;

import com.awakenedmod.awakened.content.entity.duelist.VarkosEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;

public class VarkosRenderer extends MobRenderer<VarkosEntity, VarkosModel> {
   private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("awakened", "textures/entity/varkos.png");
   private static final float RENDER_SCALE = 1.35F;

   public VarkosRenderer(Context ctx) {
      super(ctx, new VarkosModel(ctx.bakeLayer(VarkosModel.LAYER)), 0.5F);
      this.addLayer(new VarkosGlowLayer(this));
   }

   protected float getFlipDegrees(VarkosEntity entity) {
      return 0.0F;
   }

   protected void scale(VarkosEntity entity, PoseStack poseStack, float partialTick) {
      poseStack.scale(1.35F, 1.35F, 1.35F);
   }

   public ResourceLocation getTextureLocation(VarkosEntity entity) {
      return TEXTURE;
   }
}
