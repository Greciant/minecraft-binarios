package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record OpenCatalystSelectPayload(int picks, int xpCost, boolean offHand, List<String> locked, List<String> unavailable) implements CustomPacketPayload {
   public static final Type<OpenCatalystSelectPayload> TYPE = new Type(AwId.of("catalyst_select"));
   public static final StreamCodec<RegistryFriendlyByteBuf, OpenCatalystSelectPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      OpenCatalystSelectPayload::picks,
      ByteBufCodecs.VAR_INT,
      OpenCatalystSelectPayload::xpCost,
      ByteBufCodecs.BOOL,
      OpenCatalystSelectPayload::offHand,
      ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
      OpenCatalystSelectPayload::locked,
      ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
      OpenCatalystSelectPayload::unavailable,
      OpenCatalystSelectPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
