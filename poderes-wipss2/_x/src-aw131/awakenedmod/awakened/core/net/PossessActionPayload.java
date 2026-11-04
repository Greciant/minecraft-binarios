package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record PossessActionPayload(int action) implements CustomPacketPayload {
   public static final int ATTACK = 0;
   public static final int EXIT = 1;
   public static final int CYCLE = 2;
   public static final Type<PossessActionPayload> TYPE = new Type(AwId.of("possess_action"));
   public static final StreamCodec<RegistryFriendlyByteBuf, PossessActionPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, PossessActionPayload::action, PossessActionPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
