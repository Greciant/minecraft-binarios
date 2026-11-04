package com.awakenedmod.awakened.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

public final class ControlsHintCommand {
   private ControlsHintCommand() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(ControlsHintCommand::onRegister);
   }

   private static void onRegister(RegisterClientCommandsEvent event) {
      register(event.getDispatcher());
   }

   private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)Commands.literal("awakened")
            .then(
               ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("controlshint")
                           .executes(ctx -> report((CommandSourceStack)ctx.getSource())))
                        .then(Commands.literal("on").executes(ctx -> set((CommandSourceStack)ctx.getSource(), true))))
                     .then(Commands.literal("off").executes(ctx -> set((CommandSourceStack)ctx.getSource(), false))))
                  .then(Commands.literal("toggle").executes(ctx -> set((CommandSourceStack)ctx.getSource(), !(Boolean)ClientConfig.AWAKEN_CONTROLS_CHAT.get())))
            )
      );
   }

   private static int report(CommandSourceStack source) {
      boolean on = (Boolean)ClientConfig.AWAKEN_CONTROLS_CHAT.get();
      source.sendSuccess(() -> Component.translatable("commands.awakened.controlshint.status", new Object[]{state(on)}), false);
      return on ? 1 : 0;
   }

   private static int set(CommandSourceStack source, boolean value) {
      ClientConfig.AWAKEN_CONTROLS_CHAT.set(value);
      source.sendSuccess(() -> Component.translatable("commands.awakened.controlshint.set", new Object[]{state(value)}), false);
      return value ? 1 : 0;
   }

   private static Component state(boolean on) {
      return on
         ? Component.translatable("commands.awakened.controlshint.on").withStyle(ChatFormatting.GREEN)
         : Component.translatable("commands.awakened.controlshint.off").withStyle(ChatFormatting.RED);
   }
}
