package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record PlayAnimationPayload(int entityId, String animation) implements CustomPacketPayload {
   public static final Type<PlayAnimationPayload> TYPE = new Type(AwId.of("play_animation"));
   public static final StreamCodec<RegistryFriendlyByteBuf, PlayAnimationPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, PlayAnimationPayload::entityId, ByteBufCodecs.STRING_UTF8, PlayAnimationPayload::animation, PlayAnimationPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
