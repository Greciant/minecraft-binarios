package com.devilfruits.core.network;

import com.devilfruits.DevilFruits;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.world.phys.Vec3;

public record AimPayload(boolean valid, Vec3 point) implements CustomPacketPayload {
   public static final Type<AimPayload> TYPE = new Type(DevilFruits.id("aim"));
   public static final StreamCodec<FriendlyByteBuf, AimPayload> STREAM_CODEC = CustomPacketPayload.codec((p, buf) -> {
      buf.writeBoolean(p.valid);
      if (p.valid) {
         buf.writeDouble(p.point.x);
         buf.writeDouble(p.point.y);
         buf.writeDouble(p.point.z);
      }
   }, buf -> buf.readBoolean() ? new AimPayload(true, new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble())) : none());

   public static AimPayload none() {
      return new AimPayload(false, Vec3.ZERO);
   }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
