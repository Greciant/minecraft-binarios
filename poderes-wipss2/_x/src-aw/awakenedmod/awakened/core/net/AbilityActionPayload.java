package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record AbilityActionPayload(int slot, int action) implements CustomPacketPayload {
   public static final int FLING = 0;
   public static final int EXTEND = 1;
   public static final int RETRACT = 2;
   public static final int SNEAK_ACTIVATE = 3;
   public static final int THROW = 4;
   public static final int VAULT = 5;
   public static final int DETONATE = 6;
   public static final Type<AbilityActionPayload> TYPE = new Type(AwId.of("ability_action"));
   public static final StreamCodec<RegistryFriendlyByteBuf, AbilityActionPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, AbilityActionPayload::slot, ByteBufCodecs.VAR_INT, AbilityActionPayload::action, AbilityActionPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
