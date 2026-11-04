package com.awakenedmod.awakened.content;

import com.awakenedmod.awakened.content.particle.NukeFxOptions;
import com.mojang.serialization.MapCodec;
import java.util.function.Supplier;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class AwakenedParticles {
   private static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, "awakened");
   public static final Supplier<ParticleType<NukeFxOptions>> FLARE = scaled("nuclear_flare");
   public static final Supplier<ParticleType<NukeFxOptions>> BURST = scaled("nuclear_burst");
   public static final Supplier<ParticleType<NukeFxOptions>> BEAM = scaled("nuclear_beam");
   public static final Supplier<ParticleType<NukeFxOptions>> SHOCK = scaled("nuclear_shock");
   public static final Supplier<ParticleType<NukeFxOptions>> EMBER = scaled("nuclear_ember");
   public static final Supplier<ParticleType<NukeFxOptions>> SMOKE = scaled("nuclear_smoke");
   public static final Supplier<ParticleType<NukeFxOptions>> SPARK = scaled("nuclear_spark");
   public static final Supplier<ParticleType<NukeFxOptions>> FALLOUT = scaled("nuclear_fallout");
   public static final Supplier<ParticleType<NukeFxOptions>> WARP_MIST = scaled("warp_mist");
   public static final Supplier<SimpleParticleType> SUN_MOTE = PARTICLES.register("sun_mote", () -> new SimpleParticleType(false));

   private AwakenedParticles() {
   }

   public static void register(IEventBus modEventBus) {
      PARTICLES.register(modEventBus);
   }

   private static Supplier<ParticleType<NukeFxOptions>> scaled(String name) {
      return PARTICLES.register(name, () -> new ParticleType<NukeFxOptions>(false) {
            public MapCodec<NukeFxOptions> codec() {
               return NukeFxOptions.codec(this);
            }

            public StreamCodec<? super RegistryFriendlyByteBuf, NukeFxOptions> streamCodec() {
               return NukeFxOptions.streamCodec(this);
            }
         });
   }
}
