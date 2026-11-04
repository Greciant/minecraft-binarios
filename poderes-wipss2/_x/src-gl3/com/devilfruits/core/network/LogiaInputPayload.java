package com.devilfruits.core.network;

import com.devilfruits.DevilFruits;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record LogiaInputPayload(LogiaInputPayload.Action action) implements CustomPacketPayload {
   public static final Type<LogiaInputPayload> TYPE = new Type(DevilFruits.id("logia_input"));
   public static final StreamCodec<FriendlyByteBuf, LogiaInputPayload> STREAM_CODEC = CustomPacketPayload.codec(
      (p, buf) -> buf.writeEnum(p.action), buf -> new LogiaInputPayload((LogiaInputPayload.Action)buf.readEnum(LogiaInputPayload.Action.class))
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static enum Action {
      ELEMENTAL_ON,
      ELEMENTAL_OFF,
      ELEMENTAL_TOGGLE,
      DASH,
      INTANGIBILITY_TOGGLE;
   }
}
