package com.awakenedmod.awakened.core.command;

import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class MasteryCommand {
   private MasteryCommand() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(MasteryCommand::onRegister);
   }

   private static void onRegister(RegisterCommandsEvent event) {
      register(event.getDispatcher());
   }

   private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)Commands.literal("awakened")
            .then(
               ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("mastery").requires(RealisticMovementCommand::isOp))
                        .then(
                           Commands.literal("add")
                              .then(
                                 Commands.argument("target", EntityArgument.player())
                                    .then(
                                       Commands.argument("points", DoubleArgumentType.doubleArg(0.0))
                                          .executes(
                                             ctx -> add(
                                                   (CommandSourceStack)ctx.getSource(),
                                                   EntityArgument.getPlayer(ctx, "target"),
                                                   DoubleArgumentType.getDouble(ctx, "points")
                                                )
                                          )
                                    )
                              )
                        ))
                     .then(
                        Commands.literal("set")
                           .then(
                              Commands.argument("target", EntityArgument.player())
                                 .then(
                                    ((RequiredArgumentBuilder)Commands.argument("level", IntegerArgumentType.integer(0))
                                          .executes(
                                             ctx -> set(
                                                   (CommandSourceStack)ctx.getSource(),
                                                   EntityArgument.getPlayer(ctx, "target"),
                                                   IntegerArgumentType.getInteger(ctx, "level"),
                                                   0.0
                                                )
                                          ))
                                       .then(
                                          Commands.argument("progress", DoubleArgumentType.doubleArg(0.0))
                                             .executes(
                                                ctx -> set(
                                                      (CommandSourceStack)ctx.getSource(),
                                                      EntityArgument.getPlayer(ctx, "target"),
                                                      IntegerArgumentType.getInteger(ctx, "level"),
                                                      DoubleArgumentType.getDouble(ctx, "progress")
                                                   )
                                             )
                                       )
                                 )
                           )
                     ))
                  .then(
                     Commands.literal("remove")
                        .then(
                           Commands.argument("target", EntityArgument.player())
                              .executes(ctx -> remove((CommandSourceStack)ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))
                        )
                  )
            )
      );
   }

   private static int add(CommandSourceStack source, ServerPlayer target, double points) {
      if (!PowerService.data(target).hasPower()) {
         source.sendFailure(Component.translatable("commands.awakened.mastery.nopower", new Object[]{target.getName()}));
         return 0;
      } else {
         MasterySystem.grant(target, points);
         source.sendSuccess(() -> Component.translatable("commands.awakened.mastery.added", new Object[]{points, target.getName()}), true);
         return 1;
      }
   }

   private static int set(CommandSourceStack source, ServerPlayer target, int level, double progress) {
      PowerData data = PowerService.data(target);
      if (!data.hasPower()) {
         source.sendFailure(Component.translatable("commands.awakened.mastery.nopower", new Object[]{target.getName()}));
         return 0;
      } else {
         data.currentMastery().set(level, progress);
         PowerService.sync(target);
         QuestSystem.sync(target);
         source.sendSuccess(() -> Component.translatable("commands.awakened.mastery.set", new Object[]{target.getName(), level}), true);
         return 1;
      }
   }

   private static int remove(CommandSourceStack source, ServerPlayer target) {
      PowerData data = PowerService.data(target);
      if (!data.hasPower()) {
         source.sendFailure(Component.translatable("commands.awakened.mastery.nopower", new Object[]{target.getName()}));
         return 0;
      } else {
         data.currentMastery().reset();
         PowerService.sync(target);
         QuestSystem.sync(target);
         source.sendSuccess(() -> Component.translatable("commands.awakened.mastery.removed", new Object[]{target.getName()}), true);
         return 1;
      }
   }
}
