package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record StartMechPilotPayload(int entityId) implements CustomPacketPayload {
   public static final Type<StartMechPilotPayload> TYPE = new Type(AwId.of("start_mech_pilot"));
   public static final StreamCodec<RegistryFriendlyByteBuf, StartMechPilotPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, StartMechPilotPayload::entityId, StartMechPilotPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
