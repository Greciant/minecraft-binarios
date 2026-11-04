package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record CommandRenamePayload(int slot, String name) implements CustomPacketPayload {
   public static final Type<CommandRenamePayload> TYPE = new Type(AwId.of("command_rename"));
   public static final StreamCodec<RegistryFriendlyByteBuf, CommandRenamePayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, CommandRenamePayload::slot, ByteBufCodecs.stringUtf8(64), CommandRenamePayload::name, CommandRenamePayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
