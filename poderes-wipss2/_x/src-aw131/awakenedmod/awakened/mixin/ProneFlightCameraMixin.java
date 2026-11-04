package com.awakenedmod.awakened.mixin;

import com.awakenedmod.awakened.client.ProneFlightCamera;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin({Camera.class})
public abstract class ProneFlightCameraMixin {
   @Shadow
   private Entity entity;
   @Shadow
   private boolean detached;
   @Shadow
   private float partialTickTime;

   @ModifyArg(
      method = {"setup"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/Camera;setPosition(DDD)V"
      ),
      index = 1
   )
   private double awakened$liftProneFlightChaseCam(double y) {
      return this.detached && this.entity != null ? y + ProneFlightCamera.lift(this.entity, this.partialTickTime) : y;
   }
}
