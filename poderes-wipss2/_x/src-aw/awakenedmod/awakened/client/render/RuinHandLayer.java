package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.client.AwakenedAnimations;
import com.awakenedmod.awakened.content.ruin.RuinArm;
import com.awakenedmod.awakened.content.ruin.RuinHand;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderArmEvent;

public final class RuinHandLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> implements FirstPersonPassLayer {
   public RuinHandLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
      super(parent);
   }

   public static void preloadLabKeys() {
      RuinHandCrust.preloadLabKeys();
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(RuinHandLayer::onRenderArm);
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
      RuinHand hand = RuinArm.of(player);
      if (hand.any() && !player.isInvisible()) {
         this.draw(poseStack, new VfxDraw.Sink(buffers), hand, packedLight);
      }
   }

   @Override
   public boolean showsInFirstPerson(AbstractClientPlayer player) {
      return RuinArm.of(player).any();
   }

   private void draw(PoseStack ps, VfxDraw.Sink sink, RuinHand hand, int light) {
      ModelPart arm = ((PlayerModel)this.getParentModel()).rightArm;
      if (arm.visible) {
         ps.pushPose();
         arm.translateAndRotate(ps);
         RuinHandCrust.draw(ps, sink, arm, hand, light);
         ps.popPose();
      }
   }

   private static void onRenderArm(RenderArmEvent event) {
      Minecraft mc = Minecraft.getInstance();
      AbstractClientPlayer player = mc.player;
      if (player != null
         && event.getPlayer() == player
         && event.getArm() == HumanoidArm.RIGHT
         && !AwakenedAnimations.libraryOwnsFirstPerson()
         && !player.isInvisible()) {
         RuinHand hand = RuinArm.of(player);
         if (hand.any()) {
            ModelPart arm = LimbGlowFirstPerson.enterHand(event, player);
            if (arm != null) {
               RuinHandCrust.draw(event.getPoseStack(), LimbGlowFirstPerson.sinkOf(event), arm, hand, event.getPackedLight());
               event.getPoseStack().popPose();
            }
         }
      }
   }
}
