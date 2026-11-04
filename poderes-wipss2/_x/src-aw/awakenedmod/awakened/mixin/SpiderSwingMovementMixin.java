package com.awakenedmod.awakened.mixin;

import com.awakenedmod.awakened.client.SpiderSwingController;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LivingEntity.class})
public abstract class SpiderSwingMovementMixin {
   @Inject(
      method = {"travel(Lnet/minecraft/world/phys/Vec3;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void awakened$spiderSwingTravel(Vec3 travelVector, CallbackInfo ci) {
      if (this == Minecraft.getInstance().player && SpiderSwingController.active()) {
         SpiderSwingController.tickMovement((Player)this);
         ci.cancel();
      }
   }
}
