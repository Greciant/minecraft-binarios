package com.awakenedmod.awakened.content.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

public final class NukeFxOptions implements ParticleOptions {
   public static final float MIN_SCALE = 0.05F;
   public static final float MAX_SCALE = 18.0F;
   private final ParticleType<NukeFxOptions> type;
   public final float scale;

   public NukeFxOptions(ParticleType<NukeFxOptions> type, float scale) {
      this.type = type;
      this.scale = Mth.clamp(scale, 0.05F, 18.0F);
   }

   public ParticleType<?> getType() {
      return this.type;
   }

   public static MapCodec<NukeFxOptions> codec(ParticleType<NukeFxOptions> type) {
      return RecordCodecBuilder.mapCodec(i -> i.group(Codec.FLOAT.fieldOf("scale").forGetter(o -> o.scale)).apply(i, scale -> new NukeFxOptions(type, scale)));
   }

   public static StreamCodec<RegistryFriendlyByteBuf, NukeFxOptions> streamCodec(ParticleType<NukeFxOptions> type) {
      return StreamCodec.composite(ByteBufCodecs.FLOAT, o -> o.scale, scale -> new NukeFxOptions(type, scale));
   }
}
