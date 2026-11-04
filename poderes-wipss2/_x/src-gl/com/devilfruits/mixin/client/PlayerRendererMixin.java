package com.devilfruits.mixin.client;

import com.devilfruits.client.animation.RenderFlags;
import com.devilfruits.client.haki.HakiCoatRenderer;
import com.devilfruits.client.logia.LogiaBodyRenderer;
import com.devilfruits.client.logia.LogiaBodyShader;
import com.devilfruits.client.render.ArmOverlays;
import com.devilfruits.client.render.FormRenderers;
import com.devilfruits.client.render.SkinOverrides;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({PlayerRenderer.class})
public abstract class PlayerRendererMixin {
   @Unique
   private AbstractClientPlayer devilfruits$handPlayer;
   @Unique
   private MultiBufferSource devilfruits$handBuffers;
   @Unique
   private PoseStack devilfruits$handPose;
   @Unique
   private ModelPart devilfruits$handArm;

   @Inject(
      method = {"getTextureLocation(Lnet/minecraft/client/player/AbstractClientPlayer;)Lnet/minecraft/resources/ResourceLocation;"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void devilfruits$logiaSkin(AbstractClientPlayer player, CallbackInfoReturnable<ResourceLocation> cir) {
      ResourceLocation masked = LogiaBodyRenderer.maskedSkin(player);
      if (masked == null) {
         masked = SkinOverrides.skin(player);
      }

      if (masked != null) {
         cir.setReturnValue(masked);
      }
   }

   @Inject(
      method = {"renderRightHand"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void devilfruits$formRightArm(PoseStack poseStack, MultiBufferSource buffer, int combinedLight, AbstractClientPlayer player, CallbackInfo ci) {
      if (FormRenderers.renderArm(player, HumanoidArm.RIGHT, poseStack, buffer, combinedLight)) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderLeftHand"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void devilfruits$formLeftArm(PoseStack poseStack, MultiBufferSource buffer, int combinedLight, AbstractClientPlayer player, CallbackInfo ci) {
      if (FormRenderers.renderArm(player, HumanoidArm.LEFT, poseStack, buffer, combinedLight)) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderHand"},
      at = {@At("HEAD")}
   )
   private void devilfruits$beginHand(
      PoseStack poseStack,
      MultiBufferSource buffer,
      int combinedLight,
      AbstractClientPlayer player,
      ModelPart rendererArm,
      ModelPart rendererArmwear,
      CallbackInfo ci
   ) {
      RenderFlags.renderingFirstPersonArm = true;
      this.devilfruits$handPlayer = player;
      this.devilfruits$handBuffers = buffer;
      this.devilfruits$handPose = poseStack;
      this.devilfruits$handArm = rendererArm;
   }

   @ModifyArg(
      method = {"renderHand"},
      index = 1,
      at = @At(
         value = "INVOKE",
         ordinal = 0,
         target = "Lnet/minecraft/client/model/geom/ModelPart;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V"
      )
   )
   private VertexConsumer devilfruits$logiaArm(VertexConsumer original) {
      VertexConsumer body = this.devilfruits$cinematicArm();
      if (body != null) {
         return body;
      } else {
         VertexConsumer masked = this.devilfruits$handPlayer == null
            ? null
            : LogiaBodyRenderer.firstPersonArm(this.devilfruits$handPlayer, this.devilfruits$handBuffers, false);
         return masked != null ? masked : this.devilfruits$overriddenArm(original);
      }
   }

   @Unique
   private VertexConsumer devilfruits$overriddenArm(VertexConsumer original) {
      if (this.devilfruits$handPlayer == null) {
         return original;
      } else {
         ResourceLocation skin = SkinOverrides.skin(this.devilfruits$handPlayer);
         return skin == null ? original : this.devilfruits$handBuffers.getBuffer(RenderType.entityTranslucent(skin));
      }
   }

   @ModifyArg(
      method = {"renderHand"},
      index = 1,
      at = @At(
         value = "INVOKE",
         ordinal = 1,
         target = "Lnet/minecraft/client/model/geom/ModelPart;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V"
      )
   )
   private VertexConsumer devilfruits$logiaSleeve(VertexConsumer original) {
      VertexConsumer body = this.devilfruits$cinematicArm();
      if (body != null) {
         return body;
      } else {
         VertexConsumer masked = this.devilfruits$handPlayer == null
            ? null
            : LogiaBodyRenderer.firstPersonArm(this.devilfruits$handPlayer, this.devilfruits$handBuffers, true);
         return masked != null ? masked : this.devilfruits$overriddenArm(original);
      }
   }

   @Inject(
      method = {"renderHand"},
      at = {@At(
         value = "INVOKE",
         ordinal = 1,
         target = "Lnet/minecraft/client/model/geom/ModelPart;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V"
      )}
   )
   private void devilfruits$hideSleeve(
      PoseStack poseStack,
      MultiBufferSource buffer,
      int combinedLight,
      AbstractClientPlayer player,
      ModelPart rendererArm,
      ModelPart rendererArmwear,
      CallbackInfo ci
   ) {
      boolean right = ((PlayerModel)((PlayerRenderer)this).getModel()).rightArm == rendererArm;
      if (ArmOverlays.hideSleeve(player, right)) {
         rendererArmwear.visible = false;
      }
   }

   @Unique
   private VertexConsumer devilfruits$cinematicArm() {
      if (this.devilfruits$handPlayer != null && this.devilfruits$handArm != null) {
         boolean right = ((PlayerModel)((PlayerRenderer)this).getModel()).rightArm == this.devilfruits$handArm;
         RenderType type = LogiaBodyShader.arm(this.devilfruits$handPlayer, this.devilfruits$handArm, right ? 2 : 3, this.devilfruits$handPose);
         return type == null ? null : this.devilfruits$handBuffers.getBuffer(type);
      } else {
         return null;
      }
   }

   @Inject(
      method = {"renderHand"},
      at = {@At("RETURN")}
   )
   private void devilfruits$endHand(
      PoseStack poseStack,
      MultiBufferSource buffer,
      int combinedLight,
      AbstractClientPlayer player,
      ModelPart rendererArm,
      ModelPart rendererArmwear,
      CallbackInfo ci
   ) {
      boolean right = ((PlayerModel)((PlayerRenderer)this).getModel()).rightArm == rendererArm;
      HakiCoatRenderer.firstPersonArm(player, rendererArm, rendererArmwear, right, poseStack, buffer, combinedLight);
      ArmOverlays.render(player, rendererArm, rendererArmwear, right, poseStack, buffer, combinedLight);
      RenderFlags.renderingFirstPersonArm = false;
      this.devilfruits$handPlayer = null;
      this.devilfruits$handBuffers = null;
      this.devilfruits$handPose = null;
      this.devilfruits$handArm = null;
   }
}
