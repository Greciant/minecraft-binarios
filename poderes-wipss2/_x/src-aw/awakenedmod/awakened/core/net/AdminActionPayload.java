package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record AdminActionPayload(int action, String powerId, int intArg) implements CustomPacketPayload {
   public static final Type<AdminActionPayload> TYPE = new Type(AwId.of("admin_action"));
   public static final StreamCodec<RegistryFriendlyByteBuf, AdminActionPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      AdminActionPayload::action,
      ByteBufCodecs.STRING_UTF8,
      AdminActionPayload::powerId,
      ByteBufCodecs.VAR_INT,
      AdminActionPayload::intArg,
      AdminActionPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
