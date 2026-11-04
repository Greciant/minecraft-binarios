package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record SecondaryKeyPayload(boolean sneaking, boolean held) implements CustomPacketPayload {
   public static final Type<SecondaryKeyPayload> TYPE = new Type(AwId.of("secondary_key"));
   public static final StreamCodec<RegistryFriendlyByteBuf, SecondaryKeyPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.BOOL, SecondaryKeyPayload::sneaking, ByteBufCodecs.BOOL, SecondaryKeyPayload::held, SecondaryKeyPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
