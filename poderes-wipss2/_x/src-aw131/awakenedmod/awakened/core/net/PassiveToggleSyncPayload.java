package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record PassiveToggleSyncPayload(Map<String, Boolean> states) implements CustomPacketPayload {
   public static final Type<PassiveToggleSyncPayload> TYPE = new Type(AwId.of("passive_toggle_sync"));
   public static final StreamCodec<RegistryFriendlyByteBuf, PassiveToggleSyncPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.BOOL), PassiveToggleSyncPayload::states, PassiveToggleSyncPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
