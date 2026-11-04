package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record ReaperHideAuraPayload(boolean hide) implements CustomPacketPayload {
   public static final Type<ReaperHideAuraPayload> TYPE = new Type(AwId.of("reaper_hide_aura"));
   public static final StreamCodec<RegistryFriendlyByteBuf, ReaperHideAuraPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.BOOL, ReaperHideAuraPayload::hide, ReaperHideAuraPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
