package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record ReaperToggleAuraPayload() implements CustomPacketPayload {
   public static final ReaperToggleAuraPayload INSTANCE = new ReaperToggleAuraPayload();
   public static final Type<ReaperToggleAuraPayload> TYPE = new Type(AwId.of("reaper_toggle_aura"));
   public static final StreamCodec<RegistryFriendlyByteBuf, ReaperToggleAuraPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
