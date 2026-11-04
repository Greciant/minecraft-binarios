package com.devilfruits.mixin;

import com.devilfruits.core.physics.EntityMass;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Player.class})
public abstract class PlayerMixin {
   @Inject(
      method = {"isPushedByFluid"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void devilfruits$immovableBody(CallbackInfoReturnable<Boolean> cir) {
      if (EntityMass.immovable((Player)this)) {
         cir.setReturnValue(false);
      }
   }
}
