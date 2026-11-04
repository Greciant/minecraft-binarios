package com.devilfruits.mixin.client;

import com.devilfruits.fruits.suke_suke.client.ClearRender;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ItemInHandRenderer.class})
public abstract class ClearItemInHandRendererMixin {
   @Inject(
      method = {"renderItem"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void devilfruits$clearItem(
      LivingEntity entity,
      ItemStack stack,
      ItemDisplayContext context,
      boolean leftHand,
      PoseStack poseStack,
      MultiBufferSource buffer,
      int light,
      CallbackInfo ci
   ) {
      if (ClearRender.item((ItemInHandRenderer)this, entity, stack, context, leftHand, poseStack, buffer, light)) {
         ci.cancel();
      }
   }

   @WrapOperation(
      method = {"renderArmWithItem"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/AbstractClientPlayer;isInvisible()Z"
      )}
   )
   private boolean devilfruits$clearArm(AbstractClientPlayer player, Operation<Boolean> original) {
      return ClearRender.armHidden(player, (Boolean)original.call(new Object[]{player}));
   }
}
