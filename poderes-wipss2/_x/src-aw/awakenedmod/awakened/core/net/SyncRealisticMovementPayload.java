package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record SyncRealisticMovementPayload(boolean enabled) implements CustomPacketPayload {
   public static final Type<SyncRealisticMovementPayload> TYPE = new Type(AwId.of("sync_realistic_movement"));
   public static final StreamCodec<RegistryFriendlyByteBuf, SyncRealisticMovementPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.BOOL, SyncRealisticMovementPayload::enabled, SyncRealisticMovementPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
