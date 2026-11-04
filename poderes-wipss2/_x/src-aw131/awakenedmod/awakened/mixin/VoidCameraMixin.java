package com.awakenedmod.awakened.mixin;

import com.awakenedmod.awakened.client.limitless.domain.VoidIntroCamera;
import com.awakenedmod.awakened.client.voidview.VoidView;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Camera.class})
public abstract class VoidCameraMixin {
   @Shadow
   private Vec3 position;
   @Shadow
   @Final
   private Vector3f forwards;

   @Shadow
   protected abstract void setPosition(Vec3 var1);

   @Shadow
   protected abstract void setRotation(float var1, float var2, float var3);

   @Inject(
      method = {"getMaxZoom"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void awakened$voidZoom(float maxZoom, CallbackInfoReturnable<Float> cir) {
      VoidView.Sphere s = VoidView.at(this.position);
      if (s != null) {
         cir.setReturnValue(VoidView.reachInside(s, this.position, this.forwards, maxZoom));
      }
   }

   @Inject(
      method = {"setup"},
      at = {@At("TAIL")},
      require = 0
   )
   private void awakened$voidShot(CallbackInfo ci) {
      VoidIntroCamera.Shot shot = VoidIntroCamera.shot();
      if (shot != null) {
         this.setRotation(shot.yaw(), shot.pitch(), 0.0F);
         this.setPosition(shot.pos());
      }
   }
}
