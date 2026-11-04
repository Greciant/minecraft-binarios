package com.awakenedmod.awakened.core.team;

import com.awakenedmod.awakened.Config;
import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedData.Factory;

public final class TeamService {
   private static final String DATA_ID = "awakened_teams";
   private static final Map<UUID, Map<UUID, TeamService.Invite>> INVITES = new ConcurrentHashMap<>();
   private static final long INVITE_GRACE_MILLIS = 300000L;

   private TeamService() {
   }

   private static TeamService.State state(MinecraftServer server) {
      return (TeamService.State)server.overworld()
         .getDataStorage()
         .computeIfAbsent(new Factory(TeamService.State::new, TeamService.State::load, null), "awakened_teams");
   }

   public static UUID teamOf(MinecraftServer server, UUID player) {
      if (server != null && player != null) {
         TeamService.Membership membership = state(server).members.get(player);
         return membership == null ? null : membership.teamId();
      } else {
         return null;
      }
   }

   public static UUID leaderOf(MinecraftServer server, UUID teamId) {
      if (server != null && teamId != null) {
         TeamService.TeamMeta meta = state(server).teams.get(teamId);
         return meta == null ? null : meta.leader();
      } else {
         return null;
      }
   }

   public static boolean isLeader(MinecraftServer server, UUID player) {
      UUID teamId = teamOf(server, player);
      return teamId != null && player.equals(leaderOf(server, teamId));
   }

   public static boolean friendlyFire(MinecraftServer server, UUID teamId) {
      if (server != null && teamId != null) {
         TeamService.TeamMeta meta = state(server).teams.get(teamId);
         return meta == null || meta.friendlyFire();
      } else {
         return true;
      }
   }

   public static List<UUID> membersOf(MinecraftServer server, UUID teamId) {
      if (server != null && teamId != null) {
         TeamService.State state = state(server);
         UUID leader = leaderOf(server, teamId);
         List<UUID> result = new ArrayList<>();
         state.members.forEach((uuid, membership) -> {
            if (teamId.equals(membership.teamId())) {
               result.add(uuid);
            }
         });
         result.sort((a, b) -> {
            if (a.equals(leader)) {
               return -1;
            } else {
               return b.equals(leader) ? 1 : nameOf(server, a).compareToIgnoreCase(nameOf(server, b));
            }
         });
         return result;
      } else {
         return List.of();
      }
   }

   private static String displayName(ServerPlayer player) {
      return player.getName().getString();
   }

   public static String nameOf(MinecraftServer server, UUID player) {
      if (server != null && player != null) {
         TeamService.Membership membership = state(server).members.get(player);
         return membership == null ? player.toString() : membership.lastKnownName();
      } else {
         return "?";
      }
   }

   public static boolean sameTeam(MinecraftServer server, UUID a, UUID b) {
      if (a != null && b != null) {
         UUID teamA = teamOf(server, a);
         return teamA != null && teamA.equals(teamOf(server, b));
      } else {
         return false;
      }
   }

   public static boolean friendlyFireBlocked(MinecraftServer server, UUID a, UUID b) {
      if (a != null && b != null && !a.equals(b)) {
         UUID teamId = teamOf(server, a);
         return teamId != null && teamId.equals(teamOf(server, b)) && !friendlyFire(server, teamId);
      } else {
         return false;
      }
   }

   public static UUID createTeam(MinecraftServer server, ServerPlayer leader) {
      UUID existing = teamOf(server, leader.getUUID());
      if (existing != null) {
         return existing;
      } else {
         TeamService.State state = state(server);
         UUID teamId = UUID.randomUUID();
         state.teams.put(teamId, new TeamService.TeamMeta(leader.getUUID(), (Boolean)Config.TEAM_FRIENDLY_FIRE_DEFAULT.get()));
         state.members.put(leader.getUUID(), new TeamService.Membership(teamId, displayName(leader)));
         state.setDirty();
         return teamId;
      }
   }

   public static boolean join(MinecraftServer server, ServerPlayer joiner, UUID teamId) {
      TeamService.State state = state(server);
      if (teamId != null
         && state.teams.containsKey(teamId)
         && !state.members.containsKey(joiner.getUUID())
         && membersOf(server, teamId).size() < (Integer)Config.TEAM_MAX_MEMBERS.get()) {
         state.members.put(joiner.getUUID(), new TeamService.Membership(teamId, displayName(joiner)));
         state.setDirty();
         return true;
      } else {
         return false;
      }
   }

   public static UUID leave(MinecraftServer server, UUID player) {
      TeamService.State state = state(server);
      TeamService.Membership membership = state.members.remove(player);
      if (membership == null) {
         return null;
      } else {
         UUID teamId = membership.teamId();
         List<UUID> remaining = membersOf(server, teamId);
         if (remaining.isEmpty()) {
            state.teams.remove(teamId);
         } else if (player.equals(leaderOf(server, teamId))) {
            TeamService.TeamMeta old = state.teams.get(teamId);
            state.teams.put(teamId, new TeamService.TeamMeta(successor(server, remaining), old != null && old.friendlyFire()));
         }

         state.setDirty();
         return teamId;
      }
   }

