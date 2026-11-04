package com.awakenedmod.awakened.mixin;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin({LivingEntity.class})
public abstract class LandingPoofScaleMixin {
   @ModifyConstant(
      method = {"checkFallDamage"},
      constant = {@Constant(
         doubleValue = 150.0
      )}
   )
   private double awakened$scaleLandingPoof(double original) {
      float scale = ((LivingEntity)this).getScale();
      return original * (double)Mth.clamp(scale, 0.1F, 3.0F);
   }
}
