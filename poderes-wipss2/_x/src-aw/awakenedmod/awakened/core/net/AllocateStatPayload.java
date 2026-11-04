package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record AllocateStatPayload(int statId, int delta) implements CustomPacketPayload {
   public static final Type<AllocateStatPayload> TYPE = new Type(AwId.of("allocate_stat"));
   public static final int RESPEC = -1;
   public static final StreamCodec<RegistryFriendlyByteBuf, AllocateStatPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.INT, AllocateStatPayload::statId, ByteBufCodecs.INT, AllocateStatPayload::delta, AllocateStatPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
