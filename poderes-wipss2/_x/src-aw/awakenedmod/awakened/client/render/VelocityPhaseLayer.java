package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.velocity.VelocityPalette;
import com.awakenedmod.awakened.content.velocity.VelocityVisualState;
import com.awakenedmod.awakened.core.data.VelocityVisual;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;
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

public final class VelocityPhaseLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
   private static final float OFFSET_MIN = 0.06F;
   private static final float OFFSET_MAX = 0.1F;
   private static final float VERTICAL_JITTER = 0.015F;
   private static final float BODY_ALPHA = 0.35F;
   private static final float WHITE_HOT_ALPHA = 0.16F;
   private static final float JITTER_WINDOW_TICKS = 2.0F;
   private static final int SCANLINE_SKIP_ONE_IN = 5;
   private static final float SCANLINE_SLICES_PER_TICK = 3.0F;
   private static final float MODEL_SCALE = 0.9375F;

   public VelocityPhaseLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
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
      VelocityVisual vis = VelocityVisualState.of(player);
      if (vis.phasing() && !player.isInvisible()) {
         if (player != Minecraft.getInstance().player || !FirstPersonMode.isFirstPersonPass() && !isLocalFirstPerson(player.getId())) {
            ResourceLocation texture = player.getSkin().texture();
            RenderType type = RenderType.entityTranslucentEmissive(texture);
            drawGhosts(ageInTicks, player.getId(), VelocityPalette.of(vis.color()), (dx, dy, argb) -> {
               poseStack.pushPose();
               poseStack.translate(dx / 0.9375F, dy / 0.9375F, 0.0F);
               VertexConsumer vc = buffers.getBuffer(type);
               ((PlayerModel)this.getParentModel()).renderToBuffer(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, argb);
               poseStack.popPose();
            });
         }
      }
   }

   private static boolean isLocalFirstPerson(int entityId) {
      Minecraft mc = Minecraft.getInstance();
      return mc.player != null && mc.player.getId() == entityId && mc.options.getCameraType().isFirstPerson();
   }

   private static void drawGhosts(float ageInTicks, int seed, VelocityPalette palette, VelocityPhaseLayer.GhostDraw draw) {
      int slice = (int)(ageInTicks * 3.0F);
      if (!(VelocityStreaks.noise(seed, slice, 81) * 5.0F < 1.0F)) {
         int window = (int)(ageInTicks / 2.0F);
         float distance = Mth.lerp(VelocityStreaks.noise(seed, window, 17), 0.06F, 0.1F);
         float side = VelocityStreaks.noise(seed, window, 34) < 0.5F ? 1.0F : -1.0F;
         float lift = VelocityStreaks.signed(seed, window, 51) * 0.015F;
         draw.draw(side * distance, lift, VfxColor.argb(Math.round(89.25F), palette.body()));
         draw.draw(-side * distance, -lift, VfxColor.argb(Math.round(40.8F), palette.hot()));
      }
   }

   private interface GhostDraw {
      void draw(float var1, float var2, int var3);
   }
}
