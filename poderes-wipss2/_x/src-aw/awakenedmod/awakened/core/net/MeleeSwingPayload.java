package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record MeleeSwingPayload() implements CustomPacketPayload {
   public static final MeleeSwingPayload INSTANCE = new MeleeSwingPayload();
   public static final Type<MeleeSwingPayload> TYPE = new Type(AwId.of("melee_swing"));
   public static final StreamCodec<RegistryFriendlyByteBuf, MeleeSwingPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
