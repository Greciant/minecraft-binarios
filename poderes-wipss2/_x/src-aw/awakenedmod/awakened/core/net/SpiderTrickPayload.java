package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record SpiderTrickPayload(int slot, int trick) implements CustomPacketPayload {
   public static final Type<SpiderTrickPayload> TYPE = new Type(AwId.of("spider_trick"));
   public static final StreamCodec<RegistryFriendlyByteBuf, SpiderTrickPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, SpiderTrickPayload::slot, ByteBufCodecs.VAR_INT, SpiderTrickPayload::trick, SpiderTrickPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
