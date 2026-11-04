package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.core.net.SyncTeamPayload;
import com.awakenedmod.awakened.core.net.TeamActionPayload;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ClientTeamState {
   private static SyncTeamPayload state = SyncTeamPayload.NONE;

   private ClientTeamState() {
   }

   public static void apply(SyncTeamPayload payload) {
      state = payload;
   }

   public static boolean enabled() {
      return state.enabled();
   }

   public static boolean inTeam() {
      return state.inTeam();
   }

   public static Integer nameplateTint(UUID player) {
      if (state.enabled() && state.inTeam() && player != null) {
         for (SyncTeamPayload.Member member : state.members()) {
            if (player.equals(member.id())) {
               return state.friendlyFire() ? -3110356 : -11025302;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   public static boolean friendlyFire() {
      return state.friendlyFire();
   }

   public static boolean youLead() {
      return state.youLead();
   }

   public static List<SyncTeamPayload.Member> members() {
      return state.members();
   }

   public static List<String> invites() {
      return state.invites();
   }

   public static void invite(String name) {
      send(new TeamActionPayload(5, name));
   }

   public static void acceptInvite(String name) {
      send(new TeamActionPayload(6, name));
   }

   public static void denyInvite(String name) {
      send(new TeamActionPayload(7, name));
   }

   public static void kick(String name) {
      send(new TeamActionPayload(0, name));
   }

   public static void leave() {
      send(new TeamActionPayload(1, ""));
   }

   public static void disband() {
      send(new TeamActionPayload(2, ""));
   }

   public static void toggleFriendlyFire() {
      send(new TeamActionPayload(friendlyFire() ? 4 : 3, ""));
   }

   private static void send(CustomPacketPayload payload) {
      PacketDistributor.sendToServer(payload, new CustomPacketPayload[0]);
   }
}
