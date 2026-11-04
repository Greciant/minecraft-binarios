package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.entity.ArbiterEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;

public class ArbiterRenderer extends MobRenderer<ArbiterEntity, ArbiterModel> {
   private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("awakened", "textures/entity/arbiter.png");
   public static final float SCALE = 1.15F;

   public ArbiterRenderer(Context ctx) {
      super(ctx, new ArbiterModel(ctx.bakeLayer(ArbiterModel.LAYER)), 1.0F);
      this.addLayer(new ArbiterGlowLayer(this));
   }

   protected float getFlipDegrees(ArbiterEntity entity) {
      return 0.0F;
   }

   protected void scale(ArbiterEntity entity, PoseStack poseStack, float partialTick) {
      poseStack.scale(1.15F, 1.15F, 1.15F);
   }

   public ResourceLocation getTextureLocation(ArbiterEntity entity) {
      return TEXTURE;
   }
}
