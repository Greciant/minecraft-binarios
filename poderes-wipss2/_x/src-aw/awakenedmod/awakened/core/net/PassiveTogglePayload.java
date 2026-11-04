package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record PassiveTogglePayload(String key) implements CustomPacketPayload {
   public static final Type<PassiveTogglePayload> TYPE = new Type(AwId.of("passive_toggle"));
   public static final StreamCodec<RegistryFriendlyByteBuf, PassiveTogglePayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.STRING_UTF8, PassiveTogglePayload::key, PassiveTogglePayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
