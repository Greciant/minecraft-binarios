package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record AeroSurfPayload(int action) implements CustomPacketPayload {
   public static final int START = 0;
   public static final int END = 1;
   public static final int DASH = 2;
   public static final Type<AeroSurfPayload> TYPE = new Type(AwId.of("aero_surf"));
   public static final StreamCodec<RegistryFriendlyByteBuf, AeroSurfPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, AeroSurfPayload::action, AeroSurfPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
