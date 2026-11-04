package com.impactframe.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;

public record HighDamageAttackPayload(int targetId, float damage) implements CustomPacketPayload {
   public static final Type<HighDamageAttackPayload> TYPE = new Type(ResourceLocation.fromNamespaceAndPath("impactframe", "high_damage_attack"));
   public static final StreamCodec<RegistryFriendlyByteBuf, HighDamageAttackPayload> CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, HighDamageAttackPayload::targetId, ByteBufCodecs.FLOAT, HighDamageAttackPayload::damage, HighDamageAttackPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
