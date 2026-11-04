package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.entity.ArbiterEntity;
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

public final class ArbiterGlowLayer extends RenderLayer<ArbiterEntity, ArbiterModel> {
   private static final ResourceLocation GLOW = AwId.of("textures/entity/arbiter_glow.png");
   private static final ResourceLocation GLOW_ORB = AwId.of("textures/entity/arbiter_glow_orb.png");
   private static final float ORB_OUT_FROM = 50.0F;
   private static final float ORB_OUT_TO = 70.0F;
   private static final float BODY_OUT_FROM = 55.0F;
   private static final float BODY_OUT_TO = 90.0F;

   public ArbiterGlowLayer(RenderLayerParent<ArbiterEntity, ArbiterModel> parent) {
      super(parent);
   }

   public void render(
      PoseStack poseStack,
      MultiBufferSource buffers,
      int packedLight,
      ArbiterEntity entity,
      float limbSwing,
      float limbSwingAmount,
      float partialTick,
      float ageInTicks,
      float netHeadYaw,
      float headPitch
   ) {
      float death = entity.deathTime > 0 ? (float)entity.deathTime + Mth.frac(ageInTicks) : 0.0F;
      boolean windingUp = entity.getPhase() != 0;
      boolean recess = entity.inRecess();
      int body = glowTint(ageInTicks, windingUp, death, recess, false);
      if (body >>> 24 != 0) {
         VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucentEmissive(GLOW));
         ((ArbiterModel)this.getParentModel()).renderToBuffer(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, body);
      }

      int orb = glowTint(ageInTicks, windingUp, death, recess, true);
      if (orb >>> 24 != 0) {
         VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucentEmissive(GLOW_ORB));
         ((ArbiterModel)this.getParentModel()).renderToBuffer(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, orb);
      }
   }

   private static int glowTint(float ageInTicks, boolean windingUp, float deathTime, boolean inRecess, boolean orb) {
      float rate = inRecess ? 0.5F : 1.0F;
      float pulse = 0.82F + 0.13F * Mth.sin(ageInTicks * 0.12F * rate);
      float alpha = Mth.clamp(pulse + (windingUp ? 0.15F : 0.0F), 0.0F, 1.0F);
      if (inRecess) {
         alpha *= 0.5F;
      }

      if (deathTime > 0.0F) {
         float from = orb ? 50.0F : 55.0F;
         float to = orb ? 70.0F : 90.0F;
         alpha *= 1.0F - Mth.clamp((deathTime - from) / (to - from), 0.0F, 1.0F);
      }

      return (int)(alpha * 255.0F) << 24 | 16777215;
   }
}
