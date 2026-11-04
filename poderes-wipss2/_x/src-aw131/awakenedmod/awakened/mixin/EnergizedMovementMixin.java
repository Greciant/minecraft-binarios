package com.awakenedmod.awakened.mixin;

import com.awakenedmod.awakened.content.AwakenedEffects;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({ServerPlayer.class})
public abstract class EnergizedMovementMixin {
   @WrapWithCondition(
      method = {"checkMovementStatistics"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/server/level/ServerPlayer;causeFoodExhaustion(F)V"
      )}
   )
   private boolean awakened$energizedMovesForFree(ServerPlayer self, float exhaustion) {
      return !self.hasEffect(AwakenedEffects.ENERGIZED);
   }
}
