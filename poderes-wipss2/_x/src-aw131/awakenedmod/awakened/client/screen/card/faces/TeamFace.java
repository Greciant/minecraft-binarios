package com.awakenedmod.awakened.client.screen.card.faces;

import com.awakenedmod.awakened.client.ClientTeamState;
import com.awakenedmod.awakened.client.screen.card.CardContext;
import com.awakenedmod.awakened.client.screen.card.CardFace;
import com.awakenedmod.awakened.client.screen.card.CardGfx;
import com.awakenedmod.awakened.core.net.SyncTeamPayload;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;

public final class TeamFace implements CardFace {
   private static final int HEAD = 14;
   private static final int MEMBER_ROW = 24;
   private static final int INVITE_ROW = 16;
   private static final int FF_ROW = 16;
   private static final int PICK_ROW = 14;
   private static final int PICK_ROWS = 6;
   private static final int PICK_ROWS_COMPACT = 4;
   private static final String OPEN = "team.picker";
   private static final String SCROLL = "team.pickerScroll";

   @Override
   public int height(CardContext ctx, int w, boolean compact) {
      int h = 14 + invitesH() + (ClientTeamState.inTeam() ? ClientTeamState.members().size() * 24 + 2 + 16 : 14) + 12;
      if (pickerOpen(ctx)) {
         int n = invitablePlayers(ctx).size();
         int rows = Math.min(n, pickRows(compact));
         h += 27 + rows * 14 + (n > rows ? 10 : 0);
      }

      return h;
   }

   @Override
   public void draw(CardGfx gfx, CardContext ctx, int x, int y, int w, boolean compact) {
      gfx.textBold(Component.translatable("awakened.screen.team_header"), x, y, -1185569);
      int yy = y + 14;
      yy += invites(gfx, x, yy, w);
      if (ClientTeamState.inTeam()) {
         yy += members(gfx, ctx, x, yy, w) + 2;
         yy += friendlyFire(gfx, x, yy, w);
      } else {
         gfx.text(Component.translatable("awakened.screen.team_none"), x, yy, -6054250);
         yy += 14;
      }

      actions(gfx, ctx, x, yy, w);
      if (pickerOpen(ctx)) {
         yy += 20;
         gfx.rule(x, yy, w);
         picker(gfx, ctx, x, yy + 6, w, pickRows(compact));
      }
   }

   @Override
   public boolean scrolled(CardContext ctx, double amount) {
      if (pickerOpen(ctx) && amount != 0.0) {
         int max = Math.max(0, invitablePlayers(ctx).size() - pickRows(ctx.screen.height < 300));
         int s = ctx.get("team.pickerScroll", 0) + (amount > 0.0 ? -1 : 1);
         ctx.put("team.pickerScroll", Math.max(0, Math.min(max, s)));
         return true;
      } else {
         return false;
      }
   }

   private static int invitesH() {
      int n = ClientTeamState.invites().size();
      return n > 0 ? n * 16 + 4 : 0;
   }

   private static int invites(CardGfx gfx, int x, int y, int w) {
      int ry = y;

      for (String inviter : ClientTeamState.invites()) {
         String key = "team/answer/" + inviter;
         int dw = gfx.chip(
            x + w, ry, Component.translatable("awakened.screen.team_deny").getString(), true, key + "/deny", () -> ClientTeamState.denyInvite(inviter)
         );
         int aw = gfx.chip(
            x + w - dw - 4,
            ry,
            Component.translatable("awakened.screen.team_accept").getString(),
            true,
            key + "/accept",
            () -> ClientTeamState.acceptInvite(inviter)
         );
         String text = Component.translatable("awakened.screen.team_invited_by", new Object[]{inviter}).getString();
         gfx.text(gfx.clip(text, w - dw - aw - 12), x, ry + 2, -1185569);
         ry += 16;
      }

      return invitesH();
   }

   private static int members(CardGfx gfx, CardContext ctx, int x, int y, int w) {
      boolean lead = ClientTeamState.youLead();
      UUID self = ctx.mc.player != null ? ctx.mc.player.getUUID() : null;
      List<SyncTeamPayload.Member> members = ClientTeamState.members();

      for (int i = 0; i < members.size(); i++) {
         SyncTeamPayload.Member m = members.get(i);
         int ry = y + i * 24;
         int tx = x + 22;
         head(gfx, ctx, m.id(), x, ry);
         int nx = tx + (m.leader() ? gfx.textBold(m.name(), tx, ry, -1185569) : gfx.text(m.name(), tx, ry, -1185569));
         if (m.leader()) {
            gfx.text(Component.translatable("awakened.screen.team_leader"), nx + 6, ry, -5659750);
         }

         int px = tx;
         if (m.id().equals(self)) {
            px = tx + gfx.text(ctx.power.displayName(), tx, ry + 10, -3554376) + 6;
         }

         if (!m.online()) {
            gfx.text(Component.translatable("awakened.screen.team_offline"), px, ry + 10, -6054250);
         }

         if (lead && !m.leader()) {
            String name = m.name();
            gfx.chip(
               x + w, ry + 3, Component.translatable("awakened.screen.team_kick").getString(), true, "team/kick/" + name, () -> ClientTeamState.kick(name)
            );
         }
      }

      return members.size() * 24;
   }

