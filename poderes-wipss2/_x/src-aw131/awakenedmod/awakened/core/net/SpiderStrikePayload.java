package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record SpiderStrikePayload(int slot, int action, int targetId, double speed) implements CustomPacketPayload {
   public static final int START = 0;
   public static final int HIT = 1;
   public static final Type<SpiderStrikePayload> TYPE = new Type(AwId.of("spider_strike"));
   public static final StreamCodec<RegistryFriendlyByteBuf, SpiderStrikePayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      SpiderStrikePayload::slot,
      ByteBufCodecs.VAR_INT,
      SpiderStrikePayload::action,
      ByteBufCodecs.VAR_INT,
      SpiderStrikePayload::targetId,
      ByteBufCodecs.DOUBLE,
      SpiderStrikePayload::speed,
      SpiderStrikePayload::new
   );

   public static SpiderStrikePayload start(int slot) {
      return new SpiderStrikePayload(slot, 0, 0, 0.0);
   }

   public static SpiderStrikePayload hit(int slot, int targetId, double speed) {
      return new SpiderStrikePayload(slot, 1, targetId, speed);
   }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
