package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.client.AccessoryVisibility;
import com.awakenedmod.awakened.client.ClientPowerState;
import com.awakenedmod.awakened.content.AwakenedItems;
import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.data.RelicData;
import com.awakenedmod.awakened.core.relic.Relic;
import com.awakenedmod.awakened.core.relic.RelicRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Arrays;
import java.util.List;
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

public final class BeltRenderLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
   private static final ResourceLocation TEXTURE = AwId.of("textures/entity/relic_belt.png");
   private final ModelPart band;
   private final ModelPart[] gems;

   public BeltRenderLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, ModelPart root) {
      super(parent);
      this.band = root.getChild("band");
      this.gems = new ModelPart[3];

      for (int i = 0; i < this.gems.length; i++) {
         this.gems[i] = root.getChild(BeltModel.gem(i));
      }
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
      boolean wearing;
      List<String> attuned;
      if (player == Minecraft.getInstance().player) {
         wearing = ClientPowerState.get().hasRelicBelt();
         attuned = ClientPowerState.get().attunedRelics();
      } else {
         RelicData data = (RelicData)player.getData(AwakenedAttachments.RELIC_DATA);
         wearing = data.hasBelt();
         attuned = data.attuned();
      }

      if (wearing && AccessoryVisibility.of(player).shows("belt", (Item)AwakenedItems.RELIC_BELT.get())) {
         int[] colors = beltGemColors(attuned);
         RenderType renderType = RenderType.entityCutoutNoCull(TEXTURE);
         VertexConsumer vc = buffers.getBuffer(renderType);
         poseStack.pushPose();
         ((PlayerModel)this.getParentModel()).body.translateAndRotate(poseStack);
         this.band.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);

         for (int i = 0; i < this.gems.length && i < colors.length; i++) {
            this.gems[i].render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, colors[i]);
         }

         poseStack.popPose();
      }
   }

   private static int[] beltGemColors(List<String> attuned) {
      int[] colors = new int[attuned.size()];
      int n = 0;

      for (String id : attuned) {
         Relic relic = RelicRegistry.get(id);
         if (relic != null) {
            colors[n++] = relic.themeColor();
         }
      }

      return Arrays.copyOf(colors, n);
   }
}
