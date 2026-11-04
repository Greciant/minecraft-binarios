package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record CatalystOfferClosedPayload() implements CustomPacketPayload {
   public static final Type<CatalystOfferClosedPayload> TYPE = new Type(AwId.of("catalyst_offer_closed"));
   public static final CatalystOfferClosedPayload INSTANCE = new CatalystOfferClosedPayload();
   public static final StreamCodec<RegistryFriendlyByteBuf, CatalystOfferClosedPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
