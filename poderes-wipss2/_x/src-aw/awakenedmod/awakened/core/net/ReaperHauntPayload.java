package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record ReaperHauntPayload(int remainingTicks) implements CustomPacketPayload {
   public static final Type<ReaperHauntPayload> TYPE = new Type(AwId.of("reaper_haunt"));
   public static final StreamCodec<RegistryFriendlyByteBuf, ReaperHauntPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, ReaperHauntPayload::remainingTicks, ReaperHauntPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
