package com.awakenedmod.awakened.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

public final class HeadAnchorLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
   public HeadAnchorLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
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
      if (player != Minecraft.getInstance().player || !FirstPersonMode.isFirstPersonPass()) {
         this.sample(poseStack, player.getId());
      }
   }

   private void sample(PoseStack poseStack, int id) {
      poseStack.pushPose();
      ((PlayerModel)this.getParentModel()).head.translateAndRotate(poseStack);
      HeadAnchor.sample(id, poseStack.last().pose());
      poseStack.popPose();
   }
}
