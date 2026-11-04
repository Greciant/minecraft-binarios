package com.awakenedmod.awakened.core.command;

import com.awakenedmod.awakened.core.trade.PowerTrades;
import com.awakenedmod.awakened.core.trade.TradeConfig;
import com.awakenedmod.awakened.core.trade.TradeRule;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.function.Consumer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class TradeCommand {
   private static final SimpleCommandExceptionType DISABLED = new SimpleCommandExceptionType(Component.translatable("awakened.trade.reason.disabled"));

   private TradeCommand() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(TradeCommand::onRegister);
   }

   private static void onRegister(RegisterCommandsEvent event) {
      register(event.getDispatcher());
   }

   private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)Commands.literal("awakened")
            .then(
               ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("trade")
                        .then(Commands.literal("review").executes(ctx -> run((CommandSourceStack)ctx.getSource(), PowerTrades::review))))
                     .then(Commands.literal("decline").executes(ctx -> run((CommandSourceStack)ctx.getSource(), PowerTrades::decline))))
                  .then(Commands.literal("cancel").executes(ctx -> run((CommandSourceStack)ctx.getSource(), PowerTrades::cancelByCommand)))
            )
      );
      dispatcher.register(
         (LiteralArgumentBuilder)Commands.literal("awakened")
            .then(OnOffCommand.node("trading", "commands.awakened.trading", TradeRule::allowed, TradeRule::set, TradeConfig.ALLOW_POWER_TRADING))
      );
   }

   private static int run(CommandSourceStack source, Consumer<ServerPlayer> action) throws CommandSyntaxException {
      if (!TradeRule.allowed(source.getServer())) {
         throw DISABLED.create();
      } else {
         action.accept(source.getPlayerOrException());
         return 1;
      }
   }
}
