package com.devilfruits.core.network;

import com.devilfruits.DevilFruits;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record LogiaStatePayload(int entityId, int state, int limbs, int counters, int mode) implements CustomPacketPayload {
   public static final Type<LogiaStatePayload> TYPE = new Type(DevilFruits.id("logia_state"));
   public static final StreamCodec<FriendlyByteBuf, LogiaStatePayload> STREAM_CODEC = CustomPacketPayload.codec((p, buf) -> {
      buf.writeVarInt(p.entityId);
      buf.writeByte(p.state);
      buf.writeByte(p.limbs);
      buf.writeVarInt(p.counters);
      buf.writeVarInt(p.mode + 1);
   }, buf -> new LogiaStatePayload(buf.readVarInt(), buf.readByte(), buf.readByte(), buf.readVarInt(), buf.readVarInt() - 1));

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
