package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record CatalystSpinPayload(String powerId, long seed, List<String> poolIds, String currentId) implements CustomPacketPayload {
   public static final Type<CatalystSpinPayload> TYPE = new Type(AwId.of("catalyst_spin"));
   public static final int SPIN_TICKS = 92;
   public static final StreamCodec<RegistryFriendlyByteBuf, CatalystSpinPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.STRING_UTF8,
      CatalystSpinPayload::powerId,
      ByteBufCodecs.VAR_LONG,
      CatalystSpinPayload::seed,
      ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
      CatalystSpinPayload::poolIds,
      ByteBufCodecs.STRING_UTF8,
      CatalystSpinPayload::currentId,
      CatalystSpinPayload::new
   );

   public boolean offersChoice() {
      return !this.currentId.isEmpty();
   }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
