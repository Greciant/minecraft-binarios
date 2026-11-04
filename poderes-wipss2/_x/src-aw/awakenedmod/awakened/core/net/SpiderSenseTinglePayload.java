package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record SpiderSenseTinglePayload(int bearingDeg, float intensity, boolean directional, int kind, int masteryLevel) implements CustomPacketPayload {
   public static final Type<SpiderSenseTinglePayload> TYPE = new Type(AwId.of("spider_sense_tingle"));
   public static final StreamCodec<RegistryFriendlyByteBuf, SpiderSenseTinglePayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      SpiderSenseTinglePayload::bearingDeg,
      ByteBufCodecs.FLOAT,
      SpiderSenseTinglePayload::intensity,
      ByteBufCodecs.BOOL,
      SpiderSenseTinglePayload::directional,
      ByteBufCodecs.VAR_INT,
      SpiderSenseTinglePayload::kind,
      ByteBufCodecs.VAR_INT,
      SpiderSenseTinglePayload::masteryLevel,
      SpiderSenseTinglePayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
