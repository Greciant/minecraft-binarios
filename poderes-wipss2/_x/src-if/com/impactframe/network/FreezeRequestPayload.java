package com.impactframe.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;

public record FreezeRequestPayload(int ticks) implements CustomPacketPayload {
   public static final Type<FreezeRequestPayload> TYPE = new Type(ResourceLocation.fromNamespaceAndPath("impactframe", "freeze_request"));
   public static final StreamCodec<RegistryFriendlyByteBuf, FreezeRequestPayload> CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, FreezeRequestPayload::ticks, FreezeRequestPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
