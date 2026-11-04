package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.client.GammaRageState;
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

public final class GammaSkinLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
   private static final float MIN_VISIBLE = 0.08F;
   private static final float MAX_ALPHA = 0.86F;
   private static final int TINT_LOW = 10935924;
   private static final int TINT_FULL = 5552164;

   public GammaSkinLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
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
      float rage = player == Minecraft.getInstance().player ? GammaRageState.localRage() : GammaRageState.trackedRage(player.getId());
      if (!(rage <= 0.08F)) {
         ResourceLocation texture = player.getSkin().texture();
         RenderType renderType = RenderType.entityTranslucent(texture);
         VertexConsumer vc = buffers.getBuffer(renderType);
         ((PlayerModel)this.getParentModel()).renderToBuffer(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, skinColor(rage));
      }
   }

   private static int skinColor(float rage) {
      float t = Mth.clamp((rage - 0.08F) / 0.92F, 0.0F, 1.0F);
      float eased = t * (2.0F - t);
      int a = (int)(eased * 0.86F * 255.0F);
      int r = (int)Mth.lerp(eased, 166.0F, 84.0F);
      int g = (int)Mth.lerp(eased, 222.0F, 184.0F);
      int b = (int)Mth.lerp(eased, 116.0F, 36.0F);
      return a << 24 | r << 16 | g << 8 | b;
   }
}
