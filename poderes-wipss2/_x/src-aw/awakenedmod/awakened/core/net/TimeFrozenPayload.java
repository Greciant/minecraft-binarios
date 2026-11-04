package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record TimeFrozenPayload(boolean frozen) implements CustomPacketPayload {
   public static final Type<TimeFrozenPayload> TYPE = new Type(AwId.of("time_frozen"));
   public static final StreamCodec<RegistryFriendlyByteBuf, TimeFrozenPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.BOOL, TimeFrozenPayload::frozen, TimeFrozenPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
