package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record ExoFailsafePayload() implements CustomPacketPayload {
   public static final ExoFailsafePayload INSTANCE = new ExoFailsafePayload();
   public static final Type<ExoFailsafePayload> TYPE = new Type(AwId.of("exo_failsafe"));
   public static final StreamCodec<RegistryFriendlyByteBuf, ExoFailsafePayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