   private static UUID successor(MinecraftServer server, List<UUID> remaining) {
      for (UUID candidate : remaining) {
         if (server.getPlayerList().getPlayer(candidate) != null) {
            return candidate;
         }
      }

      return remaining.get(0);
   }

   public static boolean kick(MinecraftServer server, UUID leader, UUID target) {
      return isLeader(server, leader) && !leader.equals(target) && sameTeam(server, leader, target) ? leave(server, target) != null : false;
   }

   public static boolean setLeader(MinecraftServer server, UUID teamId, UUID newLeader) {
      if (server != null && teamId != null && sameTeamId(server, newLeader, teamId)) {
         TeamService.State state = state(server);
         TeamService.TeamMeta old = state.teams.get(teamId);
         if (old != null && !old.leader().equals(newLeader)) {
            state.teams.put(teamId, new TeamService.TeamMeta(newLeader, old.friendlyFire()));
            state.setDirty();
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private static boolean sameTeamId(MinecraftServer server, UUID player, UUID teamId) {
      return teamId.equals(teamOf(server, player));
   }

   public static boolean disband(MinecraftServer server, UUID leader) {
      if (!isLeader(server, leader)) {
         return false;
      } else {
         TeamService.State state = state(server);
         UUID teamId = teamOf(server, leader);
         state.members.entrySet().removeIf(e -> teamId.equals(e.getValue().teamId()));
         state.teams.remove(teamId);
         state.setDirty();
         return true;
      }
   }

   public static boolean setFriendlyFire(MinecraftServer server, UUID leader, boolean on) {
      if (!isLeader(server, leader)) {
         return false;
      } else {
         TeamService.State state = state(server);
         UUID teamId = teamOf(server, leader);
         state.teams.put(teamId, new TeamService.TeamMeta(leader, on));
         state.setDirty();
         return true;
      }
   }

   public static void invite(UUID inviter, UUID invitee) {
      long expiry = System.currentTimeMillis() + (long)((Integer)Config.TEAM_INVITE_TIMEOUT_SECONDS.get()).intValue() * 1000L;
      INVITES.compute(invitee, (k, pending) -> {
         Map<UUID, TeamService.Invite> map = (Map<UUID, TeamService.Invite>)(pending == null ? new ConcurrentHashMap<>() : pending);
         map.put(inviter, new TeamService.Invite(expiry, false));
         return map;
      });
   }

   public static TeamService.InviteStatus inviteStatus(UUID invitee, UUID inviter) {
      TeamService.Invite invite = peek(invitee, inviter);
      if (invite == null) {
         return TeamService.InviteStatus.NONE;
      } else {
         long now = System.currentTimeMillis();
         if (invite.expiresAt() > now) {
            return TeamService.InviteStatus.LIVE;
         } else {
            return now < invite.expiresAt() + 300000L ? TeamService.InviteStatus.EXPIRED : TeamService.InviteStatus.NONE;
         }
      }
   }

   public static boolean hasInvite(UUID invitee, UUID inviter) {
      return inviteStatus(invitee, inviter) == TeamService.InviteStatus.LIVE;
   }

   public static List<UUID> pendingInviters(UUID invitee) {
      return invitersMatching(invitee, TeamService.InviteStatus.LIVE, true);
   }

   public static List<UUID> expiredInviters(UUID invitee) {
      return invitersMatching(invitee, TeamService.InviteStatus.EXPIRED, false);
   }

   public static List<UUID> inviteesOf(UUID inviter) {
      List<UUID> result = new ArrayList<>();
      INVITES.forEach((invitee, pending) -> {
         if (hasInvite(invitee, inviter)) {
            result.add(invitee);
         }
      });
      return result;
   }

   public static boolean consumeInvite(UUID invitee, UUID inviter) {
      Map<UUID, TeamService.Invite> pending = INVITES.get(invitee);
      if (pending == null) {
         return false;
      } else {
         TeamService.Invite live = pending.get(inviter);
         return live != null && live.expiresAt() > System.currentTimeMillis() && pending.remove(inviter, live);
      }
   }

   public static List<TeamService.Lapsed> sweepInvites() {
      List<TeamService.Lapsed> lapsed = new ArrayList<>();
      long now = System.currentTimeMillis();
      INVITES.forEach((invitee, pending) -> {
         pending.forEach((inviter, invite) -> {
            if (now >= invite.expiresAt() + 300000L) {
               pending.remove(inviter, invite);
            } else if (invite.expiresAt() <= now && !invite.announced() && pending.replace(inviter, invite, new TeamService.Invite(invite.expiresAt(), true))) {
               lapsed.add(new TeamService.Lapsed(invitee, inviter));
            }
         });
         INVITES.computeIfPresent(invitee, (k, map) -> map.isEmpty() ? null : map);
      });
      return lapsed;
   }

   public static List<UUID> cancelInvitesFrom(UUID inviter) {
      List<UUID> holders = inviteesOf(inviter);

      for (UUID invitee : holders) {
         Map<UUID, TeamService.Invite> pending = INVITES.get(invitee);
         if (pending != null) {
            pending.remove(inviter);
         }
      }

      return holders;
   }

   public static void forget(UUID player) {
      INVITES.remove(player);
      INVITES.values().forEach(pending -> pending.remove(player));
   }

   public static void clearInvites() {
      INVITES.clear();
   }

   private static TeamService.Invite peek(UUID invitee, UUID inviter) {
      if (invitee != null && inviter != null) {
         Map<UUID, TeamService.Invite> pending = INVITES.get(invitee);
         return pending == null ? null : pending.get(inviter);
      } else {
         return null;
      }
   }

   private static List<UUID> invitersMatching(UUID invitee, TeamService.InviteStatus wanted, boolean soonestFirst) {
      Map<UUID, TeamService.Invite> pending = INVITES.get(invitee);
      if (pending == null) {
         return List.of();
      } else {
         List<UUID> result = new ArrayList<>();
         pending.forEach((inviter, invite) -> {
            if (inviteStatus(invitee, inviter) == wanted) {
               result.add(inviter);
            }
         });
         result.sort((a, b) -> {
            TeamService.Invite left = pending.get(a);
            TeamService.Invite right = pending.get(b);
            if (left != null && right != null) {
               return soonestFirst ? Long.compare(left.expiresAt(), right.expiresAt()) : Long.compare(right.expiresAt(), left.expiresAt());
            } else {
               return 0;
            }
         });
         return result;
      }
   }

   public static void refreshName(ServerPlayer player) {
      MinecraftServer server = player.serverLevel().getServer();
      if (server != null) {
         TeamService.State state = state(server);
         TeamService.Membership membership = state.members.get(player.getUUID());
         String name = displayName(player);
         if (membership != null && !membership.lastKnownName().equals(name)) {
            state.members.put(player.getUUID(), new TeamService.Membership(membership.teamId(), name));
            state.setDirty();
         }
      }
   }

   private static record Invite(long expiresAt, boolean announced) {
   }

   public static enum InviteStatus {
      LIVE,
      EXPIRED,
      NONE;
   }

   public static record Lapsed(UUID invitee, UUID inviter) {
   }

   public static record Membership(UUID teamId, String lastKnownName) {
   }

   public static final class State extends SavedData {
      static final Codec<UUID> UUID_STRING_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);
      private final Map<UUID, TeamService.Membership> members;
      private final Map<UUID, TeamService.TeamMeta> teams;

      State() {
         this(Map.of(), Map.of());
      }

      State(Map<UUID, TeamService.Membership> members, Map<UUID, TeamService.TeamMeta> teams) {
         this.members = new HashMap<>(members);
         this.teams = new HashMap<>(teams);
      }

      static TeamService.State load(CompoundTag tag, Provider provider) {
         Map<UUID, TeamService.Membership> members = new HashMap<>();
         CompoundTag membersTag = tag.getCompound("members");

         for (String key : membersTag.getAllKeys()) {
            CompoundTag row = membersTag.getCompound(key);
            members.put(UUID.fromString(key), new TeamService.Membership(UUID.fromString(row.getString("teamId")), row.getString("name")));
         }

         Map<UUID, TeamService.TeamMeta> teams = new HashMap<>();
         CompoundTag teamsTag = tag.getCompound("teams");

         for (String key : teamsTag.getAllKeys()) {
            CompoundTag row = teamsTag.getCompound(key);
            teams.put(UUID.fromString(key), new TeamService.TeamMeta(UUID.fromString(row.getString("leader")), row.getBoolean("friendlyFire")));
         }

         return new TeamService.State(members, teams);
      }

      public CompoundTag save(CompoundTag tag, Provider provider) {
         CompoundTag membersTag = new CompoundTag();
         this.members.forEach((uuid, membership) -> {
            CompoundTag row = new CompoundTag();
            row.putString("teamId", membership.teamId().toString());
            row.putString("name", membership.lastKnownName());
            membersTag.put(uuid.toString(), row);
         });
         tag.put("members", membersTag);
         CompoundTag teamsTag = new CompoundTag();
         this.teams.forEach((uuid, meta) -> {
            CompoundTag row = new CompoundTag();
            row.putString("leader", meta.leader().toString());
            row.putBoolean("friendlyFire", meta.friendlyFire());
            teamsTag.put(uuid.toString(), row);
         });
         tag.put("teams", teamsTag);
         return tag;
      }
   }

   public static record TeamMeta(UUID leader, boolean friendlyFire) {
   }
}
