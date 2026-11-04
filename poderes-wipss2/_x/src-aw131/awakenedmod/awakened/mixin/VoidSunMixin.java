package com.awakenedmod.awakened.mixin;

import com.awakenedmod.awakened.core.voidview.VoidZones;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Mob.class})
public abstract class VoidSunMixin {
   @Inject(
      method = {"isSunBurnTick"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void awakened$voidNoSun(CallbackInfoReturnable<Boolean> cir) {
      if (VoidZones.inside((Mob)this)) {
         cir.setReturnValue(false);
      }
   }
}
