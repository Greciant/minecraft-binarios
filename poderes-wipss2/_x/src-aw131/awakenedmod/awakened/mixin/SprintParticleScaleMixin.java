package com.awakenedmod.awakened.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Entity.class})
public abstract class SprintParticleScaleMixin {
   private static final float NEUTRAL_BAND = 0.15F;

   @Inject(
      method = {"spawnSprintParticle"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void awakened$scaleSprintParticle(CallbackInfo ci) {
      Entity scale = (Entity)this;
      if (scale instanceof LivingEntity body) {
         float scalex = body.getScale();
         if (!(Math.abs(scalex - 1.0F) < 0.15F)) {
            ci.cancel();
            RandomSource random = body.getRandom();
            if (!(scalex < 1.0F) || !(random.nextFloat() >= scalex)) {
               BlockPos onPos = body.getOnPosLegacy();
               Level lvl = body.level();
               BlockState state = lvl.getBlockState(onPos);
               if (!state.addRunningEffects(lvl, onPos, body)) {
                  if (state.getRenderShape() != RenderShape.INVISIBLE) {
                     Vec3 motion = body.getDeltaMovement();
                     BlockPos at = body.blockPosition();
                     int count = scalex > 1.0F ? Math.round(scalex) : 1;
                     double pop = 1.5 * (scalex > 1.0F ? 1.0 + ((double)scalex - 1.0) * 0.25 : Math.max(0.4, (double)scalex));

                     for (int i = 0; i < count; i++) {
                        double x = body.getX() + (random.nextDouble() - 0.5) * (double)body.getBbWidth();
                        double z = body.getZ() + (random.nextDouble() - 0.5) * (double)body.getBbWidth();
                        if (at.getX() != onPos.getX()) {
                           x = Mth.clamp(x, (double)onPos.getX(), (double)onPos.getX() + 1.0);
                        }

                        if (at.getZ() != onPos.getZ()) {
                           z = Mth.clamp(z, (double)onPos.getZ(), (double)onPos.getZ() + 1.0);
                        }

                        lvl.addParticle(
                           new BlockParticleOption(ParticleTypes.BLOCK, state),
                           x,
                           body.getY() + 0.1 * Math.max(0.3, (double)scalex),
                           z,
                           motion.x * -4.0,
                           pop,
                           motion.z * -4.0
                        );
                     }
                  }
               }
            }
         }
      }
   }
}
