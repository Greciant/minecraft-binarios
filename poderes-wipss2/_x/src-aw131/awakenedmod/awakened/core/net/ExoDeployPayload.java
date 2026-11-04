package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record ExoDeployPayload() implements CustomPacketPayload {
   public static final ExoDeployPayload INSTANCE = new ExoDeployPayload();
   public static final Type<ExoDeployPayload> TYPE = new Type(AwId.of("exo_deploy"));
   public static final StreamCodec<RegistryFriendlyByteBuf, ExoDeployPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
