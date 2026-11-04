package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record StoneMechInputPayload(float yaw, float pitch, float forward, float strafe, boolean jump, boolean sprint) implements CustomPacketPayload {
   public static final Type<StoneMechInputPayload> TYPE = new Type(AwId.of("stone_mech_input"));
   public static final StreamCodec<RegistryFriendlyByteBuf, StoneMechInputPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.FLOAT,
      StoneMechInputPayload::yaw,
      ByteBufCodecs.FLOAT,
      StoneMechInputPayload::pitch,
      ByteBufCodecs.FLOAT,
      StoneMechInputPayload::forward,
      ByteBufCodecs.FLOAT,
      StoneMechInputPayload::strafe,
      ByteBufCodecs.BOOL,
      StoneMechInputPayload::jump,
      ByteBufCodecs.BOOL,
      StoneMechInputPayload::sprint,
      StoneMechInputPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
