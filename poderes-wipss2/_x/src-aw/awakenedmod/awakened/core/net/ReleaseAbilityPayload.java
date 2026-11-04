package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record ReleaseAbilityPayload(int slot) implements CustomPacketPayload {
   public static final Type<ReleaseAbilityPayload> TYPE = new Type(AwId.of("release_ability"));
   public static final StreamCodec<RegistryFriendlyByteBuf, ReleaseAbilityPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, ReleaseAbilityPayload::slot, ReleaseAbilityPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
