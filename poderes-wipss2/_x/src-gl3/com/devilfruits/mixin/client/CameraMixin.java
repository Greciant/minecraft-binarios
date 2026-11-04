package com.devilfruits.mixin.client;

import com.devilfruits.client.camera.CameraOffsets;
import com.devilfruits.client.camera.ShoulderCamera;
import com.devilfruits.client.dev.CaptureRig;
import com.devilfruits.client.form.FormMovements;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Camera.class})
public abstract class CameraMixin {
   @Shadow
   private Vec3 position;
   @Shadow
   private boolean detached;
   @Unique
   private Vec3 devilfruits$pivot;

   @Shadow
   protected abstract void setPosition(Vec3 var1);

   @Shadow
   protected abstract void setRotation(float var1, float var2);

   @Inject(
      method = {"setup"},
      at = {@At("TAIL")}
   )
   private void devilfruits$captureRig(BlockGetter level, Entity entity, boolean detached, boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {
      Vec3 pivot = this.devilfruits$pivot;
      this.devilfruits$pivot = null;
      if (CaptureRig.active()) {
         this.setRotation(CaptureRig.yaw(), CaptureRig.pitch());
         this.setPosition(CaptureRig.position(partialTick));
         this.detached = true;
      } else if (detached && pivot != null) {
         Vec3 at = ShoulderCamera.place(entity, pivot, this.position, thirdPersonReverse, partialTick);
         if (at != null) {
            this.setPosition(at);
            if (ShoulderCamera.hideModel()) {
               this.detached = false;
            }
         }
      } else if (!detached) {
         double dy = CameraOffsets.firstPerson(entity, partialTick);
         if (dy != 0.0) {
            this.setPosition(this.position.add(0.0, dy, 0.0));
         }
      }
   }

   @Inject(
      method = {"setup"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/Camera;move(FFF)V",
         ordinal = 0
      )}
   )
   private void devilfruits$liftFormCamera(BlockGetter level, Entity entity, boolean detached, boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {
      float lift = FormMovements.cameraLift(entity);
      if (lift > 0.0F) {
         this.setPosition(this.position.add(0.0, (double)lift, 0.0));
      }

      this.devilfruits$pivot = this.position;
   }
}
