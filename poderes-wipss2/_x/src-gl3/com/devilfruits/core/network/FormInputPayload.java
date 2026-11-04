package com.devilfruits.core.network;

import com.devilfruits.DevilFruits;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record FormInputPayload(FormInputPayload.Action action, String form) implements CustomPacketPayload {
   public static final Type<FormInputPayload> TYPE = new Type(DevilFruits.id("form_input"));
   public static final StreamCodec<FriendlyByteBuf, FormInputPayload> STREAM_CODEC = CustomPacketPayload.codec((payload, buf) -> {
      buf.writeEnum(payload.action);
      buf.writeUtf(payload.form, 64);
   }, buf -> new FormInputPayload((FormInputPayload.Action)buf.readEnum(FormInputPayload.Action.class), buf.readUtf(64)));

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static enum Action {
      CYCLE,
      REVERT,
      SET,
      BOOST_START,
      BOOST_STOP;
   }
}
