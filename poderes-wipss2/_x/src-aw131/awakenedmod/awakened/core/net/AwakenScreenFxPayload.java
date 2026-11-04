package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record AwakenScreenFxPayload(int color, int duration, boolean shakeOnly) implements CustomPacketPayload {
   public static final Type<AwakenScreenFxPayload> TYPE = new Type(AwId.of("awaken_screen_fx"));
   public static final StreamCodec<RegistryFriendlyByteBuf, AwakenScreenFxPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      AwakenScreenFxPayload::color,
      ByteBufCodecs.VAR_INT,
      AwakenScreenFxPayload::duration,
      ByteBufCodecs.BOOL,
      AwakenScreenFxPayload::shakeOnly,
      AwakenScreenFxPayload::new
   );

   public AwakenScreenFxPayload(int color, int duration) {
      this(color, duration, false);
   }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
