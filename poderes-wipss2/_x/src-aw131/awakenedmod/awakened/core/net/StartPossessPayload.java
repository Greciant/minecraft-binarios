package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record StartPossessPayload(int entityId) implements CustomPacketPayload {
   public static final Type<StartPossessPayload> TYPE = new Type(AwId.of("start_possess"));
   public static final StreamCodec<RegistryFriendlyByteBuf, StartPossessPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, StartPossessPayload::entityId, StartPossessPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
