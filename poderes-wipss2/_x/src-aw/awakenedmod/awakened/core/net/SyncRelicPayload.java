package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.data.RelicData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record SyncRelicPayload(boolean belt, List<String> attuned, Map<String, Integer> levels) implements CustomPacketPayload {
   public static final Type<SyncRelicPayload> TYPE = new Type(AwId.of("sync_relics"));
   public static final StreamCodec<RegistryFriendlyByteBuf, SyncRelicPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.BOOL,
      SyncRelicPayload::belt,
      ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
      SyncRelicPayload::attuned,
      ByteBufCodecs.map(i -> new HashMap(), ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT),
      SyncRelicPayload::levels,
      SyncRelicPayload::new
   );

   public static SyncRelicPayload of(RelicData data) {
      return new SyncRelicPayload(data.hasBelt(), new ArrayList<>(data.attuned()), new HashMap<>(data.levels()));
   }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
