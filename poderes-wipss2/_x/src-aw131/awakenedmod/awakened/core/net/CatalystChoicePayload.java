package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record CatalystChoicePayload(boolean take) implements CustomPacketPayload {
   public static final Type<CatalystChoicePayload> TYPE = new Type(AwId.of("catalyst_choice"));
   public static final StreamCodec<RegistryFriendlyByteBuf, CatalystChoicePayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.BOOL, CatalystChoicePayload::take, CatalystChoicePayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
