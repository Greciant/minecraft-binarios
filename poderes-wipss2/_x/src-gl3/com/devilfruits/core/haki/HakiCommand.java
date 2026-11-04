package com.devilfruits.core.haki;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Collection;
import java.util.function.Consumer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class HakiCommand {
   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal(
                              "haki"
                           )
                           .requires(source -> source.hasPermission(2)))
                        .then(
                           Commands.literal("unlock")
                              .then(Commands.argument("targets", EntityArgument.players()).executes(ctx -> each(ctx, "unlock", HakiManager::unlock)))
                        ))
                     .then(
                        Commands.literal("level")
                           .then(
                              Commands.argument("targets", EntityArgument.players())
                                 .then(
                                    Commands.argument("level", IntegerArgumentType.integer(1, 10))
                                       .executes(ctx -> each(ctx, "level", p -> HakiManager.setLevel(p, IntegerArgumentType.getInteger(ctx, "level"))))
                                 )
                           )
                     ))
                  .then(
                     Commands.literal("xp")
                        .then(
                           Commands.argument("targets", EntityArgument.players())
                              .then(Commands.argument("amount", FloatArgumentType.floatArg(0.0F)).executes(ctx -> each(ctx, "xp", p -> {
                                    HakiManager.unlock(p);
                                    HakiManager.addXp(p, FloatArgumentType.getFloat(ctx, "amount"));
                                 })))
                        )
                  ))
               .then(
                  Commands.literal("reset")
                     .then(Commands.argument("targets", EntityArgument.players()).executes(ctx -> each(ctx, "reset", HakiManager::reset)))
               ))
            .then(
               Commands.literal("info")
                  .then(
                     Commands.argument("targets", EntityArgument.players())
                        .executes(
                           ctx -> {
                              for (ServerPlayer p : EntityArgument.getPlayers(ctx, "targets")) {
                                 PlayerHakiData d = HakiManager.data(p);
                                 ((CommandSourceStack)ctx.getSource())
                                    .sendSuccess(
                                       () -> Component.translatable(
                                             "commands.devilfruits.haki.info",
                                             new Object[]{
                                                p.getDisplayName(),
                                                d.armament(),
                                                d.level(),
                                                String.format("%.0f/%.0f", d.xp(), HakiManager.xpToNext(d.level())),
                                                String.format("%.0f", Math.max(0.0F, d.spirit())),
                                                Integer.toBinaryString(d.mask())
                                             }
                                          ),
                                       false
                                    );
                              }

                              return 1;
                           }
                        )
                  )
            )
      );
   }

   private static int each(CommandContext<CommandSourceStack> ctx, String what, Consumer<ServerPlayer> action) throws CommandSyntaxException {
      Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");

      for (ServerPlayer p : targets) {
         action.accept(p);
      }

      ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.translatable("commands.devilfruits.haki." + what, new Object[]{targets.size()}), true);
      return targets.size();
   }

   private HakiCommand() {
   }
}
