package com.devilfruits.fruits.gura_gura;

import com.devilfruits.registry.DFParticles;
import java.util.function.Supplier;
import net.minecraft.core.particles.SimpleParticleType;

public final class GuraParticles {
   public static final Supplier<SimpleParticleType> GLASS_SHARD = DFParticles.PARTICLES.register("gura_gura/glass_shard", () -> new SimpleParticleType(false));
   public static final Supplier<SimpleParticleType> CRACK_SPARK = DFParticles.PARTICLES.register("gura_gura/crack_spark", () -> new SimpleParticleType(false));
   public static final Supplier<SimpleParticleType> WATER_SPRAY = DFParticles.PARTICLES.register("gura_gura/water_spray", () -> new SimpleParticleType(false));
   public static final Supplier<SimpleParticleType> IMPACT_FLASH = DFParticles.PARTICLES.register("gura_gura/impact_flash", () -> new SimpleParticleType(true));

   static void init() {
   }

   private GuraParticles() {
   }
}
