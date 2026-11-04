package com.devilfruits.fruits.yomi_yomi;

import com.devilfruits.fruits.yomi_yomi.soul.SoulPhase;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Arrays;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class YomiCommand {
   public static LiteralArgumentBuilder<CommandSourceStack> node() {
      return (LiteralArgumentBuilder<CommandSourceStack>)Commands.literal("yomi")
         .then(
            Commands.literal("vessel")
               .then(
                  Commands.argument("targets", EntityArgument.players())
                     .then(
                        Commands.argument("vessel", StringArgumentType.word())
                           .suggests((ctx, b) -> SharedSuggestionProvider.suggest(Arrays.stream(Vessel.values()).map(Vessel::formName), b))
                           .executes(
                              ctx -> vessel(
                                    (CommandSourceStack)ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), StringArgumentType.getString(ctx, "vessel")
                                 )
                           )
                     )
               )
         );
   }

   private static int vessel(CommandSourceStack source, Collection<ServerPlayer> targets, String name) {
      Vessel vessel = Vessel.byName(name);
      if (vessel == null) {
         source.sendFailure(Component.translatable("commands.devilfruits.yomi_yomi.vessel.unknown", new Object[]{name}));
         return 0;
      } else {
         int changed = 0;

         for (ServerPlayer player : targets) {
            if (!YomiYomi.isUser(player)) {
               source.sendFailure(Component.translatable("commands.devilfruits.yomi_yomi.vessel.not_user", new Object[]{player.getDisplayName()}));
            } else {
               SoulPhase.becomeVessel(player, vessel);
               changed++;
            }
         }

         int count = changed;
         source.sendSuccess(() -> Component.translatable("commands.devilfruits.yomi_yomi.vessel", new Object[]{vessel.displayName(), count}), true);
         return count;
      }
   }

   private YomiCommand() {
   }
}
