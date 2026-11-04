package com.awakenedmod.awakened.mixin;

import com.awakenedmod.awakened.core.ProneFlight;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Player.class})
public abstract class ProneFlightPoseMixin {
   @Shadow
   protected abstract boolean canPlayerFitWithinBlocksAndEntitiesWhen(Pose var1);

   @Inject(
      method = {"updatePlayerPose"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void awakened$proneFlightPose(CallbackInfo ci) {
      Player self = (Player)this;
      if (self.getForcedPose() == null && ProneFlight.holds(self)) {
         if (this.canPlayerFitWithinBlocksAndEntitiesWhen(ProneFlight.POSE)) {
            self.setPose(ProneFlight.POSE);
            ci.cancel();
         }
      }
   }
}
