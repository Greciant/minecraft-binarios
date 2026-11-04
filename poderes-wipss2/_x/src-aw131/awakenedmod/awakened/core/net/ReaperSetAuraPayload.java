package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record ReaperSetAuraPayload(float shownPct) implements CustomPacketPayload {
   public static final Type<ReaperSetAuraPayload> TYPE = new Type(AwId.of("reaper_set_aura"));
   public static final StreamCodec<RegistryFriendlyByteBuf, ReaperSetAuraPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.FLOAT, ReaperSetAuraPayload::shownPct, ReaperSetAuraPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