   private static void head(CardGfx gfx, CardContext ctx, UUID id, int x, int y) {
      ClientPacketListener conn = ctx.mc.getConnection();
      PlayerInfo info = conn != null ? conn.getPlayerInfo(id) : null;
      PlayerFaceRenderer.draw(gfx.g, info != null ? info.getSkin() : DefaultPlayerSkin.get(id), x, y, 16);
   }

   private static int friendlyFire(CardGfx gfx, int x, int y, int w) {
      boolean ff = ClientTeamState.friendlyFire();
      boolean lead = ClientTeamState.youLead();
      gfx.text(Component.translatableWithFallback("awakened.card.team.friendly_fire", "Friendly fire"), x, y + 2, -1185569);
      int cw = Math.max(gfx.width("ON"), gfx.width("OFF")) + 10;
      gfx.chip(x + w, y, ff ? "ON" : "OFF", cw, true, ff, false, !lead, "team/ff", ClientTeamState::toggleFriendlyFire);
      return 16;
   }

   private static void actions(CardGfx gfx, CardContext ctx, int x, int y, int w) {
      gfx.chip(
         x,
         y,
         Component.translatable(pickerOpen(ctx) ? "awakened.screen.team_invite_close" : "awakened.screen.team_invite").getString(),
         false,
         "team/invite",
         () -> togglePicker(ctx)
      );
      if (ClientTeamState.inTeam()) {
         int right = x + w;
         if (ClientTeamState.youLead()) {
            right -= gfx.chip(
                  right,
                  y,
                  Component.translatable("awakened.screen.team_disband").getString(),
                  0,
                  true,
                  false,
                  true,
                  false,
                  "team/disband",
                  ClientTeamState::disband
               )
               + 6;
         }

         gfx.chip(right, y, Component.translatable("awakened.screen.team_leave").getString(), true, "team/leave", ClientTeamState::leave);
      }
   }

   private static boolean pickerOpen(CardContext ctx) {
      return ctx.get("team.picker", false);
   }

   private static void togglePicker(CardContext ctx) {
      ctx.put("team.picker", pickerOpen(ctx) ? null : Boolean.TRUE);
      ctx.put("team.pickerScroll", null);
   }

   private static int pickRows(boolean compact) {
      return compact ? 4 : 6;
   }

   private static void picker(CardGfx gfx, CardContext ctx, int x, int y, int w, int rows) {
      List<String> names = invitablePlayers(ctx);
      int max = Math.max(0, names.size() - rows);
      int scroll = Math.min(ctx.get("team.pickerScroll", 0), max);
      Component header = names.isEmpty()
         ? Component.translatable("awakened.screen.team_invite_nobody")
         : Component.translatable("awakened.screen.team_invite_pick", new Object[]{names.size()});
      gfx.text(header, x, y, -3554376);
      gfx.chip(x + w, y - 2, Component.translatable("awakened.screen.team_invite_type").getString(), true, "team/type", () -> promptInvite(ctx));
      int ry = y + 13;
      String label = Component.translatable("awakened.screen.team_invite_do").getString();

      for (int i = scroll; i < Math.min(names.size(), scroll + rows); i++) {
         String name = names.get(i);
         String key = "team/pick/" + name;
         Runnable invite = () -> {
            ClientTeamState.invite(name);
            ctx.put("team.picker", null);
            ctx.put("team.pickerScroll", null);
         };
         boolean hov = gfx.hover(key);
         if (hov) {
            gfx.fill(x - 4, ry - 1, w + 8, 14, -13225168);
         }

         gfx.hit(x - 4, ry - 1, w + 8, 14, key, invite);
         gfx.text(name, x, ry + 2, hov ? -1185569 : -3554376);
         gfx.chip(x + w, ry, label, true, key + "/chip", invite);
         ry += 14;
      }

      if (max > 0) {
         gfx.text(Component.translatable("awakened.screen.team_invite_more", new Object[]{names.size() - rows}), x, ry, -6054250);
      }
   }

   private static List<String> invitablePlayers(CardContext ctx) {
      ClientPacketListener conn = ctx.mc.getConnection();
      if (conn != null && ctx.mc.player != null) {
         Set<String> exclude = new HashSet<>();
         exclude.add(ctx.mc.player.getName().getString());

         for (SyncTeamPayload.Member m : ClientTeamState.members()) {
            exclude.add(m.name());
         }

         List<String> names = new ArrayList<>();

         for (PlayerInfo info : conn.getOnlinePlayers()) {
            String name = profileName(info.getProfile());
            if (!exclude.contains(name)) {
               names.add(name);
            }
         }

         names.sort(String.CASE_INSENSITIVE_ORDER);
         return names;
      } else {
         return List.of();
      }
   }

   private static String profileName(GameProfile profile) {
      return profile.getName();
   }

   private static void promptInvite(CardContext ctx) {
      ctx.mc.setScreen(new ChatScreen("/awakened team invite "));
   }
}
