package com.devilfruits.core.gear;

import com.devilfruits.DevilFruits;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record GearSwitchPayload(String gear, String subGear) implements CustomPacketPayload {
   public static final Type<GearSwitchPayload> TYPE = new Type(DevilFruits.id("gear_switch"));
   public static final StreamCodec<FriendlyByteBuf, GearSwitchPayload> STREAM_CODEC = CustomPacketPayload.codec((payload, buf) -> {
      buf.writeUtf(payload.gear, 64);
      buf.writeUtf(payload.subGear, 64);
   }, buf -> new GearSwitchPayload(buf.readUtf(64), buf.readUtf(64)));

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
