package com.awakenedmod.awakened.core.command;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.team.TeamActions;
import com.awakenedmod.awakened.core.team.TeamService;
import com.awakenedmod.awakened.core.team.Teams;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class TeamAdminCommand {
   private TeamAdminCommand() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(TeamAdminCommand::onRegister);
   }

   private static void onRegister(RegisterCommandsEvent event) {
      register(event.getDispatcher());
   }

   private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)Commands.literal("awakened")
            .then(
               ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal(
                                       "teamadmin"
                                    )
                                    .requires(RealisticMovementCommand::isOp))
                                 .then(
                                    Commands.literal("list")
                                       .then(
                                          Commands.argument("player", EntityArgument.player())
                                             .executes(ctx -> list((CommandSourceStack)ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))
                                       )
                                 ))
                              .then(
                                 Commands.literal("promote")
                                    .then(
                                       Commands.argument("player", EntityArgument.player())
                                          .executes(ctx -> promote((CommandSourceStack)ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))
                                    )
                              ))
                           .then(
                              Commands.literal("remove")
                                 .then(
                                    Commands.argument("player", EntityArgument.player())
                                       .executes(ctx -> remove((CommandSourceStack)ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))
                                 )
                           ))
                        .then(
                           Commands.literal("disband")
                              .then(
                                 Commands.argument("player", EntityArgument.player())
                                    .executes(ctx -> disband((CommandSourceStack)ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))
                              )
                        ))
                     .then(
                        Commands.literal("ff")
                           .then(
                              ((RequiredArgumentBuilder)Commands.argument("player", EntityArgument.player())
                                    .then(
                                       Commands.literal("on")
                                          .executes(ctx -> ff((CommandSourceStack)ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), true))
                                    ))
                                 .then(
                                    Commands.literal("off")
                                       .executes(ctx -> ff((CommandSourceStack)ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), false))
                                 )
                           )
                     ))
                  .then(
                     ((LiteralArgumentBuilder)Commands.literal("why").executes(ctx -> whyLookedAt((CommandSourceStack)ctx.getSource())))
                        .then(
                           Commands.argument("attacker", EntityArgument.entity())
                              .then(
                                 Commands.argument("victim", EntityArgument.entity())
                                    .executes(
                                       ctx -> why(
                                             (CommandSourceStack)ctx.getSource(),
                                             EntityArgument.getEntity(ctx, "attacker"),
                                             EntityArgument.getEntity(ctx, "victim")
                                          )
                                    )
                              )
                        )
                  )
            )
      );
   }

   private static int list(CommandSourceStack source, ServerPlayer target) {
      MinecraftServer server = source.getServer();
      UUID teamId = TeamService.teamOf(server, target.getUUID());
      if (teamId == null) {
         source.sendFailure(Component.translatable("commands.awakened.teamadmin.noteam", new Object[]{target.getName().getString()}));
         return 0;
      } else {
         UUID leader = TeamService.leaderOf(server, teamId);
         boolean ff = TeamService.friendlyFire(server, teamId);
         List<UUID> members = TeamService.membersOf(server, teamId);
         source.sendSuccess(
            () -> Component.literal("Team " + teamId + "  |  friendly fire ")
                  .withStyle(ChatFormatting.GRAY)
                  .append(Component.literal(ff ? "ON" : "OFF").withStyle(ff ? ChatFormatting.RED : ChatFormatting.GREEN)),
            false
         );

         for (UUID member : members) {
            boolean online = server.getPlayerList().getPlayer(member) != null;
            MutableComponent row = Component.literal(
                  "  " + (member.equals(leader) ? "★ " : "  ") + TeamService.nameOf(server, member) + (online ? "" : " (offline)")
               )
               .withStyle(online ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY);
            source.sendSuccess(() -> row, false);
         }

         return 1;
      }
   }

   private static int promote(CommandSourceStack source, ServerPlayer target) {
      MinecraftServer server = source.getServer();
      UUID teamId = TeamService.teamOf(server, target.getUUID());
      return report(
         source,
         teamId != null && TeamActions.forceLeader(server, teamId, target.getUUID()),
         "commands.awakened.teamadmin.promoted",
         target.getName().getString()
      );
   }

   private static int remove(CommandSourceStack source, ServerPlayer target) {
      return report(source, TeamActions.forceRemove(source.getServer(), target.getUUID()), "commands.awakened.teamadmin.removed", target.getName().getString());
   }

   private static int disband(CommandSourceStack source, ServerPlayer target) {
      MinecraftServer server = source.getServer();
      UUID teamId = TeamService.teamOf(server, target.getUUID());
      return report(source, teamId != null && TeamActions.forceDisband(server, teamId), "commands.awakened.teamadmin.disbanded", target.getName().getString());
   }

   private static int ff(CommandSourceStack source, ServerPlayer target, boolean on) {
      MinecraftServer server = source.getServer();
      UUID teamId = TeamService.teamOf(server, target.getUUID());
      return report(
         source, teamId != null && TeamActions.forceFriendlyFire(server, teamId, on), on ? "commands.awakened.team.ff.on" : "commands.awakened.team.ff.off"
      );
   }

   private static int report(CommandSourceStack source, boolean ok, String key, Object... args) {
      if (ok) {
         source.sendSuccess(() -> Component.translatable(key, args), true);
         return 1;
      } else {
         source.sendFailure(Component.translatable("commands.awakened.teamadmin.failed"));
         return 0;
      }
   }

   private static int whyLookedAt(CommandSourceStack source) throws CommandSyntaxException {
      ServerPlayer self = source.getPlayerOrException();
      LivingEntity looked = AimUtil.aimedLivingEntity(self, 32.0);
      if (looked == null) {
         source.sendFailure(Component.translatable("commands.awakened.teamadmin.why.nothing"));
         return 0;
      } else {
         return why(source, self, looked);
      }
   }

   private static int why(CommandSourceStack source, Entity attacker, Entity victim) {
      MinecraftServer server = source.getServer();
      Teams.Explanation x = Teams.explain(attacker, victim);
      source.sendSuccess(
         () -> Component.literal(name(attacker) + " → " + name(victim) + ": ")
               .withStyle(ChatFormatting.GRAY)
               .append(Component.literal(x.forbidden() ? "BLOCKED" : "ALLOWED").withStyle(x.forbidden() ? ChatFormatting.GREEN : ChatFormatting.RED)),
         false
      );
      side(source, server, "attacker", x.attackerOwner(), x.attackerVia(), attacker);
      side(source, server, "victim  ", x.victimOwner(), x.victimVia(), victim);
      if (x.attackerOwner() != null && x.victimOwner() != null) {
         if (x.attackerOwner().equals(x.victimOwner())) {
            line(source, "  both resolve to the same player, and self-harm is never blocked here", ChatFormatting.DARK_GRAY);
         } else if (!x.sameTeam()) {
            line(source, "  not on the same team", ChatFormatting.DARK_GRAY);
         } else {
            line(source, "  same team, friendly fire " + (x.friendlyFire() ? "ON" : "OFF"), ChatFormatting.DARK_GRAY);
         }
      }

      return 1;
   }

   private static void side(CommandSourceStack source, MinecraftServer server, String label, UUID owner, String via, Entity entity) {
      if (owner == null) {
         line(
            source, "  " + label + " resolves to nobody, no resolver claims " + entity.getType().toShortString() + ", so harm is allowed", ChatFormatting.GOLD
         );
      } else {
         line(
            source,
            "  " + label + " resolves to " + TeamService.nameOf(server, owner) + "  (resolver: " + (via == null ? "?" : via) + ")",
            ChatFormatting.DARK_GRAY
         );
      }
   }

   private static void line(CommandSourceStack source, String text, ChatFormatting colour) {
      source.sendSuccess(() -> Component.literal(text).withStyle(colour), false);
   }

   private static String name(Entity entity) {
      return entity == null ? "nothing" : entity.getName().getString();
   }
}
