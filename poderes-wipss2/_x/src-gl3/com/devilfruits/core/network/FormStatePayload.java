package com.devilfruits.core.network;

import com.devilfruits.DevilFruits;
import java.util.Optional;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;

public record FormStatePayload(int entityId, Optional<ResourceLocation> fruit, String form, int flags) implements CustomPacketPayload {
   public static final Type<FormStatePayload> TYPE = new Type(DevilFruits.id("form_state"));
   public static final StreamCodec<FriendlyByteBuf, FormStatePayload> STREAM_CODEC = CustomPacketPayload.codec((p, buf) -> {
      buf.writeVarInt(p.entityId);
      buf.writeOptional(p.fruit, FriendlyByteBuf::writeResourceLocation);
      buf.writeUtf(p.form, 64);
      buf.writeVarInt(p.flags);
   }, buf -> new FormStatePayload(buf.readVarInt(), buf.readOptional(FriendlyByteBuf::readResourceLocation), buf.readUtf(64), buf.readVarInt()));

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
