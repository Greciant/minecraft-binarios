package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record StoneMechActionPayload(int action) implements CustomPacketPayload {
   public static final int HAMMER = 0;
   public static final int BOULDER = 1;
   public static final int EXIT = 2;
   public static final Type<StoneMechActionPayload> TYPE = new Type(AwId.of("stone_mech_action"));
   public static final StreamCodec<RegistryFriendlyByteBuf, StoneMechActionPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, StoneMechActionPayload::action, StoneMechActionPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
