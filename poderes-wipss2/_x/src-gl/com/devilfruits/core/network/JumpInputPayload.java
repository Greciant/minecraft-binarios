package com.devilfruits.core.network;

import com.devilfruits.DevilFruits;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record JumpInputPayload(JumpInputPayload.Action action, float dx, float dz) implements CustomPacketPayload {
   public static final Type<JumpInputPayload> TYPE = new Type(DevilFruits.id("jump_input"));
   public static final StreamCodec<FriendlyByteBuf, JumpInputPayload> STREAM_CODEC = CustomPacketPayload.codec((p, buf) -> {
      buf.writeEnum(p.action);
      buf.writeFloat(p.dx);
      buf.writeFloat(p.dz);
   }, buf -> new JumpInputPayload((JumpInputPayload.Action)buf.readEnum(JumpInputPayload.Action.class), buf.readFloat(), buf.readFloat()));

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static enum Action {
      START,
      RELEASE,
      CANCEL;
   }
}
