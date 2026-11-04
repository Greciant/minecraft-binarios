package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.MoveKeys;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record MoveKeysPayload(MoveKeys.Keys keys) implements CustomPacketPayload {
   public static final Type<MoveKeysPayload> TYPE = new Type(AwId.of("move_keys"));
   public static final StreamCodec<RegistryFriendlyByteBuf, MoveKeysPayload> STREAM_CODEC = ByteBufCodecs.BYTE
      .cast()
      .map(bits -> new MoveKeysPayload(MoveKeys.Keys.fromBits(bits)), payload -> payload.keys().bits());

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
