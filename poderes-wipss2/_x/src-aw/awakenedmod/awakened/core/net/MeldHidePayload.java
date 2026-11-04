package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record MeldHidePayload(int entityId, boolean hidden) implements CustomPacketPayload {
   public static final Type<MeldHidePayload> TYPE = new Type(AwId.of("meld_hide"));
   public static final StreamCodec<RegistryFriendlyByteBuf, MeldHidePayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, MeldHidePayload::entityId, ByteBufCodecs.BOOL, MeldHidePayload::hidden, MeldHidePayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
