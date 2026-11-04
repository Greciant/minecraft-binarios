package com.devilfruits.registry;

import com.mojang.serialization.MapCodec;
import java.util.function.Supplier;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class DFParticles {
   public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, "devilfruits");
   public static final Supplier<SimpleParticleType> DUST_PUFF = PARTICLES.register("dust_puff", () -> new SimpleParticleType(false));
   public static final Supplier<SimpleParticleType> EXPLOSION_FIRE = PARTICLES.register("explosion/fire", () -> new SimpleParticleType(true));
   public static final Supplier<SimpleParticleType> EXPLOSION_SMOKE = PARTICLES.register("explosion/smoke", () -> new SimpleParticleType(true));
   public static final Supplier<SimpleParticleType> HAKI_SHARD = PARTICLES.register("haki_shard", () -> new SimpleParticleType(false));
   public static final Supplier<SimpleParticleType> HAKI_EMBER = PARTICLES.register("haki_ember", () -> new SimpleParticleType(false));
   public static final Supplier<SimpleParticleType> HAKI_SPARK = PARTICLES.register("haki_spark", () -> new SimpleParticleType(false));
   public static final Supplier<ParticleType<BlockParticleOption>> BLOCK_DEBRIS = PARTICLES.register(
      "block_debris", () -> new ParticleType<BlockParticleOption>(false) {
            public MapCodec<BlockParticleOption> codec() {
               return BlockParticleOption.codec(this);
            }

            public StreamCodec<? super RegistryFriendlyByteBuf, BlockParticleOption> streamCodec() {
               return BlockParticleOption.streamCodec(this);
            }
         }
   );

   private DFParticles() {
   }
}
