package com.devilfruits.fruits.kage_kage.shadow;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public record StolenShadow(UUID victim, ResourceLocation type, float maxHealth, float attack, String name) {
   public static final Codec<StolenShadow> CODEC = RecordCodecBuilder.create(
      i -> i.group(
               UUIDUtil.CODEC.fieldOf("victim").forGetter(StolenShadow::victim),
               ResourceLocation.CODEC.fieldOf("type").forGetter(StolenShadow::type),
               Codec.FLOAT.fieldOf("health").forGetter(StolenShadow::maxHealth),
               Codec.FLOAT.fieldOf("attack").forGetter(StolenShadow::attack),
               Codec.STRING.fieldOf("name").forGetter(StolenShadow::name)
            )
            .apply(i, StolenShadow::new)
   );
   public static final StreamCodec<ByteBuf, StolenShadow> STREAM_CODEC = StreamCodec.composite(
      UUIDUtil.STREAM_CODEC,
      StolenShadow::victim,
      ResourceLocation.STREAM_CODEC,
      StolenShadow::type,
      ByteBufCodecs.FLOAT,
      StolenShadow::maxHealth,
      ByteBufCodecs.FLOAT,
      StolenShadow::attack,
      ByteBufCodecs.STRING_UTF8,
      StolenShadow::name,
      StolenShadow::new
   );

   public float strength() {
      return this.maxHealth + this.attack * 5.0F;
   }
}
