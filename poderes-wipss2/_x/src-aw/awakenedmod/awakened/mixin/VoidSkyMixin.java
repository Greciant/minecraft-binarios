package com.awakenedmod.awakened.mixin;

import com.awakenedmod.awakened.core.voidview.VoidZones;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Level.class})
public abstract class VoidSkyMixin {
   @Inject(
      method = {"isRainingAt"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void awakened$voidNoRain(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
      if (VoidZones.inside((Level)this, pos)) {
         cir.setReturnValue(false);
      }
   }
}
