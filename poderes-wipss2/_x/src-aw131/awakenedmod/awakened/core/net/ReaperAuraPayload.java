package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record ReaperAuraPayload(int entityId, int tier, float shownPct) implements CustomPacketPayload {
   public static final Type<ReaperAuraPayload> TYPE = new Type(AwId.of("reaper_aura"));
   public static final StreamCodec<RegistryFriendlyByteBuf, ReaperAuraPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      ReaperAuraPayload::entityId,
      ByteBufCodecs.VAR_INT,
      ReaperAuraPayload::tier,
      ByteBufCodecs.FLOAT,
      ReaperAuraPayload::shownPct,
      ReaperAuraPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
