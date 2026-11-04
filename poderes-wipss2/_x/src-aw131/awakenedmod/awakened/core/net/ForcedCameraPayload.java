package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record ForcedCameraPayload(int ticks, int orbitEntityId) implements CustomPacketPayload {
   public static final Type<ForcedCameraPayload> TYPE = new Type(AwId.of("forced_camera"));
   public static final StreamCodec<RegistryFriendlyByteBuf, ForcedCameraPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, ForcedCameraPayload::ticks, ByteBufCodecs.VAR_INT, ForcedCameraPayload::orbitEntityId, ForcedCameraPayload::new
   );

   public ForcedCameraPayload(int ticks) {
      this(ticks, -1);
   }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
