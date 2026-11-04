package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record OpenInfusionScreenPayload(BlockPos pos, float chargeFraction, int tier, boolean ready) implements CustomPacketPayload {
   public static final Type<OpenInfusionScreenPayload> TYPE = new Type(AwId.of("open_infusion_screen"));
   public static final StreamCodec<RegistryFriendlyByteBuf, OpenInfusionScreenPayload> STREAM_CODEC = StreamCodec.composite(
      BlockPos.STREAM_CODEC,
      OpenInfusionScreenPayload::pos,
      ByteBufCodecs.FLOAT,
      OpenInfusionScreenPayload::chargeFraction,
      ByteBufCodecs.VAR_INT,
      OpenInfusionScreenPayload::tier,
      ByteBufCodecs.BOOL,
      OpenInfusionScreenPayload::ready,
      OpenInfusionScreenPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
