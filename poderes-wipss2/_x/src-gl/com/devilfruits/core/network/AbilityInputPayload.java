package com.devilfruits.core.network;

import com.devilfruits.DevilFruits;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;

public record AbilityInputPayload(AbilityInputPayload.Action action, ResourceLocation ability, int slot) implements CustomPacketPayload {
   public static final Type<AbilityInputPayload> TYPE = new Type(DevilFruits.id("ability_input"));
   public static final StreamCodec<FriendlyByteBuf, AbilityInputPayload> STREAM_CODEC = CustomPacketPayload.codec((payload, buf) -> {
      buf.writeEnum(payload.action);
      buf.writeResourceLocation(payload.ability);
      buf.writeVarInt(payload.slot);
   }, buf -> new AbilityInputPayload((AbilityInputPayload.Action)buf.readEnum(AbilityInputPayload.Action.class), buf.readResourceLocation(), buf.readVarInt()));

   public static AbilityInputPayload press(ResourceLocation ability) {
      return new AbilityInputPayload(AbilityInputPayload.Action.PRESS, ability, -1);
   }

   public static AbilityInputPayload release(ResourceLocation ability) {
      return new AbilityInputPayload(AbilityInputPayload.Action.RELEASE, ability, -1);
   }

   public static AbilityInputPayload select(ResourceLocation ability, int slot) {
      return new AbilityInputPayload(AbilityInputPayload.Action.SELECT, ability, slot);
   }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static enum Action {
      PRESS,
      RELEASE,
      SELECT;
   }
}
