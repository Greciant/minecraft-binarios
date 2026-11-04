package com.devilfruits.fruits.uo_uo_seiryu;

import com.devilfruits.registry.DFParticles;
import java.util.function.Supplier;
import net.minecraft.core.particles.SimpleParticleType;

public final class SeiryuParticles {
   public static final Supplier<SimpleParticleType> FLAME = DFParticles.PARTICLES.register("uo_uo_seiryu/flame", () -> new SimpleParticleType(false));
   public static final Supplier<SimpleParticleType> EMBER = DFParticles.PARTICLES.register("uo_uo_seiryu/ember", () -> new SimpleParticleType(false));
   public static final Supplier<SimpleParticleType> CLOUD = DFParticles.PARTICLES.register("uo_uo_seiryu/cloud", () -> new SimpleParticleType(true));
   public static final Supplier<SimpleParticleType> STORM_CLOUD = DFParticles.PARTICLES
      .register("uo_uo_seiryu/storm_cloud", () -> new SimpleParticleType(true));
   public static final Supplier<SimpleParticleType> SCALE_SHARD = DFParticles.PARTICLES
      .register("uo_uo_seiryu/scale_shard", () -> new SimpleParticleType(false));
   public static final Supplier<SimpleParticleType> GOLD_SPARK = DFParticles.PARTICLES.register("uo_uo_seiryu/gold_spark", () -> new SimpleParticleType(false));
   public static final Supplier<SimpleParticleType> BOLT_SPARK = DFParticles.PARTICLES.register("uo_uo_seiryu/bolt_spark", () -> new SimpleParticleType(false));
   public static final Supplier<SimpleParticleType> VIOLET_SPARK = DFParticles.PARTICLES
      .register("uo_uo_seiryu/violet_spark", () -> new SimpleParticleType(false));

   static void init() {
   }

   private SeiryuParticles() {
   }
}
