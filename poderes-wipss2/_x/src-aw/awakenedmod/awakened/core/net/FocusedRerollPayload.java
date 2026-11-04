package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record FocusedRerollPayload(boolean offHand, List<String> powerIds) implements CustomPacketPayload {
   public static final Type<FocusedRerollPayload> TYPE = new Type(AwId.of("focused_reroll"));
   public static final StreamCodec<RegistryFriendlyByteBuf, FocusedRerollPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.BOOL,
      FocusedRerollPayload::offHand,
      ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
      FocusedRerollPayload::powerIds,
      FocusedRerollPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
