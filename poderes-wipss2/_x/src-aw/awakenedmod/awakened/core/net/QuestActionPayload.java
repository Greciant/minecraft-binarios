package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record QuestActionPayload(int action, int slot, String questId) implements CustomPacketPayload {
   public static final int ACTION_PIN = 0;
   public static final int ACTION_REROLL = 1;
   public static final Type<QuestActionPayload> TYPE = new Type(AwId.of("quest_action"));
   public static final StreamCodec<RegistryFriendlyByteBuf, QuestActionPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      QuestActionPayload::action,
      ByteBufCodecs.VAR_INT,
      QuestActionPayload::slot,
      ByteBufCodecs.STRING_UTF8,
      QuestActionPayload::questId,
      QuestActionPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
