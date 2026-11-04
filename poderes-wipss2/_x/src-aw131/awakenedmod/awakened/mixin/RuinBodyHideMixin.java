package com.awakenedmod.awakened.mixin;

import com.awakenedmod.awakened.client.ruin.RuinDecayLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LivingEntityRenderer.class})
public abstract class RuinBodyHideMixin {
   @Unique
   private LivingEntity awakened$rendering;

   @Inject(
      method = {"render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"},
      at = {@At("HEAD")}
   )
   private void awakened$rememberBody(
      LivingEntity entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight, CallbackInfo ci
   ) {
      this.awakened$rendering = entity;
   }

   @ModifyArg(
      method = {"render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/MultiBufferSource;getBuffer(Lnet/minecraft/client/renderer/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"
      )
   )
   private RenderType awakened$hideDecayedBody(RenderType renderType) {
      LivingEntity entity = this.awakened$rendering;
      return entity != null ? RuinDecayLayer.swallowBody(entity, renderType) : renderType;
   }

   @Redirect(
      method = {"setupRotations(Lnet/minecraft/world/entity/LivingEntity;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V"},
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/world/entity/LivingEntity;deathTime:I",
         opcode = 180
      ),
      require = 0
   )
   private int awakened$noDeathTilt(LivingEntity entity) {
      return RuinDecayLayer.crumbling(entity) ? 0 : entity.deathTime;
   }
}
