package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record AeroFlightSyncPayload(boolean armed, boolean flying, boolean gliding, boolean forcedGliding) implements CustomPacketPayload {
   public static final Type<AeroFlightSyncPayload> TYPE = new Type(AwId.of("aero_flight_sync"));
   public static final StreamCodec<RegistryFriendlyByteBuf, AeroFlightSyncPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.BOOL,
      AeroFlightSyncPayload::armed,
      ByteBufCodecs.BOOL,
      AeroFlightSyncPayload::flying,
      ByteBufCodecs.BOOL,
      AeroFlightSyncPayload::gliding,
      ByteBufCodecs.BOOL,
      AeroFlightSyncPayload::forcedGliding,
      AeroFlightSyncPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
