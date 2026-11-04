package com.awakenedmod.awakened.core.command;

import com.awakenedmod.awakened.core.AwakenedConfigs;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;

final class OnOffCommand {
   private OnOffCommand() {
   }

   static LiteralArgumentBuilder<CommandSourceStack> node(
      String name, String keys, Predicate<MinecraftServer> get, BiFunction<MinecraftServer, Boolean, AwakenedConfigs.Saved> set, ConfigValue<Boolean> value
   ) {
      return (LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal(
                        name
                     )
                     .requires(RealisticMovementCommand::isOp))
                  .executes(ctx -> report((CommandSourceStack)ctx.getSource(), keys, get.test(((CommandSourceStack)ctx.getSource()).getServer()))))
               .then(Commands.literal("on").executes(ctx -> set((CommandSourceStack)ctx.getSource(), keys, set, value, true))))
            .then(Commands.literal("off").executes(ctx -> set((CommandSourceStack)ctx.getSource(), keys, set, value, false))))
         .then(
            Commands.literal("toggle")
               .executes(ctx -> set((CommandSourceStack)ctx.getSource(), keys, set, value, !get.test(((CommandSourceStack)ctx.getSource()).getServer())))
         );
   }

   static Component state(boolean on) {
      return on
         ? Component.translatable("commands.awakened.on").withStyle(ChatFormatting.GREEN)
         : Component.translatable("commands.awakened.off").withStyle(ChatFormatting.RED);
   }

   private static int report(CommandSourceStack source, String keys, boolean on) {
      source.sendSuccess(() -> Component.translatable(keys + ".status", new Object[]{state(on)}), false);
      return on ? 1 : 0;
   }

   private static int set(
      CommandSourceStack source, String keys, BiFunction<MinecraftServer, Boolean, AwakenedConfigs.Saved> set, ConfigValue<Boolean> config, boolean value
   ) {
      AwakenedConfigs.Saved where = set.apply(source.getServer(), value);
      if (ConfigFeedback.failed(source, where, () -> Component.translatable(keys + ".already", new Object[]{state(value)}))) {
         return 0;
      } else {
         source.sendSuccess(() -> Component.translatable(keys + ".set", new Object[]{state(value)}), true);
         ConfigFeedback.scope(source, where, config);
         return value ? 1 : 0;
      }
   }
}
