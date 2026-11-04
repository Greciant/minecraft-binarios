package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record VelocityGearPayload(int delta) implements CustomPacketPayload {
   public static final Type<VelocityGearPayload> TYPE = new Type(AwId.of("velocity_gear"));
   public static final StreamCodec<RegistryFriendlyByteBuf, VelocityGearPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, VelocityGearPayload::delta, VelocityGearPayload::new
   );
   public static final VelocityGearPayload UP = new VelocityGearPayload(1);
   public static final VelocityGearPayload DOWN = new VelocityGearPayload(-1);

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
