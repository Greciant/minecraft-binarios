package com.awakenedmod.awakened.mixin;

import com.awakenedmod.awakened.core.TickFreeze;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin({PathNavigation.class})
public abstract class SlowedPathTimeoutMixin {
   @Shadow
   @Final
   protected Mob mob;

   @ModifyConstant(
      method = {"doStuckDetection"},
      constant = {@Constant(
         doubleValue = 3.0
      )}
   )
   private double awakened$stretchTimeoutForSlowedMobs(double budget) {
      return budget * (double)TickFreeze.divisorOf(this.mob);
   }
}
