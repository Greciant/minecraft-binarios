package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record SoulMenuPayload(int action, String soulType, int amount) implements CustomPacketPayload {
   public static final int SET_ACTIVE = 0;
   public static final int LOADOUT_DELTA = 1;
   public static final int DISCARD = 2;
   public static final int TOGGLE_LOCK = 3;
   public static final int BLACKLIST_ADD = 4;
   public static final int BLACKLIST_REMOVE = 5;
   public static final Type<SoulMenuPayload> TYPE = new Type(AwId.of("soul_menu"));
   public static final StreamCodec<RegistryFriendlyByteBuf, SoulMenuPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      SoulMenuPayload::action,
      ByteBufCodecs.STRING_UTF8,
      SoulMenuPayload::soulType,
      ByteBufCodecs.VAR_INT,
      SoulMenuPayload::amount,
      SoulMenuPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
