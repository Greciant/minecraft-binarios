package com.devilfruits.fruits.kilo_kilo;

import com.devilfruits.registry.DFParticles;
import java.util.function.Supplier;
import net.minecraft.core.particles.SimpleParticleType;

public final class KiloParticles {
   public static final Supplier<SimpleParticleType> FEATHER = DFParticles.PARTICLES.register("kilo_kilo/feather", () -> new SimpleParticleType(false));
   public static final Supplier<SimpleParticleType> MOTE = DFParticles.PARTICLES.register("kilo_kilo/mote", () -> new SimpleParticleType(false));

   static void init() {
   }

   private KiloParticles() {
   }
}
