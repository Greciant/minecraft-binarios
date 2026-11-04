package com.awakenedmod.awakened.mixin;

import com.awakenedmod.awakened.core.SlowMotion;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({ClientLevel.class})
public abstract class SlowMotionTickMixin {
   @Redirect(
      method = {"tickNonPassenger"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Entity;setOldPosAndRot()V"
      )
   )
   private void awakened$holdPreviousTick(Entity entity) {
      if (SlowMotion.beginClientTick(entity)) {
         entity.setOldPosAndRot();
      }
   }

   @Redirect(
      method = {"tickNonPassenger"},
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/world/entity/Entity;tickCount:I",
         opcode = 181
      )
   )
   private void awakened$holdTickCount(Entity entity, int next) {
      if (!SlowMotion.holdingBack(entity)) {
         entity.tickCount = next;
      }
   }
}
