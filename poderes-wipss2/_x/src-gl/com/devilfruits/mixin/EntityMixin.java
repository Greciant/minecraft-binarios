package com.devilfruits.mixin;

import com.devilfruits.core.form.FormCollision;
import com.devilfruits.core.physics.EntityMass;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.PushReaction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Entity.class})
public abstract class EntityMixin {
   @Inject(
      method = {"fireImmune"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void devilfruits$formFireImmune(CallbackInfoReturnable<Boolean> cir) {
      if (FormCollision.fireImmune((Entity)this)) {
         cir.setReturnValue(true);
      }
   }

   @Inject(
      method = {"getPistonPushReaction"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void devilfruits$immovableBody(CallbackInfoReturnable<PushReaction> cir) {
      if (EntityMass.immovable((Entity)this)) {
         cir.setReturnValue(PushReaction.IGNORE);
      }
   }
}
