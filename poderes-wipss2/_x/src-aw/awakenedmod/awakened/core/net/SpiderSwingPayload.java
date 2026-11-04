package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record SpiderSwingPayload(int action, int slot, double ax, double ay, double az, boolean left) implements CustomPacketPayload {
   public static final int START = 0;
   public static final int HANG = 1;
   public static final int UNHANG = 2;
   public static final int END_RELEASE = 3;
   public static final int END_DIVE = 4;
   public static final int START_HANG = 5;
   public static final int WINGS_START = 6;
   public static final int WINGS_END = 7;
   public static final int DIVE_START = 8;
   public static final int DIVE_END = 9;
   public static final Type<SpiderSwingPayload> TYPE = new Type(AwId.of("spider_swing"));
   public static final StreamCodec<RegistryFriendlyByteBuf, SpiderSwingPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      SpiderSwingPayload::action,
      ByteBufCodecs.VAR_INT,
      SpiderSwingPayload::slot,
      ByteBufCodecs.DOUBLE,
      SpiderSwingPayload::ax,
      ByteBufCodecs.DOUBLE,
      SpiderSwingPayload::ay,
      ByteBufCodecs.DOUBLE,
      SpiderSwingPayload::az,
      ByteBufCodecs.BOOL,
      SpiderSwingPayload::left,
      SpiderSwingPayload::new
   );

   public static SpiderSwingPayload of(int action, int slot) {
      return new SpiderSwingPayload(action, slot, 0.0, 0.0, 0.0, false);
   }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
