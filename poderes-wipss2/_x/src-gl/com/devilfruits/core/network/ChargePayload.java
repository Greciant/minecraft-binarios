package com.devilfruits.core.network;

import com.devilfruits.DevilFruits;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;

public record ChargePayload(int entityId, ResourceLocation ability, boolean start, float duration, long seed, boolean released) implements CustomPacketPayload {
   public static final Type<ChargePayload> TYPE = new Type(DevilFruits.id("charge"));
   public static final StreamCodec<FriendlyByteBuf, ChargePayload> STREAM_CODEC = CustomPacketPayload.codec((p, buf) -> {
      buf.writeVarInt(p.entityId);
      buf.writeResourceLocation(p.ability);
      buf.writeBoolean(p.start);
      buf.writeFloat(p.duration);
      buf.writeLong(p.seed);
      buf.writeBoolean(p.released);
   }, buf -> new ChargePayload(buf.readVarInt(), buf.readResourceLocation(), buf.readBoolean(), buf.readFloat(), buf.readLong(), buf.readBoolean()));

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
