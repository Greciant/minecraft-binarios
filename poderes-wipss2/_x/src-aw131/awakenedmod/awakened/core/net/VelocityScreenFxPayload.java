package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record VelocityScreenFxPayload(int kind, boolean on) implements CustomPacketPayload {
   public static final int TIMELESS = 0;
   public static final int TUNNEL = 1;
   public static final int BOOM = 2;
   public static final Type<VelocityScreenFxPayload> TYPE = new Type(AwId.of("velocity_screen_fx"));
   public static final StreamCodec<RegistryFriendlyByteBuf, VelocityScreenFxPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, VelocityScreenFxPayload::kind, ByteBufCodecs.BOOL, VelocityScreenFxPayload::on, VelocityScreenFxPayload::new
   );

   public static VelocityScreenFxPayload timeless(boolean on) {
      return new VelocityScreenFxPayload(0, on);
   }

   public static VelocityScreenFxPayload tunnel(boolean on) {
      return new VelocityScreenFxPayload(1, on);
   }

   public static VelocityScreenFxPayload boom() {
      return new VelocityScreenFxPayload(2, true);
   }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
