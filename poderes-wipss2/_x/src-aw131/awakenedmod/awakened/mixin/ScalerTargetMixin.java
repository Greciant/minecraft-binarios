package com.awakenedmod.awakened.mixin;

import com.awakenedmod.awakened.content.scaler.Scaling;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({TargetingConditions.class})
public abstract class ScalerTargetMixin {
   @Shadow
   @Final
   private boolean isCombat;

   @Inject(
      method = {"test(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;)Z"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void awakened$ignoreSpecks(LivingEntity entity, LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
      if (this.isCombat && Scaling.isUntargetable(target)) {
         cir.setReturnValue(false);
      }
   }
}
