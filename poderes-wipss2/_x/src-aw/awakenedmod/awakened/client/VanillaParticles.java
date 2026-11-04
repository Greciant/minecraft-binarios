package com.awakenedmod.awakened.client;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public final class VanillaParticles {
   private VanillaParticles() {
   }

   public static boolean allowed(Minecraft mc, RandomSource random) {
      ParticleStatus status = (ParticleStatus)mc.options.particles().get();
      return status == ParticleStatus.ALL || status == ParticleStatus.DECREASED && random.nextInt(3) != 0;
   }

   @Nullable
   public static Particle spawn(ParticleOptions options, Vec3 at, Vec3 velocity, RandomSource random) {
      Minecraft mc = Minecraft.getInstance();
      if (!allowed(mc, random)) {
         return null;
      } else {
         Particle p = mc.particleEngine.createParticle(options, at.x, at.y, at.z, velocity.x, velocity.y, velocity.z);
         if (p != null) {
            p.setParticleSpeed(velocity.x, velocity.y, velocity.z);
         }

         return p;
      }
   }
}
