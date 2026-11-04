package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record SetActiveSlotPayload(int slot) implements CustomPacketPayload {
   public static final Type<SetActiveSlotPayload> TYPE = new Type(AwId.of("set_active_slot"));
   public static final StreamCodec<RegistryFriendlyByteBuf, SetActiveSlotPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, SetActiveSlotPayload::slot, SetActiveSlotPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
