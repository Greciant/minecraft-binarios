package com.awakenedmod.awakened.core.command;

import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.power.Power;
import com.awakenedmod.awakened.core.power.PowerRegistry;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class PowerCommand {
   private static final SuggestionProvider<CommandSourceStack> SUGGEST_POWERS = (ctx, builder) -> SharedSuggestionProvider.suggest(
         PowerRegistry.all().stream().map(Power::id), builder
      );

   private PowerCommand() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(PowerCommand::onRegister);
   }

   private static void onRegister(RegisterCommandsEvent event) {
      register(event.getDispatcher());
   }

   private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)Commands.literal("awakened")
            .then(
               ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("power").requires(RealisticMovementCommand::isOp))
                     .then(
                        Commands.literal("give")
                           .then(
                              Commands.argument("target", EntityArgument.player())
                                 .then(
                                    Commands.argument("power", StringArgumentType.word())
                                       .suggests(SUGGEST_POWERS)
                                       .executes(
                                          ctx -> give(
                                                (CommandSourceStack)ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "target"),
                                                StringArgumentType.getString(ctx, "power")
                                             )
                                       )
                                 )
                           )
                     ))
                  .then(
                     Commands.literal("take")
                        .then(
                           Commands.argument("target", EntityArgument.player())
                              .executes(ctx -> take((CommandSourceStack)ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))
                        )
                  )
            )
      );
   }

   private static int give(CommandSourceStack source, ServerPlayer target, String powerId) {
      Power power = PowerRegistry.get(powerId);
      if (power == null) {
         source.sendFailure(Component.translatable("commands.awakened.power.unknown", new Object[]{powerId}));
         return 0;
      } else {
         PowerService.setPower(target, powerId);
         QuestSystem.resetForPowerChange(target);
         PowerService.sync(target);
         QuestSystem.sync(target);
         source.sendSuccess(() -> Component.translatable("commands.awakened.power.given", new Object[]{target.getName(), power.displayName()}), true);
         return 1;
      }
   }

   private static int take(CommandSourceStack source, ServerPlayer target) {
      PowerService.clearPower(target);
      PowerService.sync(target);
      QuestSystem.sync(target);
      source.sendSuccess(() -> Component.translatable("commands.awakened.power.taken", new Object[]{target.getName()}), true);
      return 1;
   }
}
