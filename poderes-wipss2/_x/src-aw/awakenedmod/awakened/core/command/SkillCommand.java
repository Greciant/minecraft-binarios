package com.awakenedmod.awakened.core.command;

import com.awakenedmod.awakened.core.skill.SkillSystem;
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

public final class SkillCommand {
   private SkillCommand() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(SkillCommand::onRegister);
   }

   private static void onRegister(RegisterCommandsEvent event) {
      register(event.getDispatcher());
   }

   private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)Commands.literal("awakened")
            .then(
               ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("skill").requires(RealisticMovementCommand::isOp))
                        .then(
                           Commands.literal("add")
                              .then(
                                 Commands.argument("target", EntityArgument.player())
                                    .then(
                                       Commands.argument("xp", DoubleArgumentType.doubleArg(0.0))
                                          .executes(
                                             ctx -> add(
                                                   (CommandSourceStack)ctx.getSource(),
                                                   EntityArgument.getPlayer(ctx, "target"),
                                                   DoubleArgumentType.getDouble(ctx, "xp")
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
                                    ((RequiredArgumentBuilder)Commands.argument("level", IntegerArgumentType.integer(1))
                                          .executes(
                                             ctx -> set(
                                                   (CommandSourceStack)ctx.getSource(),
                                                   EntityArgument.getPlayer(ctx, "target"),
                                                   IntegerArgumentType.getInteger(ctx, "level"),
                                                   0.0
                                                )
                                          ))
                                       .then(
                                          Commands.argument("xp", DoubleArgumentType.doubleArg(0.0))
                                             .executes(
                                                ctx -> set(
                                                      (CommandSourceStack)ctx.getSource(),
                                                      EntityArgument.getPlayer(ctx, "target"),
                                                      IntegerArgumentType.getInteger(ctx, "level"),
                                                      DoubleArgumentType.getDouble(ctx, "xp")
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

   private static int add(CommandSourceStack source, ServerPlayer target, double xp) {
      SkillSystem.award(target, xp);
      source.sendSuccess(() -> Component.translatable("commands.awakened.skill.added", new Object[]{xp, target.getName()}), true);
      return 1;
   }

   private static int set(CommandSourceStack source, ServerPlayer target, int level, double xp) {
      SkillSystem.setLevel(target, level, xp);
      source.sendSuccess(() -> Component.translatable("commands.awakened.skill.set", new Object[]{target.getName(), level}), true);
      return 1;
   }

   private static int remove(CommandSourceStack source, ServerPlayer target) {
      SkillSystem.resetCharacter(target);
      source.sendSuccess(() -> Component.translatable("commands.awakened.skill.removed", new Object[]{target.getName()}), true);
      return 1;
   }
}
