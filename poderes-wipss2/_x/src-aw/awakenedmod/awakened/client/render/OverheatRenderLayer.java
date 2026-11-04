package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.client.ClientPowerState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class OverheatRenderLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
   private static final float MIN_VISIBLE = 0.05F;

   public OverheatRenderLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
      super(parent);
   }

   public void render(
      PoseStack poseStack,
      MultiBufferSource buffers,
      int packedLight,
      AbstractClientPlayer player,
      float limbSwing,
      float limbSwingAmount,
      float partialTick,
      float ageInTicks,
      float netHeadYaw,
      float headPitch
   ) {
      float heat = 0.0F;
      if (player == Minecraft.getInstance().player) {
         ClientPowerState ps = ClientPowerState.get();
         if ("nuclear".equals(ps.powerId())) {
            heat = ps.resourceFraction();
         }
      }

      if (!(heat <= 0.05F)) {
         ResourceLocation texture = player.getSkin().texture();
         RenderType renderType = RenderType.entityTranslucentEmissive(texture);
         VertexConsumer vc = buffers.getBuffer(renderType);
         ((PlayerModel)this.getParentModel()).renderToBuffer(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, heatColor(heat));
      }
   }

   private static int heatColor(float heat) {
      float t = Mth.clamp(heat, 0.0F, 1.0F);
      int a = (int)(Mth.clamp(t * 0.9F, 0.0F, 0.9F) * 255.0F);
      int r = 255;
      int g = (int)(40.0F + t * 200.0F);
      int b = (int)(16.0F + t * t * 210.0F);
      return a << 24 | r << 16 | g << 8 | Mth.clamp(b, 0, 255);
   }
}
