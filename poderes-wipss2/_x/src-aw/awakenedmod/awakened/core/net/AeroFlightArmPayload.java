package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record AeroFlightArmPayload() implements CustomPacketPayload {
   public static final AeroFlightArmPayload INSTANCE = new AeroFlightArmPayload();
   public static final Type<AeroFlightArmPayload> TYPE = new Type(AwId.of("aero_flight_arm"));
   public static final StreamCodec<RegistryFriendlyByteBuf, AeroFlightArmPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
