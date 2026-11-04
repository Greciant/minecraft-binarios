package com.devilfruits.mixin;

import com.devilfruits.fruits.suke_suke.SukeSuke;
import com.devilfruits.fruits.suke_suke.clear.Clear;
import com.devilfruits.fruits.suke_suke.clear.Steps;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Entity.class})
public abstract class ClearEntityMixin {
   @Inject(
      method = {"isInvisible"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void devilfruits$clear(CallbackInfoReturnable<Boolean> cir) {
      Entity self = (Entity)this;
      if (self != Clear.drawnSolid && Clear.invisible(self)) {
         cir.setReturnValue(true);
      }
   }

   @Inject(
      method = {"isInvisibleTo"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void devilfruits$clearTo(Player viewer, CallbackInfoReturnable<Boolean> cir) {
      Entity self = (Entity)this;
      if (self != Clear.drawnSolid && Clear.invisible(self)) {
         cir.setReturnValue(!Clear.sees(viewer, self, SukeSuke.fruit() != null && SukeSuke.flag("spectatorsSee")));
      }
   }

   @Inject(
      method = {"isCurrentlyGlowing"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void devilfruits$clearGlow(CallbackInfoReturnable<Boolean> cir) {
      if (Clear.hiddenOnClient((Entity)this)) {
         cir.setReturnValue(false);
      }
   }

   @Inject(
      method = {"displayFireAnimation"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void devilfruits$clearFire(CallbackInfoReturnable<Boolean> cir) {
      if (Clear.hiddenOnClient((Entity)this)) {
         cir.setReturnValue(false);
      }
   }

   @Inject(
      method = {"canSpawnSprintParticle"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void devilfruits$clearSprintDust(CallbackInfoReturnable<Boolean> cir) {
      if (Clear.hiddenOnClient((Entity)this)) {
         cir.setReturnValue(false);
      }
   }

   @WrapWithCondition(
      method = {"doWaterSplashEffect"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"
      )}
   )
   private boolean devilfruits$clearSplash(Level level, ParticleOptions particle, double x, double y, double z, double dx, double dy, double dz) {
      return !Clear.hiddenOnClient((Entity)this);
   }

   @Inject(
      method = {"playStepSound"},
      at = {@At("HEAD")}
   )
   private void devilfruits$stepBegin(BlockPos pos, BlockState state, CallbackInfo ci) {
      Steps.begin((Entity)this);
   }

   @Inject(
      method = {"playStepSound"},
      at = {@At("RETURN")}
   )
   private void devilfruits$stepEnd(BlockPos pos, BlockState state, CallbackInfo ci) {
      Steps.end();
   }
}
