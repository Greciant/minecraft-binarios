package com.awakenedmod.awakened.core.command;

import com.awakenedmod.awakened.Config;
import com.awakenedmod.awakened.core.team.TeamActions;
import com.awakenedmod.awakened.core.team.TeamService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class TeamCommand {
   private static final SuggestionProvider<CommandSourceStack> SUGGEST_TEAMMATES = (ctx, builder) -> {
      ServerPlayer self = ((CommandSourceStack)ctx.getSource()).getPlayer();
      if (self == null) {
         return builder.buildFuture();
      } else {
         MinecraftServer server = ((CommandSourceStack)ctx.getSource()).getServer();
         UUID teamId = TeamService.teamOf(server, self.getUUID());
         return SharedSuggestionProvider.suggest(
            TeamService.membersOf(server, teamId).stream().filter(member -> !member.equals(self.getUUID())).map(member -> TeamService.nameOf(server, member)),
            builder
         );
      }
   };
   private static final SimpleCommandExceptionType DISABLED = new SimpleCommandExceptionType(Component.translatable("commands.awakened.team.disabled"));
   private static final String[] HELP_KEYS = new String[]{
      "commands.awakened.team.help.invite",
      "commands.awakened.team.help.answer",
      "commands.awakened.team.help.kick",
      "commands.awakened.team.help.leave",
      "commands.awakened.team.help.promote",
      "commands.awakened.team.help.list",
      "commands.awakened.team.help.friendlyfire"
   };

   private TeamCommand() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(TeamCommand::onRegister);
   }

   private static void onRegister(RegisterCommandsEvent event) {
      register(event.getDispatcher());
   }

   private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)Commands.literal("awakened")
            .then(
               ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal(
                                                   "team"
                                                )
                                                .executes(ctx -> help((CommandSourceStack)ctx.getSource())))
                                             .then(
                                                Commands.literal("invite")
                                                   .then(
                                                      Commands.argument("player", EntityArgument.player())
                                                         .executes(
                                                            ctx -> report(
                                                                  (CommandSourceStack)ctx.getSource(),
                                                                  TeamActions.invite(
                                                                     self((CommandSourceStack)ctx.getSource()), EntityArgument.getPlayer(ctx, "player")
                                                                  )
                                                               )
                                                         )
                                                   )
                                             ))
                                          .then(
                                             ((LiteralArgumentBuilder)Commands.literal("accept")
                                                   .executes(
                                                      ctx -> report(
                                                            (CommandSourceStack)ctx.getSource(), TeamActions.accept(self((CommandSourceStack)ctx.getSource()))
                                                         )
                                                   ))
                                                .then(
                                                   Commands.argument("player", EntityArgument.player())
                                                      .executes(
                                                         ctx -> report(
                                                               (CommandSourceStack)ctx.getSource(),
                                                               TeamActions.accept(
                                                                  self((CommandSourceStack)ctx.getSource()), EntityArgument.getPlayer(ctx, "player").getUUID()
                                                               )
                                                            )
                                                      )
                                                )
                                          ))
                                       .then(
                                          ((LiteralArgumentBuilder)Commands.literal("deny")
                                                .executes(
                                                   ctx -> report(
                                                         (CommandSourceStack)ctx.getSource(), TeamActions.deny(self((CommandSourceStack)ctx.getSource()))
                                                      )
                                                ))
                                             .then(
                                                Commands.argument("player", EntityArgument.player())
                                                   .executes(
                                                      ctx -> report(
                                                            (CommandSourceStack)ctx.getSource(),
                                                            TeamActions.deny(
                                                               self((CommandSourceStack)ctx.getSource()), EntityArgument.getPlayer(ctx, "player").getUUID()
                                                            )
                                                         )
                                                   )
                                             )
                                       ))
                                    .then(
                                       Commands.literal("kick")
                                          .then(
                                             Commands.argument("name", StringArgumentType.word())
                                                .suggests(SUGGEST_TEAMMATES)
                                                .executes(
                                                   ctx -> report(
                                                         (CommandSourceStack)ctx.getSource(),
                                                         TeamActions.kick(self((CommandSourceStack)ctx.getSource()), StringArgumentType.getString(ctx, "name"))
                                                      )
                                                )
                                          )
                                    ))
                                 .then(
                                    Commands.literal("promote")
                                       .then(
                                          Commands.argument("name", StringArgumentType.word())
                                             .suggests(SUGGEST_TEAMMATES)
                                             .executes(
                                                ctx -> report(
                                                      (CommandSourceStack)ctx.getSource(),
                                                      TeamActions.promote(self((CommandSourceStack)ctx.getSource()), StringArgumentType.getString(ctx, "name"))
                                                   )
                                             )
                                       )
                                 ))
                              .then(
                                 Commands.literal("claim")
                                    .executes(ctx -> report((CommandSourceStack)ctx.getSource(), TeamActions.claim(self((CommandSourceStack)ctx.getSource()))))
                              ))
                           .then(
                              Commands.literal("leave")
                                 .executes(ctx -> report((CommandSourceStack)ctx.getSource(), TeamActions.leave(self((CommandSourceStack)ctx.getSource()))))
                           ))
                        .then(
                           Commands.literal("disband")
                              .executes(ctx -> report((CommandSourceStack)ctx.getSource(), TeamActions.disband(self((CommandSourceStack)ctx.getSource()))))
                        ))
                     .then(Commands.literal("list").executes(ctx -> list((CommandSourceStack)ctx.getSource()))))
                  .then(
                     ((LiteralArgumentBuilder)Commands.literal("friendlyfire")
                           .then(
                              Commands.literal("on")
                                 .executes(
                                    ctx -> report(
                                          (CommandSourceStack)ctx.getSource(), TeamActions.setFriendlyFire(self((CommandSourceStack)ctx.getSource()), true)
                                       )
                                 )
                           ))
                        .then(
                           Commands.literal("off")
                              .executes(
                                 ctx -> report(
                                       (CommandSourceStack)ctx.getSource(), TeamActions.setFriendlyFire(self((CommandSourceStack)ctx.getSource()), false)
                                    )
                              )
                        )
                  )
            )
      );
   }

   private static ServerPlayer self(CommandSourceStack source) throws CommandSyntaxException {
      if (!(Boolean)Config.TEAMS_ENABLED.get()) {
         throw DISABLED.create();
      } else {
         return source.getPlayerOrException();
      }
   }

   private static int report(CommandSourceStack source, TeamActions.Result result) {
      Component message = Component.translatable(result.key(), result.args());
      if (result.ok()) {
         source.sendSuccess(() -> message, false);
         return 1;
      } else {
         source.sendFailure(message);
         return 0;
      }
   }

   private static int help(CommandSourceStack source) {
      if (!(Boolean)Config.TEAMS_ENABLED.get()) {
         source.sendFailure(Component.translatable("commands.awakened.team.disabled"));
         return 0;
      } else {
         source.sendSuccess(() -> Component.translatable("commands.awakened.team.help.header").withStyle(ChatFormatting.GOLD), false);

         for (String key : HELP_KEYS) {
            source.sendSuccess(() -> Component.translatable(key).withStyle(ChatFormatting.GRAY), false);
         }

         return 1;
      }
   }

   private static int list(CommandSourceStack source) throws CommandSyntaxException {
      ServerPlayer self = self(source);
      MinecraftServer server = source.getServer();
      UUID teamId = TeamService.teamOf(server, self.getUUID());
      if (teamId == null) {
         source.sendFailure(Component.translatable("commands.awakened.team.none"));
         return 0;
      } else {
         List<UUID> members = TeamService.membersOf(server, teamId);
         UUID leader = TeamService.leaderOf(server, teamId);
         boolean ff = TeamService.friendlyFire(server, teamId);
         MutableComponent header = Component.literal("Team (" + members.size() + "/" + Config.TEAM_MAX_MEMBERS.get() + ")  |  friendly fire ")
            .withStyle(ChatFormatting.GRAY)
            .append(Component.literal(ff ? "ON" : "OFF").withStyle(ff ? ChatFormatting.RED : ChatFormatting.GREEN));
         source.sendSuccess(() -> header, false);

         for (UUID member : members) {
            String name = TeamService.nameOf(server, member);
            boolean online = server.getPlayerList().getPlayer(member) != null;
            MutableComponent row = Component.literal("  " + (member.equals(leader) ? "★ " : "  ") + name)
               .withStyle(online ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY);
            if (!online) {
               row.append(Component.literal(" (offline)").withStyle(ChatFormatting.DARK_GRAY));
            }

            source.sendSuccess(() -> row, false);
         }

         return 1;
      }
   }
}
