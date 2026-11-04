package com.devilfruits.mixin.client;

import com.devilfruits.fruits.suke_suke.client.ClearRender;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({EntityRenderDispatcher.class})
public abstract class ClearEntityRenderDispatcherMixin {
   @Inject(
      method = {"render"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private <E extends Entity> void devilfruits$clear(
      E entity, double x, double y, double z, float yaw, float partial, PoseStack poseStack, MultiBufferSource buffers, int light, CallbackInfo ci
   ) {
      if (ClearRender.dispatch((EntityRenderDispatcher)this, entity, x, y, z, yaw, partial, poseStack, buffers, light)) {
         ci.cancel();
      }
   }
}
