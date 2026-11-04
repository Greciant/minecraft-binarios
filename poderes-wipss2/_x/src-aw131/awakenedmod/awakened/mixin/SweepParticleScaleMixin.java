package com.awakenedmod.awakened.mixin;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Player.class})
public abstract class SweepParticleScaleMixin {
   private static final float MIN_SWEEP_SCALE = 0.6F;

   @Inject(
      method = {"sweepAttack"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void awakened$scaleSweepParticle(CallbackInfo ci) {
      if (((Player)this).getScale() < 0.6F) {
         ci.cancel();
      }
   }
}
