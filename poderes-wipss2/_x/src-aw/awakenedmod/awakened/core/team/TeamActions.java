package com.awakenedmod.awakened.core.team;

import com.awakenedmod.awakened.Awakened;
import com.awakenedmod.awakened.Config;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class TeamActions {
   private TeamActions() {
   }

   public static TeamActions.Result invite(ServerPlayer self, ServerPlayer target) {
      MinecraftServer server = server(self);
      if (target.getUUID().equals(self.getUUID())) {
         return TeamActions.Result.fail("commands.awakened.team.invite.self");
      } else if (TeamService.teamOf(server, target.getUUID()) != null) {
         return TeamActions.Result.fail("commands.awakened.team.invite.already", target.getName().getString());
      } else {
         UUID teamId = TeamService.teamOf(server, self.getUUID());
         if (teamId != null && TeamService.membersOf(server, teamId).size() >= (Integer)Config.TEAM_MAX_MEMBERS.get()) {
            return TeamActions.Result.fail("commands.awakened.team.full", Config.TEAM_MAX_MEMBERS.get());
         } else {
            if (teamId == null) {
               teamId = TeamService.createTeam(server, self);
               sync(server, teamId);
            }

            TeamService.invite(self.getUUID(), target.getUUID());
            TeamChat.sendInvite(self, target);
            TeamSync.send(target);
            return TeamActions.Result.ok("commands.awakened.team.invite.sent", target.getName().getString());
         }
      }
   }

   public static TeamActions.Result accept(ServerPlayer self, UUID inviter) {
      MinecraftServer server = server(self);
      if (!TeamService.hasInvite(self.getUUID(), inviter)) {
         return noLiveInvite(server, self, inviter);
      } else {
         UUID teamId = TeamService.teamOf(server, inviter);
         if (teamId == null) {
            return TeamActions.Result.fail("commands.awakened.team.accept.gone");
         } else if (TeamService.teamOf(server, self.getUUID()) != null) {
            return TeamActions.Result.fail("commands.awakened.team.accept.already");
         } else if (TeamService.membersOf(server, teamId).size() >= (Integer)Config.TEAM_MAX_MEMBERS.get()) {
            return TeamActions.Result.fail("commands.awakened.team.full", Config.TEAM_MAX_MEMBERS.get());
         } else {
            TeamService.consumeInvite(self.getUUID(), inviter);
            if (!TeamService.join(server, self, teamId)) {
               return TeamActions.Result.fail("commands.awakened.team.full", Config.TEAM_MAX_MEMBERS.get());
            } else {
               TeamChat.broadcast(server, teamId, self.getUUID(), TeamChat.note("awakened.team.joined", self.getName().getString()));
               log(teamId, "{} joined", self);
               sync(server, teamId);
               return TeamActions.Result.ok("commands.awakened.team.accept.joined");
            }
         }
      }
   }

   public static TeamActions.Result deny(ServerPlayer self, UUID inviter) {
      MinecraftServer server = server(self);
      if (!TeamService.consumeInvite(self.getUUID(), inviter)) {
         return noLiveInvite(server, self, inviter);
      } else {
         String name = nameOf(server, inviter);
         ServerPlayer online = server.getPlayerList().getPlayer(inviter);
         if (online != null) {
            TeamChat.tell(online, "awakened.team.denied", self.getName().getString());
         }

         TeamSync.send(self);
         return TeamActions.Result.ok("commands.awakened.team.deny.done", name);
      }
   }

   public static TeamActions.Result accept(ServerPlayer self) {
      return answerSole(self, TeamActions::accept);
   }

   public static TeamActions.Result deny(ServerPlayer self) {
      return answerSole(self, TeamActions::deny);
   }

   private static TeamActions.Result answerSole(ServerPlayer self, BiFunction<ServerPlayer, UUID, TeamActions.Result> choice) {
      MinecraftServer server = server(self);
      List<UUID> live = TeamService.pendingInviters(self.getUUID());
      if (live.size() == 1) {
         return choice.apply(self, live.get(0));
      } else if (live.size() > 1) {
         TeamChat.sendInvitePicker(server, self, live);
         return TeamActions.Result.fail("commands.awakened.team.accept.many", live.size());
      } else {
         List<UUID> lapsed = TeamService.expiredInviters(self.getUUID());
         return !lapsed.isEmpty()
            ? TeamActions.Result.fail("commands.awakened.team.accept.expired", nameOf(server, lapsed.get(0)))
            : TeamActions.Result.fail("commands.awakened.team.accept.nothing");
      }
   }

   private static TeamActions.Result noLiveInvite(MinecraftServer server, ServerPlayer self, UUID inviter) {
      String name = nameOf(server, inviter);
      return TeamService.inviteStatus(self.getUUID(), inviter) == TeamService.InviteStatus.EXPIRED
         ? TeamActions.Result.fail("commands.awakened.team.accept.expired", name)
         : TeamActions.Result.fail("commands.awakened.team.accept.none", name);
   }

   public static String nameOf(MinecraftServer server, UUID player) {
      ServerPlayer online = server == null ? null : server.getPlayerList().getPlayer(player);
      return online != null ? online.getName().getString() : TeamService.nameOf(server, player);
   }

   private static void voidInvitesFrom(MinecraftServer server, UUID inviter) {
      String name = nameOf(server, inviter);

      for (UUID invitee : TeamService.cancelInvitesFrom(inviter)) {
         ServerPlayer online = server.getPlayerList().getPlayer(invitee);
         if (online != null) {
            TeamChat.tell(online, "awakened.team.invite_void", name);
            TeamSync.send(online);
         }
      }
   }

   public static void tickInvites(MinecraftServer server) {
      for (TeamService.Lapsed lapsed : TeamService.sweepInvites()) {
         ServerPlayer invitee = server.getPlayerList().getPlayer(lapsed.invitee());
         ServerPlayer inviter = server.getPlayerList().getPlayer(lapsed.inviter());
         if (invitee != null) {
            TeamChat.tell(invitee, "awakened.team.invite_expired", nameOf(server, lapsed.inviter()));
            TeamSync.send(invitee);
         }

         if (inviter != null) {
            TeamChat.tell(inviter, "awakened.team.invite_expired_sent", nameOf(server, lapsed.invitee()));
         }
      }
   }

   public static void inviteesForget(ServerPlayer player) {
      MinecraftServer server = server(player);
      String name = player.getName().getString();
      List<ServerPlayer> holders = new ArrayList<>();

      for (UUID invitee : TeamService.inviteesOf(player.getUUID())) {
         ServerPlayer online = server.getPlayerList().getPlayer(invitee);
         if (online != null) {
            TeamChat.tell(online, "awakened.team.invite_gone", name);
            holders.add(online);
         }
      }

      for (UUID inviter : TeamService.pendingInviters(player.getUUID())) {
         ServerPlayer online = server.getPlayerList().getPlayer(inviter);
         if (online != null) {
            TeamChat.tell(online, "awakened.team.invite_gone_sent", name);
         }
      }

      TeamService.forget(player.getUUID());
      holders.forEach(TeamSync::send);
   }

   public static TeamActions.Result kick(ServerPlayer self, String name) {
      MinecraftServer server = server(self);
      UUID teamId = TeamService.teamOf(server, self.getUUID());
      if (teamId == null) {
         return TeamActions.Result.fail("commands.awakened.team.none");
      } else if (!TeamService.isLeader(server, self.getUUID())) {
         return TeamActions.Result.fail("commands.awakened.team.notleader");
      } else {
         UUID target = memberNamed(server, teamId, name);
         if (target == null) {
            return TeamActions.Result.fail("commands.awakened.team.kick.notmember", name);
         } else if (target.equals(self.getUUID())) {
            return TeamActions.Result.fail("commands.awakened.team.kick.self");
         } else if (!TeamService.kick(server, self.getUUID(), target)) {
            return TeamActions.Result.fail("commands.awakened.team.kick.notmember", name);
         } else {
            voidInvitesFrom(server, target);
            TeamChat.broadcast(server, teamId, null, TeamChat.note("awakened.team.kicked", name));
            ServerPlayer online = server.getPlayerList().getPlayer(target);
            if (online != null) {
               TeamChat.tell(online, "awakened.team.you_kicked");
               TeamSync.send(online);
            }

            log(teamId, "{} kicked " + name, self);
            sync(server, teamId);
            return TeamActions.Result.ok("commands.awakened.team.kick.done", name);
         }
      }
   }

   public static TeamActions.Result leave(ServerPlayer self) {
      MinecraftServer server = server(self);
      UUID teamId = TeamService.leave(server, self.getUUID());
      if (teamId == null) {
         return TeamActions.Result.fail("commands.awakened.team.none");
      } else {
         TeamChat.broadcast(server, teamId, null, TeamChat.note("awakened.team.left", self.getName().getString()));
         voidInvitesFrom(server, self.getUUID());
         sync(server, teamId);
         log(teamId, "{} left", self);
         TeamSync.send(self);
         return TeamActions.Result.ok("commands.awakened.team.leave.done");
      }
   }

   public static TeamActions.Result disband(ServerPlayer self) {
      MinecraftServer server = server(self);
      UUID teamId = TeamService.teamOf(server, self.getUUID());
      if (teamId == null) {
         return TeamActions.Result.fail("commands.awakened.team.none");
      } else if (!TeamService.isLeader(server, self.getUUID())) {
         return TeamActions.Result.fail("commands.awakened.team.notleader");
      } else {
         TeamChat.broadcast(server, teamId, self.getUUID(), TeamChat.note("awakened.team.disbanded"));
         List<UUID> former = TeamService.membersOf(server, teamId);
         TeamService.disband(server, self.getUUID());

         for (UUID member : former) {
            voidInvitesFrom(server, member);
            ServerPlayer online = server.getPlayerList().getPlayer(member);
            if (online != null) {
               TeamSync.send(online);
            }
         }

         log(teamId, "{} disbanded it", self);
         return TeamActions.Result.ok("commands.awakened.team.disband.done");
      }
   }

   public static TeamActions.Result setFriendlyFire(ServerPlayer self, boolean on) {
      MinecraftServer server = server(self);
      UUID teamId = TeamService.teamOf(server, self.getUUID());
      if (teamId == null) {
         return TeamActions.Result.fail("commands.awakened.team.none");
      } else if (!TeamService.setFriendlyFire(server, self.getUUID(), on)) {
         return TeamActions.Result.fail("commands.awakened.team.notleader");
      } else {
         TeamChat.broadcast(server, teamId, null, TeamChat.note(on ? "awakened.team.ff_on" : "awakened.team.ff_off"));
         log(teamId, "{} set friendlyFire=" + on, self);
         sync(server, teamId);
         return TeamActions.Result.ok(on ? "commands.awakened.team.ff.on" : "commands.awakened.team.ff.off");
      }
   }

   public static TeamActions.Result promote(ServerPlayer self, String name) {
      MinecraftServer server = server(self);
      UUID teamId = TeamService.teamOf(server, self.getUUID());
      if (teamId == null) {
         return TeamActions.Result.fail("commands.awakened.team.none");
      } else if (!TeamService.isLeader(server, self.getUUID())) {
         return TeamActions.Result.fail("commands.awakened.team.notleader");
      } else {
         UUID target = memberNamed(server, teamId, name);
         if (target == null || target.equals(self.getUUID())) {
            return TeamActions.Result.fail("commands.awakened.team.kick.notmember", name);
         } else if (!TeamService.setLeader(server, teamId, target)) {
            return TeamActions.Result.fail("commands.awakened.team.kick.notmember", name);
         } else {
            TeamChat.broadcast(server, teamId, null, TeamChat.note("awakened.team.leads", name));
            log(teamId, "{} promoted " + name, self);
            sync(server, teamId);
            return TeamActions.Result.ok("commands.awakened.team.promote.done", name);
         }
      }
   }

   public static TeamActions.Result claim(ServerPlayer self) {
      MinecraftServer server = server(self);
      UUID teamId = TeamService.teamOf(server, self.getUUID());
      if (teamId == null) {
         return TeamActions.Result.fail("commands.awakened.team.none");
      } else {
         UUID leader = TeamService.leaderOf(server, teamId);
         if (self.getUUID().equals(leader)) {
            return TeamActions.Result.fail("commands.awakened.team.claim.self");
         } else if (leader != null && server.getPlayerList().getPlayer(leader) != null) {
            return TeamActions.Result.fail("commands.awakened.team.claim.online", TeamService.nameOf(server, leader));
         } else if (!TeamService.setLeader(server, teamId, self.getUUID())) {
            return TeamActions.Result.fail("commands.awakened.team.none");
         } else {
            TeamChat.broadcast(server, teamId, null, TeamChat.note("awakened.team.leads", self.getName().getString()));
            log(teamId, "{} claimed leadership from an offline leader", self);
            sync(server, teamId);
            return TeamActions.Result.ok("commands.awakened.team.promote.done", self.getName().getString());
         }
      }
   }

   public static boolean forceFriendlyFire(MinecraftServer server, UUID teamId, boolean on) {
      UUID leader = TeamService.leaderOf(server, teamId);
      if (leader != null && TeamService.setFriendlyFire(server, leader, on)) {
         TeamChat.broadcast(server, teamId, null, TeamChat.note(on ? "awakened.team.ff_on" : "awakened.team.ff_off"));
         Awakened.LOGGER.info("Team {}: an operator set friendlyFire={}", teamId, on);
         sync(server, teamId);
         return true;
      } else {
         return false;
      }
   }

   public static boolean forceLeader(MinecraftServer server, UUID teamId, UUID newLeader) {
      if (!TeamService.setLeader(server, teamId, newLeader)) {
         return false;
      } else {
         String name = TeamService.nameOf(server, newLeader);
         TeamChat.broadcast(server, teamId, null, TeamChat.note("awakened.team.leads", name));
         Awakened.LOGGER.info("Team {}: an operator made {} leader", teamId, name);
         sync(server, teamId);
         return true;
      }
   }

   public static boolean forceRemove(MinecraftServer server, UUID player) {
      String name = TeamService.nameOf(server, player);
      UUID teamId = TeamService.leave(server, player);
      if (teamId == null) {
         return false;
      } else {
         TeamChat.broadcast(server, teamId, null, TeamChat.note("awakened.team.kicked", name));
         Awakened.LOGGER.info("Team {}: an operator removed {}", teamId, name);
         voidInvitesFrom(server, player);
         sync(server, teamId);
         ServerPlayer online = server.getPlayerList().getPlayer(player);
         if (online != null) {
            TeamChat.tell(online, "awakened.team.you_kicked");
            TeamSync.send(online);
         }

         return true;
      }
   }

   public static boolean forceDisband(MinecraftServer server, UUID teamId) {
      UUID leader = TeamService.leaderOf(server, teamId);
      if (leader == null) {
         return false;
      } else {
         TeamChat.broadcast(server, teamId, null, TeamChat.note("awakened.team.disbanded"));
         List<UUID> former = TeamService.membersOf(server, teamId);
         TeamService.disband(server, leader);
         Awakened.LOGGER.info("Team {}: an operator disbanded it", teamId);

         for (UUID member : former) {
            voidInvitesFrom(server, member);
            ServerPlayer online = server.getPlayerList().getPlayer(member);
            if (online != null) {
               TeamSync.send(online);
            }
         }

         return true;
      }
   }

   private static void log(UUID teamId, String what, ServerPlayer actor) {
      Awakened.LOGGER.info("Team {}: " + what, teamId, actor.getName().getString());
   }

   public static UUID memberNamed(MinecraftServer server, UUID teamId, String name) {
      for (UUID member : TeamService.membersOf(server, teamId)) {
         if (TeamService.nameOf(server, member).equalsIgnoreCase(name)) {
            return member;
         }
      }

      return null;
   }

   private static void sync(MinecraftServer server, UUID teamId) {
      TeamSync.sendToTeam(server, teamId);
   }

   private static MinecraftServer server(ServerPlayer player) {
      return player.serverLevel().getServer();
   }

   public static record Result(boolean ok, String key, Object... args) {
      static TeamActions.Result fail(String key, Object... args) {
         return new TeamActions.Result(false, key, args);
      }

      static TeamActions.Result ok(String key, Object... args) {
         return new TeamActions.Result(true, key, args);
      }
   }
}
