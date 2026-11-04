package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.client.AccessoryVisibility;
import com.awakenedmod.awakened.client.ClientPowerState;
import com.awakenedmod.awakened.client.SpiderClientState;
import com.awakenedmod.awakened.content.AwakenedItems;
import com.awakenedmod.awakened.core.AwId;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

public final class GauntletRenderLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
   private static final ResourceLocation TEXTURE = AwId.of("textures/entity/web_gauntlets.png");
   private final ModelPart cuff;

   public GauntletRenderLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, ModelPart root) {
      super(parent);
      this.cuff = root.getChild("cuff");
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
      if (player == Minecraft.getInstance().player) {
         boolean show = "spider".equals(ClientPowerState.get().powerId())
            && SpiderClientState.hasGauntlets()
            && AccessoryVisibility.of(player).shows("gauntlets", (Item)AwakenedItems.WEB_GAUNTLETS.get());
         if (show) {
            RenderType renderType = RenderType.entityCutoutNoCull(TEXTURE);
            VertexConsumer vc = buffers.getBuffer(renderType);
            poseStack.pushPose();
            ((PlayerModel)this.getParentModel()).rightArm.translateAndRotate(poseStack);
            this.cuff.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
            poseStack.pushPose();
            ((PlayerModel)this.getParentModel()).leftArm.translateAndRotate(poseStack);
            this.cuff.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
         }
      }
   }
}
