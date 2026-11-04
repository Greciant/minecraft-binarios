package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record BeginInfusionPayload(BlockPos pos) implements CustomPacketPayload {
   public static final Type<BeginInfusionPayload> TYPE = new Type(AwId.of("begin_infusion"));
   public static final StreamCodec<RegistryFriendlyByteBuf, BeginInfusionPayload> STREAM_CODEC = StreamCodec.composite(
      BlockPos.STREAM_CODEC, BeginInfusionPayload::pos, BeginInfusionPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
