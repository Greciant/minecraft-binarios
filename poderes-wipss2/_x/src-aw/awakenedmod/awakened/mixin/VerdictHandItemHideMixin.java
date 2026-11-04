package com.awakenedmod.awakened.mixin;

import com.awakenedmod.awakened.client.render.VerdictChainClient;
import com.awakenedmod.awakened.content.item.VerdictChainItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ItemInHandLayer.class})
public abstract class VerdictHandItemHideMixin {
   @Inject(
      method = {"renderArmWithItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/world/entity/HumanoidArm;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void awakened$hideHeldVerdictChain(
      LivingEntity entity,
      ItemStack stack,
      ItemDisplayContext context,
      HumanoidArm arm,
      PoseStack poseStack,
      MultiBufferSource buffers,
      int light,
      CallbackInfo ci
   ) {
      if (stack.getItem() instanceof VerdictChainItem && VerdictChainClient.chainOut(entity)) {
         ci.cancel();
      }
   }
}
