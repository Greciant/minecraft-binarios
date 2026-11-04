package com.awakenedmod.awakened.mixin;

import com.awakenedmod.awakened.content.summoning.PossessControl;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   targets = {"net.minecraft.server.level.ChunkMap$TrackedEntity"}
)
public abstract class PossessedTrackingMixin {
   @Shadow
   @Final
   Entity entity;

   @Inject(
      method = {"getEffectiveRange"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void awakened$extendRangeWhilePiloted(CallbackInfoReturnable<Integer> cir) {
      if (PossessControl.isPiloted(this.entity)) {
         cir.setReturnValue(536870911);
      }
   }
}
