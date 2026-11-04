package com.devilfruits.mixin.client;

import com.devilfruits.fruits.suke_suke.client.GlassBuffers;
import com.devilfruits.fruits.suke_suke.client.ShipLook;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({BlockEntityRenderDispatcher.class})
public abstract class ClearBlockEntityRenderDispatcherMixin {
   @Unique
   private static boolean devilfruits$glassPass;

   @Inject(
      method = {"render"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private <E extends BlockEntity> void devilfruits$hiddenShip(E blockEntity, float partial, PoseStack poseStack, MultiBufferSource buffers, CallbackInfo ci) {
      if (!devilfruits$glassPass) {
         float alpha = ShipLook.blockEntity(blockEntity, partial);
         if (!(alpha < 0.0F) && !(alpha >= 0.999F)) {
            ci.cancel();
            if (!(alpha <= 0.004F)) {
               devilfruits$glassPass = true;

               try {
                  ((BlockEntityRenderDispatcher)this).render(blockEntity, partial, poseStack, GlassBuffers.of(buffers, 12578784, alpha));
               } finally {
                  devilfruits$glassPass = false;
               }
            }
         }
      }
   }
}
