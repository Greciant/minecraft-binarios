package com.awakenedmod.awakened.mixin;

import com.awakenedmod.awakened.client.render.ArmStretch;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {PlayerModel.class},
   priority = 2500
)
public abstract class PlayerArmStretchMixin {
   @Inject(
      method = {"setupAnim(Lnet/minecraft/world/entity/Entity;FFFFF)V"},
      at = {@At("TAIL")}
   )
   private void awakened$stretchArm(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
      if (entity instanceof LivingEntity living) {
         ArmStretch.apply((PlayerModel<?>)this, living, ageInTicks);
      }
   }
}
