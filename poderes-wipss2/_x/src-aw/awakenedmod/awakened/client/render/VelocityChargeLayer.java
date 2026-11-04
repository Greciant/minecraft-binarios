package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.velocity.VelocityPalette;
import com.awakenedmod.awakened.content.velocity.VelocityVisualState;
import com.awakenedmod.awakened.core.data.VelocityVisual;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

public final class VelocityChargeLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> implements FirstPersonPassLayer {
   public VelocityChargeLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
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
      if (vis.charging() && !player.isInvisible()) {
         this.draw(
            poseStack, new VfxDraw.Sink(buffers), VelocityPalette.of(vis.color()), vis.chargeFraction(), (float)player.tickCount + partialTick, player.getId()
         );
      }
   }

   @Override
   public boolean showsInFirstPerson(AbstractClientPlayer player) {
      return VelocityVisualState.of(player).charging();
   }

   private void draw(PoseStack ps, VfxDraw.Sink sink, VelocityPalette palette, float charge, float age, int seed) {
      ps.pushPose();
      ((PlayerModel)this.getParentModel()).rightArm.translateAndRotate(ps);
      VelocityChargeStatic.palm(ps, 1.0F);
      VelocityChargeStatic.draw(ps, sink, palette, charge, age, seed);
      ps.popPose();
   }
}
