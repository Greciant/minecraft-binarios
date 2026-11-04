package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record ReaperDriftPayload(boolean drifting) implements CustomPacketPayload {
   public static final Type<ReaperDriftPayload> TYPE = new Type(AwId.of("reaper_drift"));
   public static final StreamCodec<RegistryFriendlyByteBuf, ReaperDriftPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.BOOL, ReaperDriftPayload::drifting, ReaperDriftPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
