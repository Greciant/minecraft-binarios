package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record SpiderTypePayload(String typeKey, boolean hasGauntlets) implements CustomPacketPayload {
   public static final Type<SpiderTypePayload> TYPE = new Type(AwId.of("spider_type"));
   public static final StreamCodec<RegistryFriendlyByteBuf, SpiderTypePayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.STRING_UTF8, SpiderTypePayload::typeKey, ByteBufCodecs.BOOL, SpiderTypePayload::hasGauntlets, SpiderTypePayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
