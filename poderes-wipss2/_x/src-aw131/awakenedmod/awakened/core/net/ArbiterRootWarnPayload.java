package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record ArbiterRootWarnPayload(boolean inside, int ticksToBurst) implements CustomPacketPayload {
   public static final Type<ArbiterRootWarnPayload> TYPE = new Type(AwId.of("arbiter_root_warn"));
   public static final StreamCodec<RegistryFriendlyByteBuf, ArbiterRootWarnPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.BOOL, ArbiterRootWarnPayload::inside, ByteBufCodecs.VAR_INT, ArbiterRootWarnPayload::ticksToBurst, ArbiterRootWarnPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
