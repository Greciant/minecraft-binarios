package com.awakenedmod.awakened.mixin;

import com.awakenedmod.awakened.core.SlowMotion;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin({LevelRenderer.class})
public abstract class SlowMotionPartialTickMixin {
   @ModifyArgs(
      method = {"renderLevel"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/LevelRenderer;renderEntity(Lnet/minecraft/world/entity/Entity;DDDFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V"
      )
   )
   private void awakened$stretchPartialTick(Args args) {
      Entity entity = (Entity)args.get(0);
      args.set(4, SlowMotion.partialTick(entity, (Float)args.get(4)));
   }
}
