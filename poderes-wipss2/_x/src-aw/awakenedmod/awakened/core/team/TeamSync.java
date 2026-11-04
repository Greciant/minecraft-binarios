package com.awakenedmod.awakened.core.team;

import com.awakenedmod.awakened.Config;
import com.awakenedmod.awakened.core.net.SyncTeamPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class TeamSync {
   private TeamSync() {
   }

   public static void send(ServerPlayer player) {
      MinecraftServer server = player.serverLevel().getServer();
      if (server != null) {
         if (!(Boolean)Config.TEAMS_ENABLED.get()) {
            PacketDistributor.sendToPlayer(player, SyncTeamPayload.OFF, new CustomPacketPayload[0]);
         } else {
            List<String> invites = new ArrayList<>();

            for (UUID inviter : TeamService.pendingInviters(player.getUUID())) {
               invites.add(TeamActions.nameOf(server, inviter));
            }

            UUID teamId = TeamService.teamOf(server, player.getUUID());
            if (teamId == null) {
               PacketDistributor.sendToPlayer(player, SyncTeamPayload.none(invites), new CustomPacketPayload[0]);
            } else {
               UUID leader = TeamService.leaderOf(server, teamId);
               List<SyncTeamPayload.Member> roster = new ArrayList<>();

               for (UUID member : TeamService.membersOf(server, teamId)) {
                  roster.add(
                     new SyncTeamPayload.Member(
                        TeamService.nameOf(server, member), member, member.equals(leader), server.getPlayerList().getPlayer(member) != null
                     )
                  );
               }

               PacketDistributor.sendToPlayer(
                  player,
                  new SyncTeamPayload(true, true, TeamService.friendlyFire(server, teamId), player.getUUID().equals(leader), roster, invites),
                  new CustomPacketPayload[0]
               );
            }
         }
      }
   }

   public static void sendToTeam(MinecraftServer server, UUID teamId) {
      if (server != null && teamId != null) {
         for (UUID member : TeamService.membersOf(server, teamId)) {
            ServerPlayer online = server.getPlayerList().getPlayer(member);
            if (online != null) {
               send(online);
            }
         }
      }
   }

   public static void handle(ServerPlayer player, int action, String arg) {
      if (player != null && (Boolean)Config.TEAMS_ENABLED.get()) {
         TeamActions.Result result = switch (action) {
            case 0 -> TeamActions.kick(player, arg);
            case 1 -> TeamActions.leave(player);
            case 2 -> TeamActions.disband(player);
            case 3 -> TeamActions.setFriendlyFire(player, true);
            case 4 -> TeamActions.setFriendlyFire(player, false);
            case 5 -> invite(player, arg);
            case 6 -> answer(player, arg, true);
            case 7 -> answer(player, arg, false);
            default -> null;
         };
         if (result != null) {
            if (!result.ok()) {
               player.sendSystemMessage(Component.translatable(result.key(), result.args()));
            }

            send(player);
         }
      }
   }

   private static TeamActions.Result invite(ServerPlayer self, String name) {
      ServerPlayer target = self.level().getServer().getPlayerList().getPlayerByName(name);
      return target == null ? TeamActions.Result.fail("commands.awakened.team.invite.offline", name) : TeamActions.invite(self, target);
   }

   private static TeamActions.Result answer(ServerPlayer self, String name, boolean accept) {
      if (name.isEmpty()) {
         return accept ? TeamActions.accept(self) : TeamActions.deny(self);
      } else {
         ServerPlayer inviter = self.level().getServer().getPlayerList().getPlayerByName(name);
         if (inviter == null) {
            return TeamActions.Result.fail("commands.awakened.team.accept.none", name);
         } else {
            return accept ? TeamActions.accept(self, inviter.getUUID()) : TeamActions.deny(self, inviter.getUUID());
         }
      }
   }
}
