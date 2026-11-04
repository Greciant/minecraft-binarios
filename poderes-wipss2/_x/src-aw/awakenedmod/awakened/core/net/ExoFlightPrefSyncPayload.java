package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record ExoFlightPrefSyncPayload(boolean jet) implements CustomPacketPayload {
   public static final Type<ExoFlightPrefSyncPayload> TYPE = new Type(AwId.of("exo_flight_pref_sync"));
   public static final StreamCodec<RegistryFriendlyByteBuf, ExoFlightPrefSyncPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.BOOL, ExoFlightPrefSyncPayload::jet, ExoFlightPrefSyncPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
