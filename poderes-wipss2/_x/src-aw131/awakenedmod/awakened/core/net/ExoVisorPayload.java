package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record ExoVisorPayload() implements CustomPacketPayload {
   public static final ExoVisorPayload INSTANCE = new ExoVisorPayload();
   public static final Type<ExoVisorPayload> TYPE = new Type(AwId.of("exo_visor"));
   public static final StreamCodec<RegistryFriendlyByteBuf, ExoVisorPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
