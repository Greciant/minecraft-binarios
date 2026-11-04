package com.awakenedmod.awakened.client.render;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class SteeredParticle {
   private SteeredParticle() {
   }

   @Nullable
   public static Particle spawn(ParticleOptions options, Vec3 at, Vec3 v) {
      Particle p = Minecraft.getInstance().particleEngine.createParticle(options, at.x, at.y, at.z, 0.0, 0.0, 0.0);
      if (p != null) {
         p.setParticleSpeed(v.x, v.y, v.z);
      }

      return p;
   }

   public static void block(BlockState state, Vec3 at, Vec3 v) {
      if (state != null && !state.isAir()) {
         spawn(new BlockParticleOption(ParticleTypes.BLOCK, state), at, v);
      }
   }
}
