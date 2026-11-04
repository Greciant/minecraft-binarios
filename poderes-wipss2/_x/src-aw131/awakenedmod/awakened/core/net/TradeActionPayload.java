package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record TradeActionPayload(int action, UUID partner) implements CustomPacketPayload {
   public static final Type<TradeActionPayload> TYPE = new Type(AwId.of("trade_action"));
   public static final int SEND = 0;
   public static final int ACCEPT = 1;
   public static final int CONFIRM = 2;
   public static final int CANCEL = 3;
   public static final StreamCodec<RegistryFriendlyByteBuf, TradeActionPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, TradeActionPayload::action, UUIDUtil.STREAM_CODEC, TradeActionPayload::partner, TradeActionPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
