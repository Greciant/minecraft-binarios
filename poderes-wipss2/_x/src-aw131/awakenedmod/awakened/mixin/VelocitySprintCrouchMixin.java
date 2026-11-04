package com.awakenedmod.awakened.mixin;

import com.awakenedmod.awakened.content.velocity.VelocityVisualState;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({LocalPlayer.class})
public abstract class VelocitySprintCrouchMixin {
   @Inject(
      method = {"hasEnoughImpulseToStartSprinting"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void awakened$crouchNeverBlocksOverdriveSprint(CallbackInfoReturnable<Boolean> cir) {
      LocalPlayer self = (LocalPlayer)this;
      if (!self.isUnderWater() && self.isMovingSlowly() && VelocityVisualState.of(self).overdrive()) {
         float sneak = (float)self.getAttributeValue(Attributes.SNEAKING_SPEED);
         if (sneak > 0.0F && self.input.forwardImpulse >= 0.8F * sneak) {
            cir.setReturnValue(true);
         }
      }
   }
}
