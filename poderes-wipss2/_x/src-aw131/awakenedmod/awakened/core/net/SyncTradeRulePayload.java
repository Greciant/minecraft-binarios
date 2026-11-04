package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record SyncTradeRulePayload(boolean allowed) implements CustomPacketPayload {
   public static final Type<SyncTradeRulePayload> TYPE = new Type(AwId.of("sync_trade_rule"));
   public static final StreamCodec<RegistryFriendlyByteBuf, SyncTradeRulePayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.BOOL, SyncTradeRulePayload::allowed, SyncTradeRulePayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
