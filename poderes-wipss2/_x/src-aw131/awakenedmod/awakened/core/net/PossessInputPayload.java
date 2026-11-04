package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record PossessInputPayload(float yaw, float pitch, float forward, float strafe, boolean jump, boolean sprint) implements CustomPacketPayload {
   public static final Type<PossessInputPayload> TYPE = new Type(AwId.of("possess_input"));
   public static final StreamCodec<RegistryFriendlyByteBuf, PossessInputPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.FLOAT,
      PossessInputPayload::yaw,
      ByteBufCodecs.FLOAT,
      PossessInputPayload::pitch,
      ByteBufCodecs.FLOAT,
      PossessInputPayload::forward,
      ByteBufCodecs.FLOAT,
      PossessInputPayload::strafe,
      ByteBufCodecs.BOOL,
      PossessInputPayload::jump,
      ByteBufCodecs.BOOL,
      PossessInputPayload::sprint,
      PossessInputPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
