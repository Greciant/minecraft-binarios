package com.devilfruits.mixin.client;

import com.devilfruits.client.animation.AnimationController;
import com.devilfruits.client.animation.RenderFlags;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({HumanoidModel.class})
public abstract class HumanoidModelMixin<T extends LivingEntity> {
   @Inject(
      method = {"setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V"},
      at = {@At("TAIL")}
   )
   private void devilfruits$applyAnimation(
      T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci
   ) {
      if (entity instanceof AbstractClientPlayer player && !RenderFlags.renderingFirstPersonArm) {
         AnimationController.applyToModel((HumanoidModel<?>)this, player, ageInTicks);
      }
   }
}
