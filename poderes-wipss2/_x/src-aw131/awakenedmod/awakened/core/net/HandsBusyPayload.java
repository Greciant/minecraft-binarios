package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record HandsBusyPayload(boolean busy) implements CustomPacketPayload {
   public static final Type<HandsBusyPayload> TYPE = new Type(AwId.of("hands_busy"));
   public static final StreamCodec<RegistryFriendlyByteBuf, HandsBusyPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.BOOL, HandsBusyPayload::busy, HandsBusyPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
