package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record ExoBoostPayload() implements CustomPacketPayload {
   public static final ExoBoostPayload INSTANCE = new ExoBoostPayload();
   public static final Type<ExoBoostPayload> TYPE = new Type(AwId.of("exo_boost"));
   public static final StreamCodec<RegistryFriendlyByteBuf, ExoBoostPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
