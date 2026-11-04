package com.awakenedmod.awakened.client.screen.card.faces;

import com.awakenedmod.awakened.client.ClientPowerState;
import com.awakenedmod.awakened.client.screen.GuiPanels;
import com.awakenedmod.awakened.client.screen.card.CardContext;
import com.awakenedmod.awakened.client.screen.card.CardFace;
import com.awakenedmod.awakened.client.screen.card.CardGfx;
import com.awakenedmod.awakened.client.screen.card.CardStyle;
import com.awakenedmod.awakened.core.net.QuestActionPayload;
import com.awakenedmod.awakened.core.quest.Quest;
import com.awakenedmod.awakened.core.quest.QuestRegistry;
import com.awakenedmod.awakened.core.quest.QuestReward;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public final class TrialsFace implements CardFace {
   private static final int HEAD = 12;
   private static final int PITCH = 20;
   private static final int TILE = 18;
   private static final int GAP = 10;
   private static final int NUM = 18;
   private static final int LEAD = 9;
   private static final Component HINT_PIN = Component.translatableWithFallback("awakened.card.trials.hint_pin", "Click to pin it to your HUD.");
   private static final Component HINT_PIN_SWAP = Component.translatableWithFallback(
      "awakened.card.trials.hint_pin_swap", "Click to pin it to your HUD in place of the current one."
   );
   private static final Component HINT_UNPIN = Component.translatableWithFallback("awakened.card.trials.hint_unpin", "Click to unpin it from your HUD.");
   private static final Component HINT_REROLL = Component.translatableWithFallback(
      "awakened.card.trials.hint_reroll", "Swap it for another trial of this tier or lower. Finishing any quest refills your rerolls."
   );
   private static final Component HINT_NO_REROLLS = Component.translatableWithFallback(
      "awakened.card.trials.hint_no_rerolls", "No rerolls left. Finishing any quest refills them."
   );
   private static final List<Component> HINTS = List.of(HINT_PIN, HINT_PIN_SWAP, HINT_UNPIN, HINT_REROLL, HINT_NO_REROLLS);
   private final Set<String> cut = new HashSet<>();

   @Override
   public int height(CardContext ctx, int w, boolean compact) {
      List<TrialsFace.Row> rows = rows(ctx);
      int goals = Math.max(1, (int)rows.stream().filter(rx -> !rx.trial()).count());
      int reserve = 11;

      for (TrialsFace.Row r : rows) {
         if (r.quest() != null) {
            reserve = Math.max(reserve, detailH(ctx, r.quest(), w));
         }
      }

      return 29 + (goals + 4) * 20 + 10 + 6 + reserve;
   }

   @Override
   public void draw(CardGfx gfx, CardContext ctx, int x, int y, int w, boolean compact) {
      this.cut.clear();
      ClientPowerState st = ctx.state;
      List<TrialsFace.Row> rows = rows(ctx);
      Quest hovered = null;

      for (TrialsFace.Row r : rows) {
         if (r.quest() != null && gfx.hover(r.key())) {
            hovered = r.quest();
         }
      }

      boolean anyHover = hovered != null;
      gfx.textBold(Component.translatable("awakened.screen.quests_goals"), x, y, -1185569);
      gfx.textRight(Component.translatableWithFallback("awakened.card.trials.pin_tip", "Click a row to pin it"), x + w, y, -6054250);
      int var16 = y + 17;
      int goals = 0;

      for (TrialsFace.Row rx : rows) {
         if (!rx.trial()) {
            this.row(gfx, ctx, rx, x, var16 + goals++ * 20, w, anyHover);
         }
      }

      if (goals == 0) {
         gfx.text(Component.translatable("awakened.screen.quests_no_goals"), x + 18, var16, -6054250);
         goals = 1;
      }

      var16 += goals * 20 + 10 - 5;
      gfx.textBold(Component.translatable("awakened.screen.quests_trials"), x, var16, -1185569);
      gfx.textRight(rerollsLeft(st.questRerolls()), x + w, var16, -3554376);
      var16 += 17;

      for (TrialsFace.Row rxx : rows) {
         if (rxx.trial()) {
            this.row(gfx, ctx, rxx, x, var16 + rxx.slot() * 20, w, anyHover);
         }
      }

      var16 += 80;
      gfx.rule(x, var16, w);
      var16 += 6;
      Quest focus = hovered != null ? hovered : defaultFocus(st, rows);
      if (focus == null) {
         gfx.text(Component.translatable("awakened.screen.quests_none"), x, var16, -6054250);
      } else {
         this.detail(gfx, ctx, focus, focus == hovered ? hint(gfx, st, focus) : null, x, var16, w);
      }
   }

   private static List<TrialsFace.Row> rows(CardContext ctx) {
      List<TrialsFace.Row> rows = new ArrayList<>();

      for (Quest g : QuestRegistry.goalsFor(ctx.state.powerId())) {
         rows.add(new TrialsFace.Row(g, -1));
      }

      List<String> slate = ctx.state.questSlate();

      for (int s = 0; s < 4; s++) {
         String id = s < slate.size() ? slate.get(s) : "";
         rows.add(new TrialsFace.Row(id.isEmpty() ? null : QuestRegistry.byId(id), s));
      }

      return rows;
   }

   private static Quest defaultFocus(ClientPowerState st, List<TrialsFace.Row> rows) {
      Quest first = null;
      Quest open = null;

      for (TrialsFace.Row r : rows) {
         Quest q = r.quest();
         if (q != null) {
            if (q.id().equals(st.pinnedQuest())) {
               return q;
            }

            if (first == null) {
               first = q;
            }

            if (open == null && !st.quest(q.id()).complete()) {
               open = q;
            }
         }
      }

      return open != null ? open : first;
   }

   private void row(CardGfx gfx, CardContext ctx, TrialsFace.Row r, int x, int ry, int w, boolean anyHover) {
      Quest q = r.quest();
      int tx = x + 18;
      if (q == null) {
         gfx.fill(x - 4, ry - 4, w + 8, 18, -14277856);
         gfx.text(Component.translatable("awakened.screen.quest_slot_empty"), tx, ry, -6054250);
      } else {
         ClientPowerState st = ctx.state;
         ClientPowerState.QuestView view = st.quest(q.id());
         boolean done = view.complete();
         boolean locked = locked(st, q);
         boolean single = q.required() <= 1;
         boolean hov = gfx.hover(r.key());
         boolean pinned = q.id().equals(st.pinnedQuest());
         gfx.fill(x - 4, ry - 4, w + 8, 18, hov ? -13225168 : -14277856);
         if (done) {
            gfx.hit(x - 4, ry - 4, w + 8, 18, r.key());
         } else {
            gfx.hit(x - 4, ry - 4, w + 8, 18, r.key(), () -> send(0, 0, q.id()));
         }

         if (r.trial()) {
            gfx.textBold(q.tier().glyph(), x, ry, CardStyle.opaque(q.tier().color()));
         }

         int ix = x + w - 17;
         rewardIcon(gfx, q, r.key(), ix, ry - 4, done || view.itemsClaimed());
         int rx = ix - 5;
         String count = done
            ? Component.translatable("awakened.screen.quest_done").getString()
            : (single ? "" : GuiPanels.questCount(view.progress(), q.required()));
         if (!count.isEmpty()) {
            rx = gfx.textRight(count, rx, ry, done ? -6054250 : -3554376) - 6;
         }

         if (r.trial() && (hov || !anyHover && pinned)) {
            rx -= rerollChip(gfx, ctx, r, rx, ry - 2) + 6;
         }

         String title = gfx.clip(q.title().getString(), rx - tx);
         int lx = tx + gfx.text(title, tx, ry, !done && !locked ? -1185569 : -6054250) + 6;
         if (pinned) {
            Component pin = Component.translatableWithFallback("awakened.card.trials.pinned", "Pinned");
            if (lx + gfx.width(pin) <= rx) {
               lx += gfx.text(pin, lx, ry, gfx.band) + 6;
            }
         }

         if (!done && fitLine(gfx, locked ? lockedBody(q).getString() : task(q), lx + 4, ry, rx - lx - 4, -6054250)) {
            this.cut.add(q.id());
         }

         if (!done && !single) {
            gfx.thinBar(tx, ry + 11, ix - 5 - tx, (float)view.progress() / (float)q.required(), gfx.band);
         }
      }
   }

   private static void rewardIcon(CardGfx gfx, Quest q, String key, int ix, int iy, boolean dim) {
      List<QuestReward> rewards = q.rewards();
      if (!rewards.isEmpty()) {
         ItemStack stack = rewards.get(0).preview();
         gfx.itemWithCount(stack, ix, iy);
         if (dim) {
            dimItem(gfx, ix, iy);
         }

         gfx.hit(ix, iy, 16, 16, key + "/item");
         if (gfx.hover(key + "/item")) {
            gfx.tooltip(stack);
         }
      }
   }

   private static void dimItem(CardGfx gfx, int x, int y) {
      gfx.g.pose().pushPose();
      gfx.g.pose().translate(0.0F, 0.0F, 250.0F);
      gfx.fill(x, y, 16, 16, -1439814872);
      gfx.g.pose().popPose();
   }

   private static int rerollChip(CardGfx gfx, CardContext ctx, TrialsFace.Row r, int right, int y) {
      boolean none = ctx.state.questRerolls() <= 0;
      String key = r.key() + "/reroll";
      String label = Component.translatableWithFallback("awakened.card.trials.reroll", "Reroll").getString();
      int cw = gfx.chip(right, y, label, 0, true, false, false, none, key, () -> send(1, r.slot(), ""));
      if (none) {
         gfx.hit(right - cw, y, cw, 12, key);
      }

      return cw;
   }

   private static boolean fitLine(CardGfx gfx, String s, int x, int y, int maxW, int col) {
      if (s.isEmpty()) {
         return false;
      } else if (maxW < 12) {
         return true;
      } else {
         String shown = gfx.clip(s, maxW);
         gfx.text(shown, x, y, col);
         return !shown.equals(s);
      }
   }

   private void detail(CardGfx gfx, CardContext ctx, Quest q, Component hint, int x, int y, int w) {
      ClientPowerState.QuestView view = ctx.state.quest(q.id());
      boolean locked = locked(ctx.state, q);
      int right = x + w;
      if (!q.goal()) {
         right = gfx.textRight(tierTag(q), x + w, y, CardStyle.opaque(q.tier().color())) - 6;
      }

      gfx.textBold(gfx.clip(q.title().getString(), right - x - 8), x, y, -1185569);
      int yy = y + 9 + 2;
      Component body = locked ? lockedBody(q) : (this.cut.contains(q.id()) ? q.description() : rest(q));
      if (!body.getString().isEmpty()) {
         yy += gfx.paragraph(body, x, yy, w, locked ? -6054250 : -1185569, Integer.MAX_VALUE) * 9;
      }

      Component sub = hint != null ? hint : (locked ? null : q.note());
      if (sub != null) {
         yy += gfx.paragraph(sub, x, yy, w, hint != null ? gfx.band : -5659750, Integer.MAX_VALUE) * 9;
      }

      yy += 3;
      gfx.text(gfx.clip(rewardLine(q, view).getString(), w), x, yy, -3554376);
   }

   private static Component hint(CardGfx gfx, ClientPowerState st, Quest q) {
      if (gfx.hover("quest/" + q.id() + "/reroll")) {
         return st.questRerolls() > 0 ? HINT_REROLL : HINT_NO_REROLLS;
      } else if (st.quest(q.id()).complete()) {
         return null;
      } else if (q.id().equals(st.pinnedQuest())) {
         return HINT_UNPIN;
      } else {
         return st.pinnedQuest().isEmpty() ? HINT_PIN : HINT_PIN_SWAP;
      }
   }

   private static int detailH(CardContext ctx, Quest q, int w) {
      boolean locked = locked(ctx.state, q);
      Component body = locked ? lockedBody(q) : q.description();
      int lines = body.getString().isEmpty() ? 0 : ctx.lines(body, w);
      int sub = !locked && q.note() != null ? ctx.lines(q.note(), w) : 0;

      for (Component h : HINTS) {
         sub = Math.max(sub, ctx.lines(h, w));
      }

      return 11 + (lines + sub) * 9 + 3 + 9;
   }

   private static Component rewardLine(Quest q, ClientPowerState.QuestView view) {
      boolean done = view.complete();
      List<String> parts = new ArrayList<>();
      int mastery = QuestSystem.masteryPayout(q.masteryReward(), done ? view.timesCompleted() - 1 : view.timesCompleted());
      if (mastery > 0) {
         parts.add(Component.translatable("awakened.screen.quest_reward_mastery", new Object[]{mastery}).getString());
      }

      if (q.xpReward() > 0 && (done || !view.xpClaimed())) {
         parts.add(Component.translatable("awakened.screen.quest_reward_xp", new Object[]{q.xpReward()}).getString());
      }

      if (done || !view.itemsClaimed()) {
         for (QuestReward reward : q.rewards()) {
            parts.add(reward.label().getString());
         }
      }

      String list = parts.isEmpty() ? Component.translatable("awakened.screen.quest_reward_none").getString() : String.join(", ", parts);
      return done
         ? Component.translatableWithFallback("awakened.card.trials.earned", "Earned: %s", new Object[]{list})
         : Component.translatable("awakened.screen.quest_reward", new Object[]{list});
   }

   private static Component tierTag(Quest q) {
      return Component.translatableWithFallback("awakened.card.trials.tier", "Tier %s trial", new Object[]{q.tier().glyph()});
   }

   private static Component rerollsLeft(int n) {
      return n == 1
         ? Component.translatableWithFallback("awakened.card.trials.rerolls_left_one", "1 reroll left")
         : Component.translatableWithFallback("awakened.card.trials.rerolls_left", "%s rerolls left", new Object[]{n});
   }

   private static String task(Quest q) {
      String first = sentences(q)[0].strip();
      return first.endsWith(".") ? first.substring(0, first.length() - 1) : first;
   }

   private static Component rest(Quest q) {
      String[] s = sentences(q);
      return Component.literal(s.length > 1 ? s[1].strip() : "");
   }

   private static String[] sentences(Quest q) {
      return q.description().getString().split("(?<=\\.)\\s+", 2);
   }

   private static Component lockedBody(Quest q) {
      Quest blocker = QuestRegistry.byId(q.prereq());
      Component name = (Component)(blocker != null ? blocker.title() : Component.literal(String.valueOf(q.prereq())));
      return Component.translatable("awakened.screen.quest_locked", new Object[]{name});
   }

   private static boolean locked(ClientPowerState st, Quest q) {
      return q.prereq() != null && !st.quest(q.prereq()).complete();
   }

   private static void send(int action, int slot, String questId) {
      PacketDistributor.sendToServer(new QuestActionPayload(action, slot, questId), new CustomPacketPayload[0]);
   }

   private static record Row(Quest quest, int slot) {
      boolean trial() {
         return this.slot >= 0;
      }

      String key() {
         return "quest/" + this.quest.id();
      }
   }
}
