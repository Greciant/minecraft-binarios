package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record PossessTeleportPayload(int entityId) implements CustomPacketPayload {
   public static final Type<PossessTeleportPayload> TYPE = new Type(AwId.of("possess_teleport"));
   public static final StreamCodec<RegistryFriendlyByteBuf, PossessTeleportPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, PossessTeleportPayload::entityId, PossessTeleportPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
