package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record SpiderStatePayload(boolean swinging, boolean hanging, double anchorX, double anchorY, double anchorZ, boolean leftArm)
   implements CustomPacketPayload {
   public static final Type<SpiderStatePayload> TYPE = new Type(AwId.of("spider_state"));
   public static final SpiderStatePayload IDLE = new SpiderStatePayload(false, false, 0.0, 0.0, 0.0, false);
   public static final StreamCodec<RegistryFriendlyByteBuf, SpiderStatePayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.BOOL,
      SpiderStatePayload::swinging,
      ByteBufCodecs.BOOL,
      SpiderStatePayload::hanging,
      ByteBufCodecs.DOUBLE,
      SpiderStatePayload::anchorX,
      ByteBufCodecs.DOUBLE,
      SpiderStatePayload::anchorY,
      ByteBufCodecs.DOUBLE,
      SpiderStatePayload::anchorZ,
      ByteBufCodecs.BOOL,
      SpiderStatePayload::leftArm,
      SpiderStatePayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
