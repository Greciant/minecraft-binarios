package com.devilfruits.core.network;

import com.devilfruits.DevilFruits;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record HakiInputPayload(HakiInputPayload.Action action, int mask) implements CustomPacketPayload {
   public static final Type<HakiInputPayload> TYPE = new Type(DevilFruits.id("haki_input"));
   public static final StreamCodec<ByteBuf, HakiInputPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT.map(i -> HakiInputPayload.Action.values()[Math.floorMod(i, HakiInputPayload.Action.values().length)], Enum::ordinal),
      HakiInputPayload::action,
      ByteBufCodecs.VAR_INT,
      HakiInputPayload::mask,
      HakiInputPayload::new
   );

   public static HakiInputPayload of(HakiInputPayload.Action action) {
      return new HakiInputPayload(action, 0);
   }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static enum Action {
      TOGGLE,
      SELECT,
      RYUO_START,
      RYUO_RELEASE;
   }
}
