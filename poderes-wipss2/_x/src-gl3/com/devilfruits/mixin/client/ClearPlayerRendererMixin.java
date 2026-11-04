package com.devilfruits.mixin.client;

import com.devilfruits.fruits.suke_suke.client.ClearRender;
import com.devilfruits.fruits.suke_suke.client.Visibility;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({PlayerRenderer.class})
public abstract class ClearPlayerRendererMixin {
   @Inject(
      method = {"renderRightHand"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void devilfruits$clearRightArm(PoseStack poseStack, MultiBufferSource buffer, int light, AbstractClientPlayer player, CallbackInfo ci) {
      if (ClearRender.arm((PlayerRenderer)this, player, HumanoidArm.RIGHT, poseStack, buffer, light)) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderLeftHand"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void devilfruits$clearLeftArm(PoseStack poseStack, MultiBufferSource buffer, int light, AbstractClientPlayer player, CallbackInfo ci) {
      if (ClearRender.arm((PlayerRenderer)this, player, HumanoidArm.LEFT, poseStack, buffer, light)) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"getArmPose"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void devilfruits$emptyHanded(AbstractClientPlayer player, InteractionHand hand, CallbackInfoReturnable<ArmPose> cir) {
      if (Visibility.itemsHidden(player)) {
         cir.setReturnValue(ArmPose.EMPTY);
      }
   }
}
