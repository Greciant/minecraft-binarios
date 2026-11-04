package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.entity.sentinel.AbsorbedPower;
import com.awakenedmod.awakened.content.entity.sentinel.SentinelEntity;
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

public final class SentinelGlowLayer extends RenderLayer<SentinelEntity, SentinelModel> {
   private static final ResourceLocation GLOW = AwId.of("textures/entity/sentinel_glow.png");

   public SentinelGlowLayer(RenderLayerParent<SentinelEntity, SentinelModel> parent) {
      super(parent);
   }

   public void render(
      PoseStack poseStack,
      MultiBufferSource buffers,
      int packedLight,
      SentinelEntity entity,
      float limbSwing,
      float limbSwingAmount,
      float partialTick,
      float ageInTicks,
      float netHeadYaw,
      float headPitch
   ) {
      int mask = entity.getAbsorbedMask();
      if (mask != 0) {
         int color = blendedGlow(mask, ageInTicks, entity.getEscalation(), entity.getAction());
         RenderType renderType = RenderType.entityTranslucentEmissive(GLOW);
         VertexConsumer vc = buffers.getBuffer(renderType);
         ((SentinelModel)this.getParentModel()).renderToBuffer(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, color);
      }
   }

   private static int blendedGlow(int mask, float ageInTicks, byte escalation, byte action) {
      long r = 0L;
      long g = 0L;
      long b = 0L;
      int n = 0;

      for (AbsorbedPower power : AbsorbedPower.values()) {
         if ((mask & power.bit()) != 0) {
            int c = power.glowColor();
            r += (long)(c >> 16 & 0xFF);
            g += (long)(c >> 8 & 0xFF);
            b += (long)(c & 0xFF);
            n++;
         }
      }

      if (n == 0) {
         return 0;
      } else {
         r /= (long)n;
         g /= (long)n;
         b /= (long)n;
         float pulse = 0.8F + 0.2F * Mth.sin(ageInTicks * 0.18F);
         float load = Mth.clamp(0.4F + (float)n * 0.04F + (float)(escalation - 1) * 0.08F, 0.0F, 0.75F);
         boolean firing = action != 0 && action != 14;
         float alpha = Mth.clamp(load * pulse + (firing ? 0.15F : 0.0F), 0.0F, 0.8F);
         int a = (int)(alpha * 255.0F);
         return a << 24 | (int)r << 16 | (int)g << 8 | (int)b;
      }
   }
}
