package com.impactframe.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;

public record FreezeStatePayload(boolean active) implements CustomPacketPayload {
   public static final Type<FreezeStatePayload> TYPE = new Type(ResourceLocation.fromNamespaceAndPath("impactframe", "freeze_state"));
   public static final StreamCodec<RegistryFriendlyByteBuf, FreezeStatePayload> CODEC = StreamCodec.composite(
      ByteBufCodecs.BOOL, FreezeStatePayload::active, FreezeStatePayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
