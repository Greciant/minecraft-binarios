package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record EquipBeltPayload(boolean equip) implements CustomPacketPayload {
   public static final Type<EquipBeltPayload> TYPE = new Type(AwId.of("equip_belt"));
   public static final StreamCodec<RegistryFriendlyByteBuf, EquipBeltPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.BOOL, EquipBeltPayload::equip, EquipBeltPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
