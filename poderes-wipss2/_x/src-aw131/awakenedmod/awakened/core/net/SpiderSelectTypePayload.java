package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record SpiderSelectTypePayload(String typeKey) implements CustomPacketPayload {
   public static final Type<SpiderSelectTypePayload> TYPE = new Type(AwId.of("spider_select_type"));
   public static final StreamCodec<RegistryFriendlyByteBuf, SpiderSelectTypePayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.STRING_UTF8, SpiderSelectTypePayload::typeKey, SpiderSelectTypePayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
