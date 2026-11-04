package com.devilfruits.core.network;

import com.devilfruits.DevilFruits;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public record FxPayload(ResourceLocation fx, int entityId, Vec3 pos, Vec3 dir, float power, long seed, int extra) implements CustomPacketPayload {
   public static final Type<FxPayload> TYPE = new Type(DevilFruits.id("fx"));
   public static final StreamCodec<FriendlyByteBuf, FxPayload> STREAM_CODEC = CustomPacketPayload.codec((p, buf) -> {
      buf.writeResourceLocation(p.fx);
      buf.writeVarInt(p.entityId);
      buf.writeVec3(p.pos);
      buf.writeVec3(p.dir);
      buf.writeFloat(p.power);
      buf.writeLong(p.seed);
      buf.writeVarInt(p.extra);
   }, buf -> new FxPayload(buf.readResourceLocation(), buf.readVarInt(), buf.readVec3(), buf.readVec3(), buf.readFloat(), buf.readLong(), buf.readVarInt()));

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
