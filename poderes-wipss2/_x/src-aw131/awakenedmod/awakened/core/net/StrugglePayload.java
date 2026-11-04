package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record StrugglePayload() implements CustomPacketPayload {
   public static final StrugglePayload INSTANCE = new StrugglePayload();
   public static final Type<StrugglePayload> TYPE = new Type(AwId.of("struggle"));
   public static final StreamCodec<RegistryFriendlyByteBuf, StrugglePayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
