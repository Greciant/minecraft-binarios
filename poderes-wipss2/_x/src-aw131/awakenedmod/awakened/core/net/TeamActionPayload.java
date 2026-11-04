package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record TeamActionPayload(int action, String arg) implements CustomPacketPayload {
   public static final int ACTION_KICK = 0;
   public static final int ACTION_LEAVE = 1;
   public static final int ACTION_DISBAND = 2;
   public static final int ACTION_FF_ON = 3;
   public static final int ACTION_FF_OFF = 4;
   public static final int ACTION_INVITE = 5;
   public static final int ACTION_ACCEPT = 6;
   public static final int ACTION_DENY = 7;
   public static final Type<TeamActionPayload> TYPE = new Type(AwId.of("team_action"));
   public static final StreamCodec<RegistryFriendlyByteBuf, TeamActionPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, TeamActionPayload::action, ByteBufCodecs.STRING_UTF8, TeamActionPayload::arg, TeamActionPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
