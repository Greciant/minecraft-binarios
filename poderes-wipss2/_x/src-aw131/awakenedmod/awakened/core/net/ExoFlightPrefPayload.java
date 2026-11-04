package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record ExoFlightPrefPayload() implements CustomPacketPayload {
   public static final ExoFlightPrefPayload INSTANCE = new ExoFlightPrefPayload();
   public static final Type<ExoFlightPrefPayload> TYPE = new Type(AwId.of("exo_flight_pref"));
   public static final StreamCodec<RegistryFriendlyByteBuf, ExoFlightPrefPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
