package com.devilfruits.mixin;

import com.devilfruits.fruits.suke_suke.clear.Clear;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({LivingEntity.class})
public abstract class ClearLivingEntityMixin {
   private static final String ADD_PARTICLE = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V";

   @WrapWithCondition(
      method = {"tickEffects"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"
      )}
   )
   private boolean devilfruits$clearSwirls(Level level, ParticleOptions particle, double x, double y, double z, double dx, double dy, double dz) {
      return !Clear.hiddenOnClient((LivingEntity)this);
   }

   @WrapWithCondition(
      method = {"spawnItemParticles"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"
      )}
   )
   private boolean devilfruits$clearCrumbs(Level level, ParticleOptions particle, double x, double y, double z, double dx, double dy, double dz) {
      LivingEntity self = (LivingEntity)this;
      return !level.isClientSide() || !Clear.hiddenOnClient(self) && (!Clear.hiddenItems(self) || !Clear.itemsHiddenHere.test(self));
   }
}
