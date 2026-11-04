package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record SolarHealthFxPayload() implements CustomPacketPayload {
   public static final Type<SolarHealthFxPayload> TYPE = new Type(AwId.of("solar_health_fx"));
   public static final StreamCodec<RegistryFriendlyByteBuf, SolarHealthFxPayload> STREAM_CODEC = StreamCodec.unit(new SolarHealthFxPayload());

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
