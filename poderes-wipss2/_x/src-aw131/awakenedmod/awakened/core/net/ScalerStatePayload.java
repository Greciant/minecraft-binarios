package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record ScalerStatePayload(boolean rayShifted, boolean seized, boolean seizing) implements CustomPacketPayload {
   public static final Type<ScalerStatePayload> TYPE = new Type(AwId.of("scaler_state"));
   public static final StreamCodec<RegistryFriendlyByteBuf, ScalerStatePayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.BOOL,
      ScalerStatePayload::rayShifted,
      ByteBufCodecs.BOOL,
      ScalerStatePayload::seized,
      ByteBufCodecs.BOOL,
      ScalerStatePayload::seizing,
      ScalerStatePayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
