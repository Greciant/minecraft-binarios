package com.awakenedmod.awakened.core.command;

import com.awakenedmod.awakened.content.exosavant.Failsafe;
import com.awakenedmod.awakened.core.PowerService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class FailsafeCommand {
   private FailsafeCommand() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(FailsafeCommand::onRegister);
   }

   private static void onRegister(RegisterCommandsEvent event) {
      register(event.getDispatcher());
   }

   private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)Commands.literal("awakened")
            .then(
               ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("failsafe")
                           .executes(ctx -> report((CommandSourceStack)ctx.getSource())))
                        .then(Commands.literal("on").executes(ctx -> set((CommandSourceStack)ctx.getSource(), true))))
                     .then(Commands.literal("off").executes(ctx -> set((CommandSourceStack)ctx.getSource(), false))))
                  .then(Commands.literal("toggle").executes(ctx -> toggle((CommandSourceStack)ctx.getSource())))
            )
      );
   }

   private static int report(CommandSourceStack source) throws CommandSyntaxException {
      ServerPlayer player = source.getPlayerOrException();
      boolean on = Failsafe.enabled(PowerService.data(player));
      source.sendSuccess(() -> Component.translatable("commands.awakened.failsafe.status", new Object[]{state(on)}), false);
      return on ? 1 : 0;
   }

   private static int toggle(CommandSourceStack source) throws CommandSyntaxException {
      ServerPlayer player = source.getPlayerOrException();
      return set(source, !Failsafe.enabled(PowerService.data(player)));
   }

   private static int set(CommandSourceStack source, boolean value) throws CommandSyntaxException {
      ServerPlayer player = source.getPlayerOrException();
      Failsafe.setEnabled(player, value);
      source.sendSuccess(() -> Component.translatable("commands.awakened.failsafe.set", new Object[]{state(value)}), false);
      return value ? 1 : 0;
   }

   private static Component state(boolean on) {
      return on
         ? Component.translatable("commands.awakened.failsafe.on").withStyle(ChatFormatting.GREEN)
         : Component.translatable("commands.awakened.failsafe.off").withStyle(ChatFormatting.RED);
   }
}
