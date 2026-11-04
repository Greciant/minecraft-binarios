package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record ThunderfallAimPayload(boolean aiming) implements CustomPacketPayload {
   public static final Type<ThunderfallAimPayload> TYPE = new Type(AwId.of("thunderfall_aim"));
   public static final StreamCodec<RegistryFriendlyByteBuf, ThunderfallAimPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.BOOL, ThunderfallAimPayload::aiming, ThunderfallAimPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
