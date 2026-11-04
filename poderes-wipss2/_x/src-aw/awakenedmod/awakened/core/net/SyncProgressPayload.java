package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.data.ProgressData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record SyncProgressPayload(int timesAwakened, int highestMastery, int enemiesDefeated, int bossesDefeated) implements CustomPacketPayload {
   public static final Type<SyncProgressPayload> TYPE = new Type(AwId.of("sync_progress"));
   public static final StreamCodec<RegistryFriendlyByteBuf, SyncProgressPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      SyncProgressPayload::timesAwakened,
      ByteBufCodecs.VAR_INT,
      SyncProgressPayload::highestMastery,
      ByteBufCodecs.VAR_INT,
      SyncProgressPayload::enemiesDefeated,
      ByteBufCodecs.VAR_INT,
      SyncProgressPayload::bossesDefeated,
      SyncProgressPayload::new
   );

   public static SyncProgressPayload of(ProgressData data) {
      return new SyncProgressPayload(data.timesAwakened(), data.highestMastery(), data.enemiesDefeated(), data.bossesDefeated());
   }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
