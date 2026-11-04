package com.awakenedmod.awakened.core.team;

import com.awakenedmod.awakened.core.ChatFx;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class TeamChat {
   private TeamChat() {
   }

   private static MutableComponent prefix() {
      return ChatFx.prefix();
   }

   public static void sendInvite(ServerPlayer inviter, ServerPlayer invitee) {
      String inviterName = inviter.getName().getString();
      invitee.sendSystemMessage(prefix().append(note("awakened.team.invited_you", inviterName)).append(buttons(inviterName)));
      tell(inviter, "awakened.team.invite_sent", invitee.getName().getString());
   }

   public static void sendInvitePicker(MinecraftServer server, ServerPlayer invitee, List<UUID> inviters) {
      invitee.sendSystemMessage(prefix().append(note("awakened.team.invite_pick")));

      for (UUID inviter : inviters) {
         String name = TeamActions.nameOf(server, inviter);
         invitee.sendSystemMessage(Component.literal("  " + name + " ").withStyle(ChatFormatting.GRAY).append(buttons(name)));
      }
   }

   public static void tell(ServerPlayer target, String key, Object... args) {
      target.sendSystemMessage(prefix().append(note(key, args)));
   }

   private static MutableComponent buttons(String inviterName) {
      return button("awakened.team.button_accept", ChatFormatting.GREEN, "awakened team accept " + inviterName, "awakened.team.button_accept_tip", inviterName)
         .append(Component.literal(" "))
         .append(button("awakened.team.button_deny", ChatFormatting.RED, "awakened team deny " + inviterName, "awakened.team.button_deny_tip", inviterName));
   }

   private static MutableComponent button(String labelKey, ChatFormatting color, String command, String tipKey, Object... tipArgs) {
      return ChatFx.button(labelKey, color, command, tipKey, tipArgs);
   }

   public static void broadcast(MinecraftServer server, UUID teamId, UUID except, Component message) {
      if (server != null && teamId != null) {
         MutableComponent line = prefix().append(message);

         for (UUID member : TeamService.membersOf(server, teamId)) {
            if (!member.equals(except)) {
               ServerPlayer online = server.getPlayerList().getPlayer(member);
               if (online != null) {
                  online.sendSystemMessage(line);
               }
            }
         }
      }
   }

   public static Component note(String key, Object... args) {
      return ChatFx.note(key, args);
   }
}
