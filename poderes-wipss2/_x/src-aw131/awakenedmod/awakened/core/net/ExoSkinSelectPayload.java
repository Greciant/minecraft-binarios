package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record ExoSkinSelectPayload(String id) implements CustomPacketPayload {
   public static final Type<ExoSkinSelectPayload> TYPE = new Type(AwId.of("exo_skin_select"));
   public static final StreamCodec<RegistryFriendlyByteBuf, ExoSkinSelectPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.stringUtf8(64), ExoSkinSelectPayload::id, ExoSkinSelectPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
