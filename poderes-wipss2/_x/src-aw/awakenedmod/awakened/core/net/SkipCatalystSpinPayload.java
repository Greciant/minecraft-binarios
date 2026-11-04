package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record SkipCatalystSpinPayload() implements CustomPacketPayload {
   public static final Type<SkipCatalystSpinPayload> TYPE = new Type(AwId.of("skip_catalyst_spin"));
   public static final SkipCatalystSpinPayload INSTANCE = new SkipCatalystSpinPayload();
   public static final StreamCodec<RegistryFriendlyByteBuf, SkipCatalystSpinPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
