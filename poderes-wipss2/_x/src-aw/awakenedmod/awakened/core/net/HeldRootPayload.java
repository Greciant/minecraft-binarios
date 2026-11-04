package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record HeldRootPayload(boolean rooted) implements CustomPacketPayload {
   public static final Type<HeldRootPayload> TYPE = new Type(AwId.of("held_root"));
   public static final StreamCodec<RegistryFriendlyByteBuf, HeldRootPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.BOOL, HeldRootPayload::rooted, HeldRootPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
