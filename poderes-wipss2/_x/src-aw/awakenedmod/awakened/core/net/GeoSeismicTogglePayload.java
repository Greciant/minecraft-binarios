package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record GeoSeismicTogglePayload() implements CustomPacketPayload {
   public static final GeoSeismicTogglePayload INSTANCE = new GeoSeismicTogglePayload();
   public static final Type<GeoSeismicTogglePayload> TYPE = new Type(AwId.of("geo_seismic_toggle"));
   public static final StreamCodec<RegistryFriendlyByteBuf, GeoSeismicTogglePayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
