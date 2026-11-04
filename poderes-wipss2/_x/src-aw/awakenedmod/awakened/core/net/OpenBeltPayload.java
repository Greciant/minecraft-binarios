package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record OpenBeltPayload() implements CustomPacketPayload {
   public static final OpenBeltPayload INSTANCE = new OpenBeltPayload();
   public static final Type<OpenBeltPayload> TYPE = new Type(AwId.of("open_belt"));
   public static final StreamCodec<RegistryFriendlyByteBuf, OpenBeltPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
