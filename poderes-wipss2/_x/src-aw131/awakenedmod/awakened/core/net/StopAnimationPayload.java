package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record StopAnimationPayload(int entityId) implements CustomPacketPayload {
   public static final Type<StopAnimationPayload> TYPE = new Type(AwId.of("stop_animation"));
   public static final StreamCodec<RegistryFriendlyByteBuf, StopAnimationPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, StopAnimationPayload::entityId, StopAnimationPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
