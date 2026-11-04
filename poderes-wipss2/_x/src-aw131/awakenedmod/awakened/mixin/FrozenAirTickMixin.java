package com.awakenedmod.awakened.mixin;

import com.awakenedmod.awakened.client.FrozenAir;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({ParticleEngine.class})
public abstract class FrozenAirTickMixin {
   @Redirect(
      method = {"tickParticle"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/particle/Particle;tick()V"
      )
   )
   private void awakened$slowTick(Particle particle) {
      if (!FrozenAir.beginTick(particle)) {
         particle.tick();
         FrozenAir.endTick(particle);
      }
   }
}
