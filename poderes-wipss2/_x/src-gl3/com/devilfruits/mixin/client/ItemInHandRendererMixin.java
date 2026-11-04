package com.devilfruits.mixin.client;

import com.devilfruits.client.animation.AnimationController;
import com.devilfruits.client.haki.HakiCoatRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ItemInHandRenderer.class})
public abstract class ItemInHandRendererMixin {
   @Shadow
   private void renderPlayerArm(PoseStack poseStack, MultiBufferSource buffer, int packedLight, float equippedProgress, float swingProgress, HumanoidArm side) {
      throw new AssertionError("mixin shadow");
   }

   @Inject(
      method = {"renderArmWithItem"},
      at = {@At("HEAD")}
   )
   private void devilfruits$pushArm(
      AbstractClientPlayer player,
      float partialTicks,
      float pitch,
      InteractionHand hand,
      float swingProgress,
      ItemStack stack,
      float equippedProgress,
      PoseStack poseStack,
      MultiBufferSource buffer,
      int combinedLight,
      CallbackInfo ci
   ) {
      HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
      poseStack.pushPose();
      AnimationController.applyFirstPerson(poseStack, player, arm, partialTicks);
      if (hand == InteractionHand.OFF_HAND
         && stack.isEmpty()
         && !player.isInvisible()
         && !player.isScoping()
         && AnimationController.wantsBothArmsFirstPerson(player, partialTicks)) {
         poseStack.pushPose();
         this.renderPlayerArm(poseStack, buffer, combinedLight, equippedProgress, swingProgress, arm);
         poseStack.popPose();
      }
   }

   @Inject(
      method = {"renderItem"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void devilfruits$hakiItem(
      LivingEntity entity,
      ItemStack stack,
      ItemDisplayContext context,
      boolean leftHand,
      PoseStack poseStack,
      MultiBufferSource buffer,
      int light,
      CallbackInfo ci
   ) {
      if (HakiCoatRenderer.item(entity, stack, context, leftHand, poseStack, buffer, light)) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderArmWithItem"},
      at = {@At("RETURN")}
   )
   private void devilfruits$popArm(
      AbstractClientPlayer player,
      float partialTicks,
      float pitch,
      InteractionHand hand,
      float swingProgress,
      ItemStack stack,
      float equippedProgress,
      PoseStack poseStack,
      MultiBufferSource buffer,
      int combinedLight,
      CallbackInfo ci
   ) {
      poseStack.popPose();
   }
}
