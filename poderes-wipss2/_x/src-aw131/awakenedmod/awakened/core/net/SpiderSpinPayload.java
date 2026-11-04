package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record SpiderSpinPayload(int ticks, float yawPerTick) implements CustomPacketPayload {
   public static final Type<SpiderSpinPayload> TYPE = new Type(AwId.of("spider_spin"));
   public static final StreamCodec<RegistryFriendlyByteBuf, SpiderSpinPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, SpiderSpinPayload::ticks, ByteBufCodecs.FLOAT, SpiderSpinPayload::yawPerTick, SpiderSpinPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
