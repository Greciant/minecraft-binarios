package com.devilfruits.fruits.suna_suna;

import com.devilfruits.registry.DFParticles;
import java.util.function.Supplier;
import net.minecraft.core.particles.SimpleParticleType;

public final class SunaParticles {
   public static final Supplier<SimpleParticleType> GRAIN = DFParticles.PARTICLES.register("suna_suna/grain", () -> new SimpleParticleType(false));
   public static final Supplier<SimpleParticleType> CLUMP = DFParticles.PARTICLES.register("suna_suna/clump", () -> new SimpleParticleType(false));
   public static final Supplier<SimpleParticleType> DUST = DFParticles.PARTICLES.register("suna_suna/dust", () -> new SimpleParticleType(false));
   public static final Supplier<SimpleParticleType> WET_CLUMP = DFParticles.PARTICLES.register("suna_suna/wet_clump", () -> new SimpleParticleType(false));
   public static final Supplier<SimpleParticleType> DRY_FLAKE = DFParticles.PARTICLES.register("suna_suna/dry_flake", () -> new SimpleParticleType(false));
   public static final Supplier<SimpleParticleType> DEHYDRATED = DFParticles.PARTICLES.register("suna_suna/dehydrated", () -> new SimpleParticleType(false));

   static void init() {
   }

   private SunaParticles() {
   }
}
