package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record GammaRagePayload(int entityId, float rage) implements CustomPacketPayload {
   public static final Type<GammaRagePayload> TYPE = new Type(AwId.of("gamma_rage"));
   public static final StreamCodec<RegistryFriendlyByteBuf, GammaRagePayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, GammaRagePayload::entityId, ByteBufCodecs.FLOAT, GammaRagePayload::rage, GammaRagePayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
