package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record SyncQuestPayload(List<SyncQuestPayload.Entry> entries, List<String> slate, String pinned, int rerolls) implements CustomPacketPayload {
   public static final Type<SyncQuestPayload> TYPE = new Type(AwId.of("sync_quests"));
   public static final StreamCodec<RegistryFriendlyByteBuf, SyncQuestPayload> STREAM_CODEC = StreamCodec.composite(
      SyncQuestPayload.Entry.STREAM_CODEC.apply(ByteBufCodecs.list()),
      SyncQuestPayload::entries,
      ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
      SyncQuestPayload::slate,
      ByteBufCodecs.STRING_UTF8,
      SyncQuestPayload::pinned,
      ByteBufCodecs.VAR_INT,
      SyncQuestPayload::rerolls,
      SyncQuestPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static record Entry(String id, int progress, boolean complete, int timesCompleted, boolean xpClaimed, boolean itemsClaimed) {
      public static final StreamCodec<RegistryFriendlyByteBuf, SyncQuestPayload.Entry> STREAM_CODEC = StreamCodec.composite(
         ByteBufCodecs.STRING_UTF8,
         SyncQuestPayload.Entry::id,
         ByteBufCodecs.VAR_INT,
         SyncQuestPayload.Entry::progress,
         ByteBufCodecs.BOOL,
         SyncQuestPayload.Entry::complete,
         ByteBufCodecs.VAR_INT,
         SyncQuestPayload.Entry::timesCompleted,
         ByteBufCodecs.BOOL,
         SyncQuestPayload.Entry::xpClaimed,
         ByteBufCodecs.BOOL,
         SyncQuestPayload.Entry::itemsClaimed,
         SyncQuestPayload.Entry::new
      );
   }
}
