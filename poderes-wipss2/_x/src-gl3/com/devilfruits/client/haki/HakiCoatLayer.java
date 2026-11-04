package com.devilfruits.client.haki;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

public class HakiCoatLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
   public HakiCoatLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
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
      if (!player.isInvisible()) {
         int mask = HakiClientState.blinkedOut(player) ? 0 : HakiClientState.shown(player);
         int contact = HakiClientState.contact(player);
         if (mask != 0 || contact != 0) {
            HakiCoatRenderer.humanoid(
               player,
               (HumanoidModel<?>)this.getParentModel(),
               mask,
               contact,
               HakiClientState.progress(player, partialTick),
               poseStack,
               buffers,
               packedLight,
               LivingEntityRenderer.getOverlayCoords(player, 0.0F)
            );
         }
      }
   }
}
