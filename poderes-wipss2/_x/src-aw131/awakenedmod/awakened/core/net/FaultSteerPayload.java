package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record FaultSteerPayload(boolean live) implements CustomPacketPayload {
   public static final Type<FaultSteerPayload> TYPE = new Type(AwId.of("fault_steer"));
   public static final StreamCodec<RegistryFriendlyByteBuf, FaultSteerPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.BOOL, FaultSteerPayload::live, FaultSteerPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
