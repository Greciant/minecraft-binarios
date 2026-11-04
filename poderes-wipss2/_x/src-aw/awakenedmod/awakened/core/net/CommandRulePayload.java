package com.awakenedmod.awakened.core.net;

import com.awakenedmod.awakened.content.command.Rule;
import com.awakenedmod.awakened.core.AwId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record CommandRulePayload(int slot, Rule rule) implements CustomPacketPayload {
   public static final Type<CommandRulePayload> TYPE = new Type(AwId.of("command_rule"));
   public static final StreamCodec<RegistryFriendlyByteBuf, CommandRulePayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, CommandRulePayload::slot, Rule.STREAM_CODEC, CommandRulePayload::rule, CommandRulePayload::new
   );

   public static CommandRulePayload clear(int slot) {
      return new CommandRulePayload(slot, Rule.EMPTY);
   }

   public boolean clears() {
      return this.rule.isEmpty();
   }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
