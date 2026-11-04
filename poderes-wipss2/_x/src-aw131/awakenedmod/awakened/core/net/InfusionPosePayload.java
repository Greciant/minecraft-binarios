package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record InfusionPosePayload(boolean active, float bodyYaw) implements CustomPacketPayload {
   public static final Type<InfusionPosePayload> TYPE = new Type(AwId.of("infusion_pose"));
   public static final StreamCodec<RegistryFriendlyByteBuf, InfusionPosePayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.BOOL, InfusionPosePayload::active, ByteBufCodecs.FLOAT, InfusionPosePayload::bodyYaw, InfusionPosePayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
