package com.awakenedmod.awakened.client.render.duelist;

import com.awakenedmod.awakened.content.entity.duelist.VarkosEntity;
import com.awakenedmod.awakened.core.AwId;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class VarkosGlowLayer extends RenderLayer<VarkosEntity, VarkosModel> {
   private static final ResourceLocation GLOW = AwId.of("textures/entity/varkos_glow.png");
   private static final int COND_R = 200;
   private static final int COND_G = 170;
   private static final int COND_B = 255;

   public VarkosGlowLayer(RenderLayerParent<VarkosEntity, VarkosModel> parent) {
      super(parent);
   }

   public void render(
      PoseStack poseStack,
      MultiBufferSource buffers,
      int packedLight,
      VarkosEntity entity,
      float limbSwing,
      float limbSwingAmount,
      float partialTick,
      float ageInTicks,
      float netHeadYaw,
      float headPitch
   ) {
      float death = entity.deathTime > 0 ? (float)entity.deathTime + Mth.frac(ageInTicks) : 0.0F;
      int color = buildGlowColor(ageInTicks, death);
      if (color >>> 24 != 0) {
         RenderType renderType = RenderType.entityTranslucentEmissive(GLOW);
         VertexConsumer vc = buffers.getBuffer(renderType);
         ((VarkosModel)this.getParentModel()).renderToBuffer(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, color);
      }
   }

   private static int buildGlowColor(float age, float death) {
      float fade = 1.0F;
      if (death > 40.0F) {
         fade = 1.0F - Mth.clamp((death - 40.0F) / 70.0F, 0.0F, 1.0F);
      }

      if (fade <= 0.01F) {
         return 0;
      } else {
         float glow = (0.45F + 0.1F * Mth.sin(age * 0.08F)) * fade;
         int a = (int)(Mth.clamp(glow, 0.0F, 0.85F) * 255.0F);
         return a <= 2 ? 0 : a << 24 | 13107200 | 43520 | 0xFF;
      }
   }
}
