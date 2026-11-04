package com.awakenedmod.awakened.mixin;

import com.awakenedmod.awakened.content.command.vocab.effect.WeightlessDrift;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({LivingEntity.class})
public abstract class WeightlessDriftMixin {
   @ModifyConstant(
      method = {"travel"},
      constant = {@Constant(
         floatValue = 0.91F
      )}
   )
   private float awakened$weightlessFriction(float vanilla) {
      return WeightlessDrift.airFriction((LivingEntity)this, vanilla);
   }

   @Inject(
      method = {"getFrictionInfluencedSpeed"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void awakened$weightlessPush(float friction, CallbackInfoReturnable<Float> cir) {
      float vanilla = cir.getReturnValueF();
      float push = WeightlessDrift.airInput((LivingEntity)this, vanilla);
      if (push != vanilla) {
         cir.setReturnValue(push);
      }
   }
}
