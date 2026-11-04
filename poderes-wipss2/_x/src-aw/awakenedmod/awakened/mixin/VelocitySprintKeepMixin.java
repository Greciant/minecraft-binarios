package com.awakenedmod.awakened.mixin;

import com.awakenedmod.awakened.content.velocity.VelocityVisualState;
import java.util.function.BiPredicate;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({LocalPlayer.class})
public abstract class VelocitySprintKeepMixin {
   @Redirect(
      method = {"aiStep"},
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/client/player/LocalPlayer;horizontalCollision:Z",
         opcode = 180
      )
   )
   private boolean awakened$bumpDoesNotEndTheRun(LocalPlayer self) {
      return self.horizontalCollision && !awakened$overdrive(self);
   }

   @Redirect(
      method = {"aiStep"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/LocalPlayer;isInWater()Z",
         ordinal = 1
      )
   )
   private boolean awakened$shallowWaterDoesNotEndTheRun(LocalPlayer self) {
      return self.isInWater() && !awakened$overdrive(self);
   }

   @Redirect(
      method = {"aiStep"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/LocalPlayer;isInFluidType(Ljava/util/function/BiPredicate;)Z",
         ordinal = 1
      )
   )
   private boolean awakened$shallowFluidDoesNotEndTheRun(LocalPlayer self, BiPredicate swimmable) {
      return self.isInFluidType(swimmable) && !awakened$overdrive(self);
   }

   @Unique
   private static boolean awakened$overdrive(LocalPlayer self) {
      return VelocityVisualState.of(self).overdrive();
   }
}
