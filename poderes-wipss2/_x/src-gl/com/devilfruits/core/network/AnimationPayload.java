package com.devilfruits.core.network;

import com.devilfruits.DevilFruits;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;

public record AnimationPayload(int entityId, ResourceLocation animation, boolean stop) implements CustomPacketPayload {
   public static final Type<AnimationPayload> TYPE = new Type(DevilFruits.id("animation"));
   public static final StreamCodec<FriendlyByteBuf, AnimationPayload> STREAM_CODEC = CustomPacketPayload.codec((p, buf) -> {
      buf.writeVarInt(p.entityId);
      buf.writeResourceLocation(p.animation);
      buf.writeBoolean(p.stop);
   }, buf -> new AnimationPayload(buf.readVarInt(), buf.readResourceLocation(), buf.readBoolean()));

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
