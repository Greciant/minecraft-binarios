package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record TradeScreenPayload(
   int stage,
   UUID partner,
   String partnerName,
   String givePower,
   String getPower,
   int giveMastery,
   int getMastery,
   boolean youReady,
   boolean partnerReady,
   int armTicks
) implements CustomPacketPayload {
   public static final Type<TradeScreenPayload> TYPE = new Type(AwId.of("trade_screen"));
   public static final int STAGE_PROPOSE = 0;
   public static final int STAGE_REVIEW = 1;
   public static final int STAGE_FINAL = 2;
   public static final int STAGE_CLOSED = 3;
   public static final StreamCodec<RegistryFriendlyByteBuf, TradeScreenPayload> STREAM_CODEC = StreamCodec.of(
      TradeScreenPayload::write, TradeScreenPayload::read
   );

   public static TradeScreenPayload closed() {
      return new TradeScreenPayload(3, new UUID(0L, 0L), "", "", "", 0, 0, false, false, 0);
   }

   private static void write(RegistryFriendlyByteBuf buf, TradeScreenPayload p) {
      buf.writeVarInt(p.stage);
      buf.writeUUID(p.partner);
      buf.writeUtf(p.partnerName);
      buf.writeUtf(p.givePower);
      buf.writeUtf(p.getPower);
      buf.writeVarInt(p.giveMastery);
      buf.writeVarInt(p.getMastery);
      buf.writeBoolean(p.youReady);
      buf.writeBoolean(p.partnerReady);
      buf.writeVarInt(p.armTicks);
   }

   private static TradeScreenPayload read(RegistryFriendlyByteBuf buf) {
      return new TradeScreenPayload(
         buf.readVarInt(),
         buf.readUUID(),
         buf.readUtf(),
         buf.readUtf(),
         buf.readUtf(),
         buf.readVarInt(),
         buf.readVarInt(),
         buf.readBoolean(),
         buf.readBoolean(),
         buf.readVarInt()
      );
   }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
