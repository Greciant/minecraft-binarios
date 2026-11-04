package com.awakenedmod.awakened.mixin;

import com.awakenedmod.awakened.client.voidview.VoidView;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({BlockEntityRenderDispatcher.class})
public abstract class VoidBlockEntityMixin {
   @Inject(
      method = {"render"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private <E extends BlockEntity> void awakened$voidBlockEntity(
      E blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffers, CallbackInfo ci
   ) {
      if (VoidView.hidesWorld()) {
         ci.cancel();
      }
   }
}
