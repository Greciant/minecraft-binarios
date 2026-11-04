package com.awakenedmod.awakened.content.exosavant;

import com.awakenedmod.awakened.core.command.RealisticMovementCommand;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Arrays;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class ExoSkinCommand {
   private ExoSkinCommand() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(ExoSkinCommand::onRegister);
   }

   private static void onRegister(RegisterCommandsEvent event) {
      register(event.getDispatcher());
   }

   private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)Commands.literal("awakened")
            .then(
               ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("exoskins").requires(RealisticMovementCommand::isOp))
                     .then(
                        Commands.literal("unlock")
                           .then(
                              Commands.argument("target", EntityArgument.player())
                                 .executes(ctx -> unlockAll((CommandSourceStack)ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))
                           )
                     ))
                  .then(
                     Commands.literal("wear")
                        .then(
                           Commands.argument("target", EntityArgument.player())
                              .then(
                                 Commands.argument("skin", StringArgumentType.word())
                                    .suggests((ctx, b) -> SharedSuggestionProvider.suggest(Arrays.stream(ExoSkin.values()).map(ExoSkin::id), b))
                                    .executes(
                                       ctx -> wear(
                                             (CommandSourceStack)ctx.getSource(),
                                             EntityArgument.getPlayer(ctx, "target"),
                                             StringArgumentType.getString(ctx, "skin")
                                          )
                                    )
                              )
                        )
                  )
            )
      );
   }

   private static int wear(CommandSourceStack source, ServerPlayer target, String id) {
      ExoSkin skin = ExoSkin.byId(id);
      if (!skin.id().equals(id)) {
         source.sendFailure(Component.translatable("commands.awakened.exoskins.unknown", new Object[]{id}));
         return 0;
      } else {
         ExoSkins.unlock(target, skin);
         ExoSkins.select(target, skin.id());
         source.sendSuccess(() -> Component.translatable("commands.awakened.exoskins.wearing", new Object[]{target.getName(), skin.displayName()}), true);
         return 1;
      }
   }

   private static int unlockAll(CommandSourceStack source, ServerPlayer target) {
      for (ExoSkin skin : ExoSkin.values()) {
         ExoSkins.unlock(target, skin);
      }

      source.sendSuccess(() -> Component.translatable("commands.awakened.exoskins.unlocked", new Object[]{target.getName()}), true);
      return 1;
   }
}
