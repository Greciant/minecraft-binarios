package com.devilfruits.mixin.client;

import com.devilfruits.client.logia.LogiaBodyShader;
import com.devilfruits.client.render.BodyModels;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({LivingEntityRenderer.class})
public abstract class LivingEntityRendererMixin<T extends LivingEntity, M extends EntityModel<T>> {
   @Shadow
   protected M model;

   @Inject(
      method = {"render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"},
      at = {@At("HEAD")}
   )
   private void devilfruits$logiaBegin(T entity, float yaw, float partial, PoseStack poseStack, MultiBufferSource buffers, int light, CallbackInfo ci) {
      if (entity instanceof AbstractClientPlayer player) {
         LogiaBodyShader.begin(player, poseStack, partial);
      }
   }

   @Inject(
      method = {"render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"
      )}
   )
   private void devilfruits$logiaPose(T entity, float yaw, float partial, PoseStack poseStack, MultiBufferSource buffers, int light, CallbackInfo ci) {
      if (entity instanceof AbstractClientPlayer player && this.model instanceof PlayerModel<?> playerModel) {
         LogiaBodyShader.pose(player, playerModel, poseStack);
      }
   }

   @WrapOperation(
      method = {"render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"
      )}
   )
   private void devilfruits$bodyModel(
      EntityModel<?> model,
      PoseStack pose,
      VertexConsumer consumer,
      int light,
      int overlay,
      int color,
      Operation<Void> original,
      @Local(argsOnly = true) LivingEntity entity,
      @Local(argsOnly = true) MultiBufferSource buffers
   ) {
      if (entity instanceof AbstractClientPlayer player
         && model instanceof PlayerModel<?> playerModel
         && BodyModels.render(player, playerModel, pose, buffers, light, overlay, color)) {
         return;
      }

      original.call(new Object[]{model, pose, consumer, light, overlay, color});
   }

   @Inject(
      method = {"getRenderType"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void devilfruits$logiaType(T entity, boolean bodyVisible, boolean translucent, boolean glowing, CallbackInfoReturnable<RenderType> cir) {
      if (bodyVisible && !translucent && entity instanceof AbstractClientPlayer player) {
         RenderType type = LogiaBodyShader.type(player);
         if (type != null) {
            cir.setReturnValue(type);
         }
      }
   }
}
