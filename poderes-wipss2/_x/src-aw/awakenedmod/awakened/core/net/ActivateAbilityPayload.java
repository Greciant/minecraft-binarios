package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record ActivateAbilityPayload(int slot) implements CustomPacketPayload {
   public static final Type<ActivateAbilityPayload> TYPE = new Type(AwId.of("activate_ability"));
   public static final StreamCodec<RegistryFriendlyByteBuf, ActivateAbilityPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, ActivateAbilityPayload::slot, ActivateAbilityPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
