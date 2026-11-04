package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record SpiderForceStopPayload() implements CustomPacketPayload {
   public static final SpiderForceStopPayload INSTANCE = new SpiderForceStopPayload();
   public static final Type<SpiderForceStopPayload> TYPE = new Type(AwId.of("spider_force_stop"));
   public static final StreamCodec<RegistryFriendlyByteBuf, SpiderForceStopPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
