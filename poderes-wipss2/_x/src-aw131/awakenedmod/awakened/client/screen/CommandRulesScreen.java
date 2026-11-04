package com.awakenedmod.awakened.client.screen;

import com.awakenedmod.awakened.client.AwakenedKeybinds;
import com.awakenedmod.awakened.client.ClientPowerState;
import com.awakenedmod.awakened.client.HudText;
import com.awakenedmod.awakened.client.screen.card.CardChrome;
import com.awakenedmod.awakened.client.screen.card.CardGfx;
import com.awakenedmod.awakened.client.screen.card.CardHits;
import com.awakenedmod.awakened.client.screen.card.CardStyle;
import com.awakenedmod.awakened.content.command.CommandConfig;
import com.awakenedmod.awakened.content.command.CommandCost;
import com.awakenedmod.awakened.content.command.CommandData;
import com.awakenedmod.awakened.content.command.CommandPower;
import com.awakenedmod.awakened.content.command.CommandPresets;
import com.awakenedmod.awakened.content.command.CommandSentences;
import com.awakenedmod.awakened.content.command.Rule;
import com.awakenedmod.awakened.content.command.RuleSlot;
import com.awakenedmod.awakened.content.command.RuleToggleAbility;
import com.awakenedmod.awakened.content.command.vocab.Clash;
import com.awakenedmod.awakened.content.command.vocab.Effect;
import com.awakenedmod.awakened.content.command.vocab.Target;
import com.awakenedmod.awakened.content.command.vocab.TargetKind;
import com.awakenedmod.awakened.content.command.vocab.Trigger;
import com.awakenedmod.awakened.content.command.vocab.Vocabulary;
import com.awakenedmod.awakened.content.command.vocab.Word;
import com.awakenedmod.awakened.core.net.ActivateAbilityPayload;
import com.awakenedmod.awakened.core.net.CommandRenamePayload;
import com.awakenedmod.awakened.core.net.CommandRulePayload;
import com.awakenedmod.awakened.core.power.Power;
import com.awakenedmod.awakened.core.power.PowerRegistry;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.blaze3d.platform.InputConstants.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.IntFunction;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

public final class CommandRulesScreen extends AwakenedScreen {
   private static final int PANEL_W = 456;
   private static final int PAD = 14;
   private static final int SCREEN_MARGIN = 8;
   private static final int LINE = 10;
   private static final int HEAD_TO_BAR = 6;
   private static final int BAR_H = 9;
   private static final int HEAD_H = 51;
   private static final int HEAD_TO_SLOT = 7;
   private static final int SLOT_TOP = 1;
   private static final int SENTENCE_H = 20;
   private static final int ROW_TO_META = 2;
   private static final int META_H = 9;
   private static final int SLOT_GAP = 6;
   private static final int SLOT_BODY_H = 32;
   private static final int LOCKED_BODY_H = 12;
   private static final int FOOT_GAP = 6;
   private static final int BOTTOM = 6;
   private static final int GUTTER = 16;
   private static final int NUMERAL_INSET = 4;
   private static final int LIVE_BAR_W = 2;
   private static final int CHIP_H = 16;
   private static final int CHIP_PAD = 3;
   private static final int CHIP_GAP = 4;
   private static final float CHIP_SCALE_MAX = 1.25F;
   private static final float CHIP_SCALE_MIN = 1.0F;
   private static final int BTN_H = 12;
   private static final int LANE_GAP = 6;
   private static final int GLYPH_W = 5;
   private static final int CARD_PAD = 8;
   private static final int CARD_ROW_H = 11;
   private static final int CARD_COL_GAP = 6;
   private static final int CARD_MAX_COLS = 5;
   private static final int CARD_MIN_ROWS = 6;
   private static final int CARD_EDGE = 4;
   private static final int CARD_MIN_INNER_W = 240;
   private static final int CARD_FOOT_MIN_LINES = 2;
   private static final int CARD_FOOT_MAX_LINES = 4;
   private static final int CARD_FOOT_LINE_H = 9;
   private static final int ACCENT = CardStyle.band(-1525686);
   private static final int INK1 = -1185569;
   private static final int SETTLED = -3554376;
   private static final int INK2 = -5659750;
   private static final int INK3 = -6054250;
   private static final int ERROR = -1734024;
   private static final int CARD_DIM = -1728053248;
   private static final CardHits NO_HITS = new CardHits();
   private final List<CommandRulesScreen.Hit> hits = new ArrayList<>();
   private final List<CommandRulesScreen.Hit> cardHits = new ArrayList<>();
   private final Rule[] draft = new Rule[5];
   private final Rule[] known = new Rule[5];
   @Nullable
   private CommandRulesScreen.Blank pickerBlank;
   private int pickerSlot = -1;
   private int pickerScroll;
   private int cardVisibleRows = 6;
   private int pickerIndex;
   private int cardAnchorX;
   private int cardAnchorY;
   private int cardAnchorW;
   private boolean cardAnchorSeen;
   private int cardX0;
   private int cardY0;
   private int cardX1;
   private int cardY1;
   private int focusSlot;
   @Nullable
   private CommandRulesScreen.Blank focusBlank = CommandRulesScreen.Blank.TARGET;
   private int pendingClear = -1;
   private static boolean startersOpen = false;
   private static final int STARTER_ROW = 14;
   private String numberBuffer = "";
   private int namingSlot = -1;
   private String nameBuffer = "";
   private final String[] sentNames = new String[5];
   private String filter = "";
   @Nullable
   private CommandRulesScreen.Blank dragBlank;
   private int dragSlot = -1;
   private int dragStartX;
   private int dragStartValue;
   @Nullable
   private String note;
   private int noteTicks;
   private int flashSlot = -1;
   private int flashTicks;
   private float uiScale = 1.0F;
   private int uiPivotX;
   private int uiPivotY;
   private boolean onLimitReached;
   private int maxOn = 1;

   public CommandRulesScreen() {
      super(Component.translatable("awakened.command.screen.title"));
   }

   public boolean isPauseScreen() {
      return false;
   }

   protected void init() {
      super.init();
      this.uiScale = 1.0F;
      this.uiPivotX = this.width / 2;
      this.uiPivotY = this.height / 2;
   }

   public void tick() {
      super.tick();
      if (this.noteTicks > 0 && --this.noteTicks == 0) {
         this.note = null;
      }

      if (this.flashTicks > 0) {
         this.flashTicks--;
      }
   }

   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      super.render(g, mouseX, mouseY, partialTick);
      if (this.minecraft != null && this.minecraft.player != null) {
         CommandData rules = CommandData.of(this.minecraft.player);
         this.syncDrafts(rules);
         int mastery = this.mastery();
         this.maxOn = CommandPower.maxOn(mastery);
         this.onLimitReached = rules.atOnLimit(this.maxOn);
         int bareH = 58 + slotsHeight(mastery) + 6 + 6;
         this.uiScale = Math.min(1.0F, Math.min((float)(this.height - 16) / (float)bareH, (float)(this.width - 16) / 456.0F));
         boolean scaled = this.uiScale < 1.0F;
         int x = (this.width - 456) / 2;
         int y = Math.max(8, (this.height - bareH) / 2);
         mouseX = this.unscale(mouseX, this.uiPivotX);
         mouseY = this.unscale(mouseY, this.uiPivotY);
         if (scaled) {
            g.pose().pushPose();
            g.pose().translate((float)this.uiPivotX, (float)this.uiPivotY, 0.0F);
            g.pose().scale(this.uiScale, this.uiScale, 1.0F);
            g.pose().translate((float)(-this.uiPivotX), (float)(-this.uiPivotY), 0.0F);
         }

         CardGfx gfx = new CardGfx(g, this.font, NO_HITS, ACCENT, mouseX, mouseY, this.uiScale);
         CardChrome.face(gfx, x, y, 456, bareH);
         this.hits.clear();
         this.cardHits.clear();
         this.cardAnchorSeen = false;
         int cx = x + 14;
         int cw = 428;
         this.drawBand(gfx, rules, x, y, mouseX, mouseY);
         int cy = y + 2 + 32 + 2 + 6;
         this.drawBudget(g, rules, mastery, cx, cy, cw, mouseX, mouseY);
         cy += 16;
         CommandRulesScreen.Grid grid = this.buildGrid(rules, mastery, cx + 16, cx + cw);

         for (int slot = 0; slot < 5; slot++) {
            this.drawSlot(g, rules.slot(slot), slot, mastery, grid, cx, cy, cw, mouseX, mouseY);
            int bodyH = slotBodyHeight(slot, mastery);
            if (slot < 4) {
               gfx.rule(cx, cy + bodyH + 3, cw);
            }

            cy += bodyH + 6;
         }

         CardChrome.frame(gfx, x, y, 456, bareH);
         if (this.pickerBlank != null && !this.cardAnchorSeen) {
            this.closeCard();
         }

         if (this.pickerBlank != null) {
            g.pose().pushPose();
            g.pose().translate(0.0F, 0.0F, 400.0F);
            g.fill(
               this.unscale(0, this.uiPivotX),
               this.unscale(0, this.uiPivotY),
               this.unscale(this.width, this.uiPivotX),
               this.unscale(this.height, this.uiPivotY),
               -1728053248
            );
            this.drawCard(g, mastery, x, mouseX, mouseY);
            g.pose().popPose();
         }

         if (scaled) {
            g.pose().popPose();
         }

         GuiPanels.flushTooltip(g, this.font, mouseX, mouseY);
      }
   }

   private static int slotBodyHeight(int slot, int mastery) {
      return mastery >= CommandPower.slotUnlockLevel(slot) ? 32 : 12;
   }

   private static int slotsHeight(int mastery) {
      int h = 0;

      for (int slot = 0; slot < 5; slot++) {
         h += slotBodyHeight(slot, mastery) + 6;
      }

      return h - 6;
   }

   private void drawBand(CardGfx gfx, CommandData rules, int x, int y, int mx, int my) {
      CardChrome.title(gfx, Component.translatable("awakened.command.screen.title"), x + 14, y);
      int qy = CardChrome.buttonY(y);
      int ink = CardStyle.titleInk(gfx.band);
      int right = x + 456 - 14;
      float max = ClientPowerState.get().staminaMax();
      String caption = Component.translatable("awakened.command.screen.budget_caption", new Object[]{Math.round(held(rules, max)), Math.round(max)})
         .getString();
      gfx.textRight(caption, right, qy + 1, ink);
      String count = Component.translatable("awakened.command.screen.on_count", new Object[]{rules.onCount(), this.maxOn}).getString();
      int countW = this.onLimitReached ? gfx.widthBold(count) : this.font.width(count);
      if (this.onLimitReached) {
         gfx.textBold(count, right - countW, qy + 11, ink);
      } else {
         gfx.text(count, right - countW, qy + 11, ink);
      }

      if (this.hovering(mx, my, right - countW, qy + 10, countW, 10)) {
         int open = 0;

         for (int slot = 0; slot < 5; slot++) {
            if (this.mastery() >= CommandPower.slotUnlockLevel(slot)) {
               open++;
            }
         }

         GuiPanels.queueTooltip(this.font, Component.translatable("awakened.command.screen.on_limit_tip", new Object[]{open, this.maxOn}), 200);
      }
   }

   private void drawBudget(GuiGraphics g, CommandData rules, int mastery, int cx, int cy, int cw, int mx, int my) {
      float max = ClientPowerState.get().staminaMax();
      float locked = held(rules, max);
      g.fill(cx, cy, cx + cw, cy + 9, -15461872);
      int ix = cx + 2;
      int iw = cw - 4;
      int fw = max > 0.0F ? Math.round((float)iw * GuiPanels.clamp01(locked / max)) : 0;
      if (fw > 0) {
         g.fill(ix, cy + 2, ix + fw, cy + 9 - 2, ACCENT);
         g.fill(ix, cy + 2, ix + fw, cy + 3, GuiPanels.lighten(ACCENT, 0.3F));
      }

      if (max > 0.0F) {
         float run = 0.0F;

         for (int slot = 0; slot < 5; slot++) {
            RuleSlot stored = rules.slot(slot);
            if (stored.on() && !(stored.held() <= 0.0F)) {
               run = Math.min(max, run + stored.held());
               int edge = ix + Math.round((float)iw * (run / max));
               g.fill(edge - 1, cy + 2, edge, cy + 9 - 2, -15461872);
            }
         }

         this.drawGhostSegment(g, rules, mastery, ix, cy, iw, max, locked);
      }

      if (this.hovering(mx, my, cx, cy, cw, 9)) {
         GuiPanels.queueTooltip(this.font, Component.translatable("awakened.command.screen.budget_tip"), 200);
      }
   }

   private static float held(CommandData rules, float max) {
      float synced = ClientPowerState.get().staminaLocked();
      return Math.max(0.0F, Math.min(max, Math.max(synced, rules.lockedTotal())));
   }

   private void drawGhostSegment(GuiGraphics g, CommandData rules, int mastery, int cx, int cy, int cw, float max, float locked) {
      int slot = this.editedSlot(rules);
      if (slot >= 0) {
         Rule rule = this.draft[slot];
         int points = CommandCost.points(rule, mastery);
         if (points > 0) {
            RuleSlot stored = rules.slot(slot);
            float base = Math.max(0.0F, locked - (stored.on() ? stored.held() : 0.0F));
            boolean fits = max <= 0.0F || (float)points <= max;
            int x0 = cx + Math.round((float)cw * Math.min(1.0F, base / max));
            int x1 = cx + Math.min(cw, Math.round((float)cw * Math.min(1.0F, (base + (float)points) / max)));
            if (x1 <= x0) {
               x1 = x0 + 1;
            }

            int colour = fits ? ACCENT : -1734024;
            g.fill(x0, cy + 2, x1, cy + 9 - 2, GuiPanels.withAlpha(colour, 85));
            GuiPanels.outline(g, x0, cy, x1 - x0, 9, colour);
         }
      }
   }

   private int editedSlot(CommandData rules) {
      if (this.focusSlot >= 0
         && this.focusSlot < 5
         && this.draft[this.focusSlot] != null
         && !this.draft[this.focusSlot].isEmpty()
         && !this.draft[this.focusSlot].equals(rules.slot(this.focusSlot).rule())) {
         return this.focusSlot;
      } else {
         for (int slot = 0; slot < 5; slot++) {
            if (this.draft[slot] != null && !this.draft[slot].isEmpty() && !this.draft[slot].equals(rules.slot(slot).rule())) {
               return slot;
            }
         }

         return -1;
      }
   }

   private void drawSlot(GuiGraphics g, RuleSlot stored, int slot, int mastery, CommandRulesScreen.Grid grid, int cx, int cy, int cw, int mx, int my) {
      int unlock = CommandPower.slotUnlockLevel(slot);
      boolean unlocked = mastery >= unlock;
      int right = cx + cw;
      boolean running = unlocked && stored.on();
      int bodyH = slotBodyHeight(slot, mastery);
      boolean rowHover = this.hovering(mx, my, cx, cy, cw, bodyH);
      if (rowHover) {
         g.fill(cx - 4, cy, right + 4, cy + bodyH, -13225168);
      }

      if (this.flashSlot == slot && this.flashTicks > 0) {
         g.fill(cx - 4, cy, right + 4, cy + bodyH, GuiPanels.withAlpha(ACCENT, 6 + this.flashTicks * 3));
      }

      int sy = cy + 1;
      if (running) {
         g.fill(cx, sy, cx + 2, sy + 20, ACCENT);
      }

      int metaY = cy + 1 + 20 + 2;
      int textX = cx + 16;
      String num = numeral(slot);
      int numX = textX - 4 - this.font.width(num);
      if (!unlocked) {
         int lineY = sy + 1;
         g.drawString(this.font, num, numX, lineY, -6054250, false);
         g.drawString(this.font, Component.translatable("awakened.command.screen.slot_locked", new Object[]{unlock}).getString(), textX, lineY, -6054250, false);
      } else {
         boolean numHover = this.hovering(mx, my, cx, sy, textX - cx - 2, 20);
         g.drawString(this.font, num, numX, sy + (20 - 9) / 2, !numHover && this.namingSlot != slot ? -5659750 : -1185569, false);
         this.hits.add(new CommandRulesScreen.Hit(cx, sy, textX - 2, sy + 20, CommandRulesScreen.Act.NAME, slot, null, ""));
         if (numHover && this.namingSlot != slot) {
            GuiPanels.queueTooltip(this.font, Component.translatable("awakened.command.screen.name_tip"), 160);
         }

         int room = Math.max(80, right - this.laneWidth() - 6 - textX);
         CommandRulesScreen.Reveal hint = this.drawSentence(g, slot, grid, room, sy, mx, my, mastery);
         this.drawLane(g, stored, slot, right, sy, mx, my);
         this.drawMeta(g, stored, slot, mastery, textX, metaY, right, rowHover, mx, my, hint);
      }
   }

   private static String numeral(int slot) {
      return CommandSentences.numeral(slot);
   }

   private int laneWidth() {
      return this.switchWidth();
   }

   private int switchWidth() {
      return Math.max(
            this.font.width(Component.translatable("awakened.command.screen.switch_on")),
            this.font.width(Component.translatable("awakened.command.screen.switch_off"))
         )
         + 8;
   }

   private int decreeWidth() {
      return this.font.width(Component.translatable("awakened.command.screen.decree")) + 8;
   }

   private void drawLane(GuiGraphics g, RuleSlot stored, int slot, int right, int sy, int mx, int my) {
      if (!stored.isEmpty()) {
         this.drawSwitch(g, stored, slot, right - this.switchWidth(), sy + 4, mx, my);
      }
   }

   private int drawDecree(GuiGraphics g, RuleSlot stored, int slot, int mastery, int right, int y, int mx, int my) {
      Rule rule = this.draft[slot];
      if (rule.resolves() && !rule.equals(stored.rule())) {
         int w = this.decreeWidth();
         int x = right - w;
         int top = y - 2;
         boolean fits = fits(rule, mastery);
         boolean hover = this.hovering(mx, my, x, top, w, 12);
         cardChip(g, x, top, w, 12, false, hover, false);
         String decree = Component.translatable("awakened.command.screen.decree").getString();
         g.drawString(this.font, decree, x + (w - this.font.width(decree) + 1) / 2, y, fits ? -1185569 : -1734024, false);
         this.hits.add(new CommandRulesScreen.Hit(x, top, right, top + 12, CommandRulesScreen.Act.DECREE, slot, null, ""));
         return x;
      } else {
         return right;
      }
   }

   private void drawSwitch(GuiGraphics g, RuleSlot stored, int slot, int x, int y, int mx, int my) {
      boolean on = stored.on();
      boolean usable = stored.rule().resolves();
      boolean blocked = usable && !on && this.onLimitReached;
      int w = this.switchWidth();
      boolean hover = usable && this.hovering(mx, my, x, y, w, 12);
      cardChip(g, x, y, w, 12, on, hover, !usable);
      String label = Component.translatable(on ? "awakened.command.screen.switch_on" : "awakened.command.screen.switch_off").getString();
      int ink = on ? -13751512 : (usable && !blocked ? -1185569 : -6054250);
      g.drawString(this.font, label, x + (w - this.font.width(label) + 1) / 2, y + 2, ink, false);
      if (usable) {
         this.hits.add(new CommandRulesScreen.Hit(x, y, x + w, y + 12, CommandRulesScreen.Act.SWITCH, slot, null, ""));
      }

      if (blocked && hover) {
         GuiPanels.queueTooltip(this.font, Component.translatable("awakened.command.screen.switch_full", new Object[]{this.maxOn}), 200);
      }
   }

   private int drawQuietActions(GuiGraphics g, RuleSlot stored, int slot, int right, int y, boolean rowHover, int mx, int my) {
      int x = right;
      boolean edited = !this.draft[slot].equals(stored.rule());
      if (!this.draft[slot].isEmpty() || !stored.isEmpty()) {
         x = this.quietAction(g, "awakened.command.screen.clear", CommandRulesScreen.Act.CLEAR, slot, right, y, rowHover, mx, my);
      }

      if (edited) {
         x = this.quietAction(g, "awakened.command.screen.revert", CommandRulesScreen.Act.REVERT, slot, x - 8, y, rowHover, mx, my);
      }

      return x;
   }

   private int quietAction(GuiGraphics g, String key, CommandRulesScreen.Act act, int slot, int right, int y, boolean rowHover, int mx, int my) {
      String label = Component.translatable(key).getString();
      int w = this.font.width(label);
      int x = right - w;
      boolean hover = this.hovering(mx, my, x, y - 1, w, 10);
      int colour = hover ? -1185569 : (rowHover ? -5659750 : -12238791);
      g.drawString(this.font, label, x, y, colour, false);
      this.hits.add(new CommandRulesScreen.Hit(x, y - 1, x + w, y + 9, act, slot, null, ""));
      return x;
   }

   private void drawMeta(
      GuiGraphics g,
      RuleSlot stored,
      int slot,
      int mastery,
      int x,
      int y,
      int right,
      boolean rowHover,
      int mx,
      int my,
      @Nullable CommandRulesScreen.Reveal hint
   ) {
      if (this.pendingClear == slot) {
         this.drawClearPrompt(g, slot, right, y, mx, my);
      } else {
         int costLeft = this.drawCost(g, slot, mastery, right, y, mx, my);
         int textRight = this.drawQuietActions(g, stored, slot, costLeft - 10, y, rowHover, mx, my);
         textRight = this.drawDecree(g, stored, slot, mastery, textRight - 8, y, mx, my);
         if (this.namingSlot == slot) {
            this.drawNameField(g, x, y, textRight);
         } else {
            String name = this.shownName(slot);
            if (!name.isEmpty() || !this.draft[slot].isEmpty()) {
               x = this.drawNameChip(g, slot, name, x, y, textRight, mx, my);
            }

            Rule rule = this.draft[slot];
            if (hint != null) {
               int limit = textRight - 8;
               int w = this.font.width(hint.text());
               int hx = Math.max(x, Math.min(hint.x(), limit - w));
               if (hx + w <= limit) {
                  g.drawString(this.font, hint.text(), hx, y, hint.bad() ? -1734024 : -6054250, false);
               } else if (limit - hx >= 24) {
                  g.drawString(this.font, GuiPanels.clip(this.font, hint.text(), limit - hx), hx, y, hint.bad() ? -1734024 : -6054250, false);
               }
            } else {
               int textRoom = Math.max(20, textRight - x - 8);
               String reason = this.whyNotWritable(rule, mastery);
               if (reason != null) {
                  g.drawString(this.font, GuiPanels.clip(this.font, reason, textRoom), x, y, -1734024, false);
               } else if (rule.isEmpty()) {
                  g.drawString(this.font, GuiPanels.clip(this.font, example(slot), textRoom), x, y, -6054250, false);
               } else if (stored.on() && rule.equals(stored.rule())) {
                  g.drawString(this.font, GuiPanels.clip(this.font, this.liveStatus(stored), textRoom), x, y, -5659750, false);
               }
            }
         }
      }
   }

   private void drawNameField(GuiGraphics g, int x, int y, int right) {
      int w = Math.max(this.font.width("n".repeat(16)), this.font.width(this.nameBuffer + "_")) + 6;
      cardChip(g, x, y - 2, w, 12, false, true, false);
      GuiPanels.outline(g, x, y - 2, w, 12, ACCENT);
      g.drawString(this.font, this.nameBuffer + "_", x + 3, y, -1185569, false);
      String how = Component.translatable("awakened.command.screen.name_how").getString();
      g.drawString(this.font, GuiPanels.clip(this.font, how, Math.max(20, right - x - w - 8)), x + w + 6, y, -6054250, false);
   }

   private int drawNameChip(GuiGraphics g, int slot, String name, int x, int y, int right, int mx, int my) {
      boolean named = !name.isEmpty();
      int room = Math.max(24, right - x - 8 - 6);
      Component label = (Component)(named ? CardGfx.bold(GuiPanels.clip(this.font, name, room)) : Component.translatable("awakened.command.screen.name_blank"));
      int w = this.font.width(label) + 6;
      boolean hover = this.hovering(mx, my, x, y - 2, w, 12);
      cardChip(g, x, y - 2, w, 12, false, hover, false);
      g.drawString(this.font, label, x + 3, y, named ? -1185569 : -5659750, false);
      this.hits.add(new CommandRulesScreen.Hit(x, y - 2, x + w, y - 2 + 12, CommandRulesScreen.Act.NAME, slot, null, ""));
      if (hover) {
         GuiPanels.queueTooltip(this.font, Component.translatable("awakened.command.screen.name_tip"), 160);
      }

      return x + w + 6;
   }

   private void startNaming(int slot) {
      this.commitName();
      this.namingSlot = slot;
      this.nameBuffer = this.shownName(slot);
   }

   private String shownName(int slot) {
      String synced = CommandData.of(this.minecraft.player).name(slot);
      if (this.sentNames[slot] != null && this.sentNames[slot].equals(synced)) {
         this.sentNames[slot] = null;
      }

      return this.sentNames[slot] != null ? this.sentNames[slot] : synced;
   }

   private void commitName() {
      if (this.namingSlot >= 0) {
         String clean = CommandData.cleanName(this.nameBuffer);
         if (!clean.equals(this.shownName(this.namingSlot))) {
            send(new CommandRenamePayload(this.namingSlot, clean));
            this.sentNames[this.namingSlot] = clean;
         }

         this.namingSlot = -1;
         this.nameBuffer = "";
      }
   }

   private boolean nameKey(int key) {
      if (key == 257 || key == 335) {
         this.commitName();
      } else if (key == 256) {
         this.namingSlot = -1;
         this.nameBuffer = "";
      } else if (key == 259 && !this.nameBuffer.isEmpty()) {
         this.nameBuffer = this.nameBuffer.substring(0, this.nameBuffer.length() - 1);
      }

      return true;
   }

   private int drawCost(GuiGraphics g, int slot, int mastery, int right, int y, int mx, int my) {
      Rule rule = this.draft[slot];
      CommandCost.Breakdown price = CommandCost.breakdown(rule, mastery);
      if (price == null) {
         return right;
      } else {
         String suffix = costSuffix(rule);
         int end = right;
         if (!suffix.isEmpty()) {
            end = right - this.font.width(suffix);
            g.drawString(this.font, suffix, end, y, -6054250, false);
            end -= 3;
         }

         int x = this.costValue(g, String.valueOf(price.points()), end, y);
         if (this.hovering(mx, my, x, y - 1, right - x, 9)) {
            GuiPanels.queueTooltip(this.font, priceTooltip(rule, price), 220);
         }

         return x;
      }
   }

   private static String costSuffix(Rule rule) {
      TargetKind kind = rule.target() == null ? null : rule.target().kind();
      return kind != null && kind.lands() ? Component.translatable("awakened.command.screen.cost_per_body").getString() : "";
   }

   private int costValue(GuiGraphics g, String digits, int right, int y) {
      int gx = right - 5;
      staminaGlyph(g, gx, y + 2);
      int x = gx - 2 - this.font.width(digits);
      g.drawString(this.font, digits, x, y, -5659750, false);
      return x;
   }

   private static void staminaGlyph(GuiGraphics g, int x, int y) {
      g.fill(x, y + 1, x + 3, y + 4, -6054250);
      g.fill(x + 1, y, x + 2, y + 5, -6054250);
   }

   private void drawClearPrompt(GuiGraphics g, int slot, int right, int y, int mx, int my) {
      String no = Component.translatable("awakened.command.screen.no").getString();
      int noW = this.font.width(no);
      int noX = right - noW;
      this.promptWord(
         g, no, noX, y, -5659750, mx, my, new CommandRulesScreen.Hit(noX, y - 1, noX + noW, y + 9, CommandRulesScreen.Act.CLEAR_NO, slot, null, "")
      );
      String yes = Component.translatable("awakened.command.screen.yes").getString();
      int yesW = this.font.width(yes);
      int yesX = noX - 8 - yesW;
      this.promptWord(
         g, yes, yesX, y, -1734024, mx, my, new CommandRulesScreen.Hit(yesX, y - 1, yesX + yesW, y + 9, CommandRulesScreen.Act.CLEAR_YES, slot, null, "")
      );
      String q = Component.translatable("awakened.command.screen.clear_q").getString();
      g.drawString(this.font, q, yesX - 8 - this.font.width(q), y, -1734024, false);
   }

   private void promptWord(GuiGraphics g, String label, int x, int y, int colour, int mx, int my, CommandRulesScreen.Hit hit) {
      boolean hover = this.hovering(mx, my, x, y - 1, this.font.width(label), 9);
      g.drawString(this.font, label, x, y, hover ? -1185569 : colour, false);
      this.hits.add(hit);
   }

   private static Component priceTooltip(Rule rule, CommandCost.Breakdown price) {
      TargetKind kind = rule.target() == null ? null : rule.target().kind();
      String key = kind != null && kind.lands()
         ? "awakened.command.screen.price_tip_body"
         : (kind == TargetKind.NEAR ? "awakened.command.screen.price_tip_aura" : "awakened.command.screen.price_tip_self");
      MutableComponent tip = Component.literal(price.formula())
         .append("\n")
         .append(Component.translatable(price.radial() ? "awakened.command.screen.price_terms_radius" : "awakened.command.screen.price_terms"))
         .append("\n")
         .append(Component.translatable(key, new Object[]{price.points()}));
      if (rule.instant()) {
         tip.append("\n").append(Component.translatable("awakened.command.screen.price_tip_instant"));
      }

      return tip;
   }

   private static String example(int slot) {
      List<CommandPresets.Starter> starters = CommandPresets.all();
      return starters.isEmpty()
         ? ""
         : Component.translatable("awakened.command.screen.example", new Object[]{starters.get(Math.floorMod(slot, starters.size())).rule().describe()})
            .getString();
   }

   private String liveStatus(RuleSlot stored) {
      StringBuilder out = new StringBuilder();
      out.append(
         stored.held() > 0.5F
            ? Component.translatable("awakened.command.screen.on_holding", new Object[]{Math.round(stored.held())}).getString()
            : Component.translatable("awakened.command.screen.on").getString()
      );
      if (stored.heldBodies() > 1) {
         out.append(" . ").append(Component.translatable("awakened.command.screen.bodies_held", new Object[]{stored.heldBodies()}).getString());
      }

      if (stored.bound()) {
         out.append(" . ")
            .append(Component.translatable(stored.boundDormant() ? "awakened.command.screen.bound_waiting" : "awakened.command.screen.bound").getString());
      } else if (stored.ticksLeft() <= 0 && stored.holdEndsIn() > 0) {
         out.append(" . ")
            .append(
               Component.translatable("awakened.command.screen.back_in", new Object[]{Math.max(1, Math.round((float)stored.holdEndsIn() / 20.0F))}).getString()
            );
      } else if (stored.rule().resolves() && stored.rule().target().kind().lands()) {
         out.append(" . ").append(Component.translatable("awakened.command.screen.waiting_hit").getString());
      }

      out.append(" . ");
      if (this.live(stored)) {
         out.append(Component.translatable("awakened.command.screen.live_now").getString());
      } else {
         out.append(Component.translatable("awakened.command.screen.not_live", new Object[]{stored.rule().trigger().displayName()}).getString());
      }

      return out.toString();
   }

   private boolean live(RuleSlot stored) {
      return this.minecraft != null && this.minecraft.player != null && stored.rule().resolves() && stored.rule().trigger().liveOnClient(this.minecraft.player);
   }

   @Nullable
   private CommandRulesScreen.Reveal drawSentence(GuiGraphics g, int slot, CommandRulesScreen.Grid shared, int room, int y, int mx, int my, int mastery) {
      int chipY = y + 2;
      CommandRulesScreen.Reveal reveal = null;
      List<CommandRulesScreen.Piece> pieces = this.sentencePieces(slot, this.draft[slot], mastery);
      CommandRulesScreen.Grid grid = shared.shared()
         ? shared
         : this.layout(List.of(pieces), shared.scale(), shared.x(CommandRulesScreen.Col.SUBJECT), room, false);

      for (CommandRulesScreen.Piece piece : pieces) {
         int x = grid.x(piece.col());
         int w = grid.w(piece.col());
         switch (piece.kind()) {
            case CHIP:
               this.drawWordChip(g, piece, slot, x, chipY, w, grid.scale(), mx, my);
               break;
            case NUMBER:
               CommandRulesScreen.Reveal asked = this.drawNumberChip(g, piece, slot, x, chipY, w, grid.scale(), mx, my);
               if (asked != null) {
                  reveal = asked;
               }
               break;
            case TEXT:
               g.drawString(this.font, piece.text(), x, y + (20 - 9) / 2, -5659750, false);
         }
      }

      return reveal;
   }

   private CommandRulesScreen.Grid buildGrid(CommandData rules, int mastery, int textX, int right) {
      List<List<CommandRulesScreen.Piece>> rows = new ArrayList<>(5);
      int room = right - textX;

      for (int slot = 0; slot < 5; slot++) {
         if (mastery >= CommandPower.slotUnlockLevel(slot)) {
            Rule rule = this.draft[slot];
            room = Math.min(room, right - this.laneWidth() - 6 - textX);
            rows.add(this.sentencePieces(slot, rule, mastery));
         }
      }

      room = Math.max(80, room);
      float scale = 1.25F;

      int[] w;
      for (w = this.columnWidths(rows, scale); scale > 1.0F && total(w) > room; w = this.columnWidths(rows, scale)) {
         scale = Math.max(1.0F, scale - 0.05F);
      }

      return this.layout(rows, scale, textX, room, total(w) <= room);
   }

   private CommandRulesScreen.Grid layout(List<List<CommandRulesScreen.Piece>> rows, float scale, int textX, int room, boolean shared) {
      int[] w = this.columnWidths(rows, scale);
      squeezeColumns(w, total(w) - room);
      int[] x = new int[CommandRulesScreen.Col.values().length];
      int at = textX;

      for (int i = 0; i < x.length; i++) {
         x[i] = at;
         if (w[i] > 0) {
            at += w[i] + 4;
         }
      }

      return new CommandRulesScreen.Grid(scale, x, w, shared);
   }

   private int[] columnWidths(List<List<CommandRulesScreen.Piece>> rows, float scale) {
      int[] w = new int[CommandRulesScreen.Col.values().length];

      for (List<CommandRulesScreen.Piece> row : rows) {
         for (CommandRulesScreen.Piece piece : row) {
            int i = piece.col().ordinal();
            w[i] = Math.max(w[i], this.pieceWidth(piece, scale));
         }
      }

      return w;
   }

   private int pieceWidth(CommandRulesScreen.Piece piece, float scale) {
      int text = piece.kind() != CommandRulesScreen.PieceKind.CHIP && piece.kind() != CommandRulesScreen.PieceKind.NUMBER
         ? this.font.width(piece.text())
         : Math.round((float)this.font.width(piece.text()) * scale);

      return switch (piece.kind()) {
         case CHIP -> 6 + text;
         case NUMBER -> 6 + text;
         case TEXT -> text;
      };
   }

   private static int total(int[] w) {
      int sum = 0;

      for (int width : w) {
         if (width > 0) {
            sum += width + 4;
         }
      }

      return Math.max(0, sum - 4);
   }

   private static void squeezeColumns(int[] w, int over) {
      if (over > 0) {
         int floor = 28;
         int pool = 0;

         for (CommandRulesScreen.Col col : new CommandRulesScreen.Col[]{
            CommandRulesScreen.Col.SUBJECT, CommandRulesScreen.Col.VERB, CommandRulesScreen.Col.TRIGGER
         }) {
            pool += Math.max(0, w[col.ordinal()] - floor);
         }

         if (pool > 0) {
            int asked = over;

            for (CommandRulesScreen.Col col : new CommandRulesScreen.Col[]{
               CommandRulesScreen.Col.SUBJECT, CommandRulesScreen.Col.VERB, CommandRulesScreen.Col.TRIGGER
            }) {
               int i = col.ordinal();
               int give = Math.min(
                  Math.max(0, w[i] - floor), Math.min(over, Math.max(1, Math.round((float)asked * ((float)Math.max(0, w[i] - floor) / (float)pool))))
               );
               w[i] -= give;
               over -= give;
            }
         }
      }
   }

   private List<CommandRulesScreen.Piece> sentencePieces(int slot, Rule rule, int mastery) {
      List<CommandRulesScreen.Piece> pieces = new ArrayList<>(8);
      Target target = rule.target();
      Effect effect = rule.effect();
      Trigger trigger = rule.trigger();
      pieces.add(this.wordPiece(target, CommandRulesScreen.Col.SUBJECT, CommandRulesScreen.Blank.TARGET, "awakened.command.screen.pick_target", mastery));
      pieces.add(this.wordPiece(effect, CommandRulesScreen.Col.VERB, CommandRulesScreen.Blank.EFFECT, "awakened.command.screen.pick_effect", mastery));
      if (target != null && effect != null) {
         if (effect.hasLevel()) {
            pieces.add(
               numberPiece(
                  CommandRulesScreen.Col.LEVEL,
                  CommandRulesScreen.Blank.LEVEL,
                  rule.level(),
                  numberCap(CommandRulesScreen.Blank.LEVEL, mastery),
                  effect::numberText
               )
            );
         }

         if (target.usesRadius()) {
            pieces.add(
               new CommandRulesScreen.Piece(
                  CommandRulesScreen.PieceKind.TEXT,
                  CommandRulesScreen.Col.WITHIN,
                  Component.translatable("awakened.command.screen.within").getString(),
                  "",
                  null,
                  CommandRulesScreen.Act.PICK,
                  CommandRulesScreen.ChipState.FILLED
               )
            );
            pieces.add(
               numberPiece(
                  CommandRulesScreen.Col.RADIUS,
                  CommandRulesScreen.Blank.RADIUS,
                  rule.radius(),
                  numberCap(CommandRulesScreen.Blank.RADIUS, mastery),
                  blocks -> Component.translatable("awakened.command.unit.blocks", new Object[]{blocks})
               )
            );
         }

         if (!rule.instant()) {
            pieces.add(
               new CommandRulesScreen.Piece(
                  CommandRulesScreen.PieceKind.TEXT,
                  CommandRulesScreen.Col.FOR,
                  Component.translatable("awakened.command.screen.for").getString(),
                  "",
                  null,
                  CommandRulesScreen.Act.PICK,
                  CommandRulesScreen.ChipState.FILLED
               )
            );
            int durationCap = CommandCost.durationCap(mastery);
            boolean infinite = rule.infinite();
            pieces.add(
               new CommandRulesScreen.Piece(
                  CommandRulesScreen.PieceKind.NUMBER,
                  CommandRulesScreen.Col.SECONDS,
                  infinite
                     ? Component.translatable("awakened.command.rule.forever").getString()
                     : Component.translatable("awakened.command.screen.n_seconds", new Object[]{rule.seconds()}).getString(),
                  infinite ? "" : Component.translatable("awakened.command.screen.of_seconds", new Object[]{durationCap}).getString(),
                  CommandRulesScreen.Blank.SECONDS,
                  CommandRulesScreen.Act.NUMBER_UP,
                  !infinite && rule.seconds() > durationCap ? CommandRulesScreen.ChipState.BAD : CommandRulesScreen.ChipState.FILLED
               )
            );
         }

         pieces.add(this.wordPiece(trigger, CommandRulesScreen.Col.TRIGGER, CommandRulesScreen.Blank.TRIGGER, "awakened.command.screen.pick_trigger", mastery));
         return this.withTypedNumber(slot, pieces);
      } else {
         return pieces;
      }
   }

   private static CommandRulesScreen.Piece numberPiece(
      CommandRulesScreen.Col col, CommandRulesScreen.Blank blank, int value, int cap, IntFunction<Component> unit
   ) {
      boolean over = value > cap;
      String hint = Component.translatable(over ? "awakened.command.screen.over_limit" : "awakened.command.screen.of", new Object[]{unit.apply(cap)})
         .getString();
      return new CommandRulesScreen.Piece(
         CommandRulesScreen.PieceKind.NUMBER,
         col,
         unit.apply(value).getString(),
         hint,
         blank,
         CommandRulesScreen.Act.NUMBER_UP,
         over ? CommandRulesScreen.ChipState.BAD : CommandRulesScreen.ChipState.FILLED
      );
   }

   private List<CommandRulesScreen.Piece> withTypedNumber(int slot, List<CommandRulesScreen.Piece> pieces) {
      if (!this.numberBuffer.isEmpty() && slot == this.focusSlot && this.focusBlank != null) {
         for (int i = 0; i < pieces.size(); i++) {
            CommandRulesScreen.Piece piece = pieces.get(i);
            if (piece.kind() == CommandRulesScreen.PieceKind.NUMBER && piece.blank() == this.focusBlank) {
               pieces.set(
                  i, new CommandRulesScreen.Piece(piece.kind(), piece.col(), this.numberBuffer + "_", piece.hint(), piece.blank(), piece.act(), piece.state())
               );
            }
         }

         return pieces;
      } else {
         return pieces;
      }
   }

   private CommandRulesScreen.Piece wordPiece(
      @Nullable Word word, CommandRulesScreen.Col col, CommandRulesScreen.Blank blank, String placeholderKey, int mastery
   ) {
      if (word == null) {
         return new CommandRulesScreen.Piece(
            CommandRulesScreen.PieceKind.CHIP,
            col,
            Component.translatable(placeholderKey).getString(),
            "",
            blank,
            CommandRulesScreen.Act.PICK,
            CommandRulesScreen.ChipState.EMPTY
         );
      } else {
         CommandRulesScreen.ChipState state = word.unlockLevel() > mastery ? CommandRulesScreen.ChipState.LOCKED : CommandRulesScreen.ChipState.FILLED;
         return new CommandRulesScreen.Piece(
            CommandRulesScreen.PieceKind.CHIP, col, word.displayName().getString(), "", blank, CommandRulesScreen.Act.PICK, state
         );
      }
   }

   private void drawWordChip(GuiGraphics g, CommandRulesScreen.Piece piece, int slot, int x, int y, int w, float scale, int mx, int my) {
      boolean open = this.pickerBlank == piece.blank() && this.pickerSlot == slot;
      boolean hover = this.hovering(mx, my, x, y, w, 16);
      this.chipFace(g, x, y, w, 16, piece.state(), hover, open);
      String text = GuiPanels.clip(this.font, piece.text(), Math.max(8, Math.round((float)(w - 6) / scale)));
      HudText.scaled(g, this.font, text, (float)(x + 3), (float)y + (16.0F - 9.0F * scale) / 2.0F, scale, 0, 0, this.chipInk(piece.state(), slot), false);
      this.hits.add(new CommandRulesScreen.Hit(x, y, x + w, y + 16, CommandRulesScreen.Act.PICK, slot, piece.blank(), ""));
      if (open) {
         this.cardAnchorX = x;
         this.cardAnchorY = y;
         this.cardAnchorW = w;
         this.cardAnchorSeen = true;
      }

      this.drawFocusMark(g, slot, piece.blank(), x, y, w);
      this.queueWordTooltip(piece, slot, hover);
   }

   @Nullable
   private CommandRulesScreen.Reveal drawNumberChip(
      GuiGraphics g, CommandRulesScreen.Piece piece, int slot, int x, int y, int colW, float scale, int mx, int my
   ) {
      boolean hover = this.hovering(mx, my, x, y, colW, 16);
      this.chipFace(g, x, y, colW, 16, piece.state(), hover, false);
      String shown = this.shownNumber(piece, slot);
      float textW = (float)this.font.width(shown) * scale;
      HudText.scaled(
         g, this.font, shown, (float)(x + colW - 3) - textW, (float)y + (16.0F - 9.0F * scale) / 2.0F, scale, 0, 0, this.chipInk(piece.state(), slot), false
      );
      this.hits.add(new CommandRulesScreen.Hit(x, y, x + colW, y + 16, piece.act(), slot, piece.blank(), ""));
      this.drawFocusMark(g, slot, piece.blank(), x, y, colW);
      if (hover && piece.blank() == CommandRulesScreen.Blank.LEVEL) {
         this.queueStrengthCapTooltip();
      }

      boolean bad = piece.state() == CommandRulesScreen.ChipState.BAD;
      boolean asking = hover || bad || slot == this.focusSlot && piece.blank() == this.focusBlank;
      return asking && !piece.hint().isEmpty() ? new CommandRulesScreen.Reveal(piece.hint(), x, bad) : null;
   }

   private String shownNumber(CommandRulesScreen.Piece piece, int slot) {
      if (piece.blank() == CommandRulesScreen.Blank.SECONDS && (this.numberBuffer.isEmpty() || slot != this.focusSlot)) {
         RuleSlot stored = CommandData.of(this.minecraft.player).slot(slot);
         int left = stored.secondsLeft();
         return left >= 0 && this.draft[slot].equals(stored.rule())
            ? Component.translatable("awakened.command.screen.n_seconds", new Object[]{left}).getString()
            : piece.text();
      } else {
         return piece.text();
      }
   }

   private void chipFace(GuiGraphics g, int x, int y, int w, int h, CommandRulesScreen.ChipState state, boolean hover, boolean open) {
      switch (state) {
         case LOCKED:
            cardChip(g, x, y, w, h, false, false, true);
            break;
         case BAD:
            g.fill(x, y, x + w, y + h, -15461872);
            g.fill(x + 1, y + 1, x + w - 1, y + h - 1, GuiPanels.blend(-12962254, -1734024, 0.25F));
            break;
         default:
            cardChip(g, x, y, w, h, false, hover || open, false);
            if (open) {
               GuiPanels.outline(g, x, y, w, h, ACCENT);
            }
      }
   }

   private static void cardChip(GuiGraphics g, int x, int y, int w, int h, boolean lit, boolean hover, boolean locked) {
      if (w > 0 && h > 0) {
         g.fill(x, y, x + w, y + h, locked ? -12962254 : -15461872);
         int face = locked ? -14803942 : (lit ? (hover ? -1646120 : -2567480) : (hover ? -12041411 : -12962254));
         g.fill(x + 1, y + 1, x + w - 1, y + h - 1, face);
      }
   }

   private int chipInk(CommandRulesScreen.ChipState state, int slot) {
      return switch (state) {
         case EMPTY -> -5659750;
         case FILLED -> slot == this.focusSlot ? -1185569 : -3554376;
         case LOCKED -> -6054250;
         case BAD -> -1734024;
      };
   }

   private void drawFocusMark(GuiGraphics g, int slot, @Nullable CommandRulesScreen.Blank blank, int x, int y, int w) {
      if (slot == this.focusSlot && blank != null && blank == this.focusBlank && this.pickerBlank == null) {
         g.fill(x + 1, y + 16, x + w - 1, y + 16 + 1, -5659750);
      }
   }

   private void drawCard(GuiGraphics g, int mastery, int panelX, int mx, int my) {
      List<? extends Word> whole = book(this.pickerBlank);
      List<? extends Word> shown = this.filtered(whole);
      Rule rule = this.draft[this.pickerSlot];
      int cols = this.cardColumns(whole, rule, mastery);
      int colW = this.cardColumnWidth(whole, rule, mastery);
      int bookRows = Math.max(1, (whole.size() + cols - 1) / cols);
      int innerW = Math.max(cols * colW + (cols - 1) * 6, Math.min(240, this.cardRoom()));
      List<CommandPresets.Starter> starters = rule.isEmpty() ? CommandPresets.all() : List.of();
      List<List<CommandPresets.Starter>> starterRows = startersOpen ? this.wrapStarters(starters, innerW) : List.of();
      int starterH = starters.isEmpty() ? 0 : 14 + starterRows.size() * 14 + 4;
      int cardW = innerW + 16;
      int footLines = this.footLines(whole, innerW);
      int fixedH = 18 + starterH + 4 + footLines * 9 + 8;
      int screenTop = this.unscale(0, this.uiPivotY) + 4;
      int floor = this.unscale(this.height, this.uiPivotY) - 4;
      int belowTop = this.cardAnchorY + 16 + 4;
      int roomBelow = floor - belowTop;
      int roomAbove = this.cardAnchorY - 4 - screenTop;
      int fullH = fixedH + bookRows * 11;
      boolean below = fullH <= roomBelow || fullH > roomAbove && roomBelow >= roomAbove;
      int side = below ? roomBelow : roomAbove;
      int gridRows = Math.min(bookRows, Math.max(3, (side - fixedH) / 11));
      this.cardVisibleRows = gridRows;
      int rows = Math.max(1, (shown.size() + cols - 1) / cols);
      this.pickerScroll = Mth.clamp(this.pickerScroll, 0, Math.max(0, rows - gridRows));
      int cardH = fixedH + gridRows * 11;
      int screenLeft = this.unscale(0, this.uiPivotX) + 4;
      int screenRight = this.unscale(this.width, this.uiPivotX) - 4;
      int left = Math.max(screenLeft, Math.min(this.cardAnchorX, screenRight - cardW));
      int top = below ? belowTop : this.cardAnchorY - 4 - cardH;
      this.cardX0 = left;
      this.cardY0 = top;
      this.cardX1 = left + cardW;
      this.cardY1 = top + cardH;
      CardGfx gfx = new CardGfx(g, this.font, NO_HITS, ACCENT, mx, my, 1.0F);
      gfx.fill(left, top, cardW, cardH, -13751512);
      int tickX = Mth.clamp(this.cardAnchorX + this.cardAnchorW / 2, left + 4, this.cardX1 - 6);
      if (below) {
         g.fill(tickX - 1, this.cardAnchorY + 16, tickX + 1, top, ACCENT);
      } else {
         g.fill(tickX - 1, this.cardY1, tickX + 1, this.cardAnchorY, ACCENT);
      }

      int cx = left + 8;
      int cy = top + 8;
      String typed = this.filter.isEmpty() ? "" : Component.translatable("awakened.command.screen.filter", new Object[]{this.filter}).getString();
      int typedW = typed.isEmpty() ? 0 : this.font.width(typed) + 6;
      g.drawString(
         this.font,
         GuiPanels.clip(
            this.font,
            Component.translatable(
                  "awakened.command.screen.pick_header",
                  new Object[]{Component.translatable("awakened.command.screen.blank_" + this.pickerBlank.name().toLowerCase(Locale.ROOT))}
               )
               .getString(),
            Math.max(20, innerW - typedW)
         ),
         cx,
         cy,
         -5659750,
         false
      );
      if (!typed.isEmpty()) {
         g.drawString(this.font, typed, cx + innerW - this.font.width(typed), cy, -6054250, false);
      }

      cy += 10;
      if (!starters.isEmpty()) {
         cy = this.drawStarterRows(g, starterRows, cx, cy, mx, my) + 4;
      }

      String description = null;
      Word hovered = null;

      for (int i = 0; i < shown.size(); i++) {
         int row = i / cols - this.pickerScroll;
         if (row >= 0 && row < gridRows) {
            Word word = shown.get(i);
            int x0 = cx + i % cols * (colW + 6);
            int y0 = cy + row * 11;
            String reason = whyNot(word, rule, mastery);
            boolean pickable = reason == null;
            boolean hover = inBox(mx, my, x0, y0, colW, 10);
            if (hover || i == this.pickerIndex) {
               g.fill(x0, y0, x0 + colW, y0 + 11 - 1, -13225168);
            }

            if (hover) {
               hovered = word;
               description = word.description().getString();
            }

            boolean chosen = word.id().equals(chosenId(rule, this.pickerBlank));
            String note = pickable ? "" : reason;
            int noteW = note.isEmpty() ? 0 : this.font.width(note) + 4;
            String name = GuiPanels.clip(this.font, word.displayName().getString(), Math.max(16, colW - 6 - noteW));
            g.drawString(this.font, name, x0 + 3, y0 + 2, !pickable ? -6054250 : (chosen ? ACCENT : -3554376), false);
            if (!note.isEmpty()) {
               g.drawString(
                  this.font,
                  note,
                  x0 + colW - 3 - this.font.width(note),
                  y0 + 2,
                  word.unlockLevel() > mastery ? -6054250 : GuiPanels.withAlpha(-1734024, 204),
                  false
               );
            }

            this.cardHits
               .add(
                  new CommandRulesScreen.Hit(
                     x0,
                     y0,
                     x0 + colW,
                     y0 + 11 - 1,
                     pickable ? CommandRulesScreen.Act.PICK_WORD : CommandRulesScreen.Act.PICK_REFUSED,
                     this.pickerSlot,
                     this.pickerBlank,
                     pickable ? word.id() : reason
                  )
               );
         }
      }

      if (hovered == null && this.pickerIndex >= 0 && this.pickerIndex < shown.size()) {
         hovered = shown.get(this.pickerIndex);
         description = hovered.description().getString();
      }

      this.drawCardFooter(g, hovered, description, cx, cy + gridRows * 11 + 4, innerW, footLines, shown.isEmpty());
      CardChrome.frame(gfx, left, top, cardW, cardH);
   }

   private void drawCardFooter(GuiGraphics g, @Nullable Word hovered, @Nullable String description, int x, int y, int room, int maxLines, boolean empty) {
      if (this.noteTicks > 0 && this.note != null) {
         g.drawString(this.font, GuiPanels.clip(this.font, this.note, room), x, y, -1734024, false);
      } else if (empty) {
         g.drawString(this.font, Component.translatable("awakened.command.screen.no_match").getString(), x, y, -6054250, false);
      } else {
         if (hovered != null) {
            String contribution = contribution(hovered);
            if (!contribution.isEmpty()) {
               g.drawString(this.font, contribution, x + room - this.font.width(contribution), y, -6054250, false);
            }
         }

         if (description == null) {
            g.drawString(this.font, Component.translatable("awakened.command.screen.pick_hint").getString(), x, y, -6054250, false);
         } else {
            int width = hovered == null ? room : this.descriptionWidth(hovered, room);
            List<FormattedCharSequence> lines = GuiPanels.clipLines(this.font, Component.literal(description), width, maxLines);

            for (int i = 0; i < lines.size() && i < maxLines; i++) {
               g.drawString(this.font, lines.get(i), x, y + i * 9, -3554376, false);
            }
         }
      }
   }

   private int footLines(List<? extends Word> book, int room) {
      int most = 2;

      for (Word word : book) {
         most = Math.max(most, this.font.split(word.description(), this.descriptionWidth(word, room)).size());
      }

      return Math.min(4, most);
   }

   private int descriptionWidth(Word word, int room) {
      String contribution = contribution(word);
      int contribW = contribution.isEmpty() ? 0 : this.font.width(contribution) + 6;
      return Math.max(40, room - contribW);
   }

   private int cardColumnWidth(List<? extends Word> book, Rule rule, int mastery) {
      int widest = 60;

      for (Word word : book) {
         String reason = whyNot(word, rule, mastery);
         int noteW = reason == null ? 0 : this.font.width(reason) + 4;
         widest = Math.max(widest, this.font.width(word.displayName().getString()) + noteW + 8);
      }

      return Math.min(widest, this.cardRoom());
   }

   private int cardRoom() {
      return 428;
   }

   private int cardColumns(List<? extends Word> book, Rule rule, int mastery) {
      int colW = this.cardColumnWidth(book, rule, mastery);
      int widest = Mth.clamp((this.cardRoom() + 6) / (colW + 6), 1, 5);
      int rowsFit = this.cardRowsFit(rule);
      int needed = Math.max(1, (book.size() + rowsFit - 1) / rowsFit);
      return Math.min(needed, widest);
   }

   private int cardRowsFit(Rule rule) {
      int starterH = rule.isEmpty() && !CommandPresets.all().isEmpty() ? 14 + (startersOpen ? 42 : 0) + 4 : 0;
      int fixedH = 18 + starterH + 4 + 36 + 8;
      int screenTop = this.unscale(0, this.uiPivotY) + 4;
      int floor = this.unscale(this.height, this.uiPivotY) - 4;
      int side = Math.max(floor - (this.cardAnchorY + 16 + 4), this.cardAnchorY - 4 - screenTop);
      return Math.max(6, (side - fixedH) / 11);
   }

   private List<List<CommandPresets.Starter>> wrapStarters(List<CommandPresets.Starter> starters, int room) {
      if (starters.isEmpty()) {
         return List.of();
      } else {
         List<List<CommandPresets.Starter>> rows = new ArrayList<>(2);
         List<CommandPresets.Starter> row = new ArrayList<>(4);
         int x = 0;

         for (CommandPresets.Starter starter : starters) {
            int w = this.starterWidth(starter) + 4;
            if (!row.isEmpty() && x + w > room) {
               rows.add(row);
               row = new ArrayList<>(4);
               x = 0;
            }

            row.add(starter);
            x += w;
         }

         if (!row.isEmpty()) {
            rows.add(row);
         }

         return rows;
      }
   }

   private int starterWidth(CommandPresets.Starter starter) {
      String name = this.starterLabel(starter);
      return this.font.width(name) + 8;
   }

   private String starterLabel(CommandPresets.Starter starter) {
      String name = starter.displayName().getString();
      return starter.unlockLevel() > this.mastery()
         ? name + " " + Component.translatable("awakened.command.screen.needs_mastery", new Object[]{starter.unlockLevel()}).getString()
         : name;
   }

   private int drawStarterRows(GuiGraphics g, List<List<CommandPresets.Starter>> rows, int cx, int cy, int mx, int my) {
      String label = Component.translatable("awakened.command.screen.starters").getString();
      int labelW = this.font.width(label) + 8;
      boolean labelHover = inBox(mx, my, cx, cy, labelW, 12);
      cardChip(g, cx, cy, labelW, 12, startersOpen, labelHover, false);
      g.drawString(this.font, label, cx + 4, cy + 2, startersOpen ? -13751512 : -1185569, false);
      this.cardHits.add(new CommandRulesScreen.Hit(cx, cy, cx + labelW, cy + 12, CommandRulesScreen.Act.STARTERS, this.pickerSlot, null, ""));
      int x = cx;
      int y = cy + 14;

      for (List<CommandPresets.Starter> row : rows) {
         for (CommandPresets.Starter starter : row) {
            int w = this.starterWidth(starter);
            boolean locked = starter.unlockLevel() > this.mastery();
            boolean hover = !locked && inBox(mx, my, x, y, w, 12);
            cardChip(g, x, y, w, 12, false, hover, locked);
            g.drawString(this.font, this.starterLabel(starter), x + 4, y + 2, locked ? -6054250 : -3554376, false);
            if (!locked) {
               this.cardHits.add(new CommandRulesScreen.Hit(x, y, x + w, y + 12, CommandRulesScreen.Act.PRESET, this.pickerSlot, null, starter.id()));
            }

            x += w + 4;
         }

         y += 14;
         x = cx;
      }

      return rows.isEmpty() ? cy + 14 : y;
   }

   private List<? extends Word> filtered(List<? extends Word> book) {
      if (this.filter.isEmpty()) {
         return book;
      } else {
         String needle = this.filter.toLowerCase(Locale.ROOT);
         List<Word> kept = new ArrayList<>(book.size());

         for (Word word : book) {
            if (word.displayName().getString().toLowerCase(Locale.ROOT).contains(needle)) {
               kept.add(word);
            }
         }

         return kept;
      }
   }

   private static String contribution(Word word) {
      if (word instanceof Effect effect) {
         return Component.translatable("awakened.command.screen.base", new Object[]{Math.round(CommandConfig.weight(effect))}).getString();
      } else {
         return word instanceof Target target
            ? Component.translatable("awakened.command.screen.times", new Object[]{String.format(Locale.ROOT, "%.1f", CommandConfig.weight(target))})
               .getString()
            : "";
      }
   }

   private void closeCard() {
      this.pickerBlank = null;
      this.pickerSlot = -1;
      this.pickerScroll = 0;
      this.pickerIndex = -1;
      this.filter = "";
   }

   private void openCard(int slot, @Nullable CommandRulesScreen.Blank blank) {
      this.pickerBlank = blank;
      this.pickerSlot = slot;
      this.pickerScroll = 0;
      this.filter = "";
      this.focusSlot = slot;
      List<? extends Word> book = book(blank);
      String chosen = chosenId(this.draft[slot], blank);
      this.pickerIndex = 0;

      for (int i = 0; i < book.size(); i++) {
         if (book.get(i).id().equals(chosen)) {
            this.pickerIndex = i;
            break;
         }
      }
   }

   private static List<? extends Word> book(@Nullable CommandRulesScreen.Blank blank) {
      return switch (blank == null ? CommandRulesScreen.Blank.TARGET : blank) {
         case TARGET -> List.copyOf(Vocabulary.targets());
         case EFFECT -> List.copyOf(Vocabulary.effects());
         default -> List.copyOf(Vocabulary.triggers());
      };
   }

   private static String chosenId(Rule rule, @Nullable CommandRulesScreen.Blank blank) {
      return switch (blank == null ? CommandRulesScreen.Blank.TARGET : blank) {
         case TARGET -> rule.targetId();
         case EFFECT -> rule.effectId();
         default -> rule.triggerId();
      };
   }

   private static String whyNot(Word word, Rule rule, int mastery) {
      if (word.unlockLevel() > mastery) {
         return Component.translatable("awakened.command.screen.needs_mastery", new Object[]{word.unlockLevel()}).getString();
      } else {
         Target target = rule.target();
         Effect effect = rule.effect();
         if (word instanceof Effect picked && target != null && !picked.supports(target.kind())) {
            return pairingReason(picked, target.kind());
         }

         if (word instanceof Target picked && effect != null && !effect.supports(picked.kind())) {
            return pairingReason(effect, picked.kind());
         }

         Target withTarget = word instanceof Target picked ? picked : target;
         Effect withEffect = word instanceof Effect pickedx ? pickedx : effect;
         Trigger withTrigger = word instanceof Trigger pickedxx ? pickedxx : rule.trigger();
         if (withTarget != null && withEffect != null && withTrigger != null) {
            Clash clash = Clash.of(withTarget, withEffect, withTrigger);
            if (clash != null) {
               return Component.translatable(clash.noteKey()).getString();
            }
         }

         return null;
      }
   }

   private static String pairingReason(Effect effect, TargetKind refused) {
      boolean self = effect.supports(TargetKind.SELF);
      boolean others = effect.supports(TargetKind.HIT);
      if (refused != TargetKind.SELF && self && !others) {
         return Component.translatable("awakened.command.screen.only_you").getString();
      } else {
         return refused == TargetKind.SELF && !self && others
            ? Component.translatable("awakened.command.screen.only_others").getString()
            : Component.translatable("awakened.command.screen.unsupported").getString();
      }
   }

   @Nullable
   private String whyNotWritable(Rule rule, int mastery) {
      if (rule.target() == null) {
         return rule.effect() == null ? null : Component.translatable("awakened.command.screen.need_target").getString();
      } else if (rule.effect() == null) {
         return Component.translatable("awakened.command.screen.need_effect").getString();
      } else if (!rule.resolves()) {
         return Component.translatable("awakened.command.screen.need_trigger").getString();
      } else if (!fits(rule, mastery)) {
         int points = CommandCost.points(rule, mastery);
         return Component.translatable("awakened.command.screen.too_big", new Object[]{points, Math.round(ClientPowerState.get().staminaMax())}).getString();
      } else {
         return null;
      }
   }

   private static boolean fits(Rule rule, int mastery) {
      float max = ClientPowerState.get().staminaMax();
      return max <= 0.0F || (float)CommandCost.points(rule, mastery) <= max;
   }

   private void queueWordTooltip(CommandRulesScreen.Piece piece, int slot, boolean hover) {
      if (hover && piece.blank() != null && piece.state() != CommandRulesScreen.ChipState.EMPTY) {
         Word word = wordOf(this.draft[slot], piece.blank());
         if (word != null) {
            GuiPanels.queueTooltip(this.font, word.description(), 190);
         }
      }
   }

   @Nullable
   private static Word wordOf(Rule rule, CommandRulesScreen.Blank blank) {
      return (Word)(switch (blank) {
         case TARGET -> rule.target();
         case EFFECT -> rule.effect();
         case TRIGGER -> rule.trigger();
         default -> null;
      });
   }

   private boolean hovering(int mx, int my, int x, int y, int w, int h) {
      return this.pickerBlank == null && inBox(mx, my, x, y, w, h);
   }

   private static boolean inBox(int mx, int my, int x, int y, int w, int h) {
      return mx >= x && mx <= x + w && my >= y && my <= y + h;
   }

   private int mastery() {
      return ClientPowerState.get().masteryLevel();
   }

   private static float abilityPower() {
      return ClientPowerState.get().abilityPower();
   }

   private static int numberCap(CommandRulesScreen.Blank blank, int mastery) {
      return blank == CommandRulesScreen.Blank.RADIUS ? CommandCost.radiusCap(mastery) : CommandCost.strengthCap(mastery, abilityPower());
   }

   private void queueStrengthCapTooltip() {
      int mastery = this.mastery();
      int fromMastery = CommandCost.levelCap(mastery);
      int cap = numberCap(CommandRulesScreen.Blank.LEVEL, mastery);
      if (cap > fromMastery) {
         GuiPanels.queueTooltip(
            this.font, Component.translatable("awakened.command.screen.strength_cap", new Object[]{cap, fromMastery, cap - fromMastery}), 200
         );
      }
   }

   private void syncDrafts(CommandData rules) {
      for (int i = 0; i < 5; i++) {
         Rule stored = rules.slot(i).rule();
         if (this.draft[i] == null || !stored.equals(this.known[i])) {
            this.draft[i] = stored;
            this.known[i] = stored;
         }
      }
   }

   private void pick(int slot, CommandRulesScreen.Blank blank, String wordId) {
      Rule rule = this.draft[slot];

      Rule next = switch (blank) {
         case TARGET -> rule.withTarget(wordId);
         case EFFECT -> rule.withEffect(wordId).withSeconds(rule.instant() ? 0 : rule.seconds());
         default -> rule.withTrigger(wordId);
      };
      this.draft[slot] = this.settle(next);
      this.focusSlot = slot;
      this.closeCard();
   }

   private Rule settle(Rule rule) {
      Target target = rule.target();
      Effect effect = rule.effect();
      String effectId = rule.effectId();
      if (target != null && effect != null && (!effect.supports(target.kind()) || rule.trigger() != null && Clash.of(target, effect, rule.trigger()) != null)) {
         effectId = "";
      }

      int level = Mth.clamp(rule.level(), 1, numberCap(CommandRulesScreen.Blank.LEVEL, this.mastery()));
      int radius = rule.radius();
      if (target != null && target.usesRadius()) {
         radius = Mth.clamp(radius > 0 ? radius : 4, 1, numberCap(CommandRulesScreen.Blank.RADIUS, this.mastery()));
      }

      int seconds = rule.seconds();
      if (seconds == 0) {
         seconds = Math.min(5, CommandCost.durationCap(this.mastery()));
      } else if (seconds != -1) {
         seconds = Mth.clamp(seconds, 1, CommandCost.durationCap(this.mastery()));
      } else if (target != null && !target.allowsInfinite()) {
         seconds = CommandCost.durationCap(this.mastery());
      }

      return new Rule(rule.targetId(), effectId, level, radius, seconds, rule.triggerId());
   }

   private static int numberOf(Rule rule, CommandRulesScreen.Blank blank) {
      return blank == CommandRulesScreen.Blank.RADIUS ? rule.radius() : rule.level();
   }

   private Rule withNumber(Rule rule, CommandRulesScreen.Blank blank, int value) {
      int clamped = Mth.clamp(value, 1, numberCap(blank, this.mastery()));
      return blank == CommandRulesScreen.Blank.RADIUS ? rule.withRadius(clamped) : rule.withLevel(clamped);
   }

   private void step(int slot, CommandRulesScreen.Blank blank, int delta) {
      Rule rule = this.draft[slot];
      this.focusSlot = slot;
      if (blank != CommandRulesScreen.Blank.SECONDS) {
         this.draft[slot] = this.withNumber(rule, blank, numberOf(rule, blank) + delta);
      } else {
         int cap = CommandCost.durationCap(this.mastery());
         int seconds;
         if (rule.infinite()) {
            seconds = delta < 0 ? cap : -1;
         } else {
            int next = rule.seconds() + delta;
            if (next > cap) {
               seconds = this.foreverAllowed(rule) ? -1 : cap;
            } else {
               seconds = Math.max(1, next);
            }
         }

         this.draft[slot] = rule.withSeconds(seconds);
      }
   }

   private boolean foreverAllowed(Rule rule) {
      return CommandCost.allowsInfinite(this.mastery()) && rule.allowsInfinite();
   }

   private void applyStarter(int slot, String id) {
      for (CommandPresets.Starter starter : CommandPresets.all()) {
         if (starter.id().equals(id)) {
            this.draft[slot] = this.settle(starter.rule());
            this.focusSlot = slot;
            this.closeCard();
            return;
         }
      }
   }

   private boolean handleClick(int mx, int my) {
      mx = this.unscale(mx, this.uiPivotX);
      my = this.unscale(my, this.uiPivotY);
      this.commitNumber();
      this.commitName();
      if (this.pickerBlank != null) {
         for (CommandRulesScreen.Hit hit : this.cardHits) {
            if (hit.has(mx, my)) {
               this.act(hit);
               return true;
            }
         }

         if (mx >= this.cardX0 && mx <= this.cardX1 && my >= this.cardY0 && my <= this.cardY1) {
            return true;
         } else {
            this.closeCard();
            return true;
         }
      } else {
         for (CommandRulesScreen.Hit hitx : this.hits) {
            if (hitx.has(mx, my)) {
               if (hitx.blank() != null) {
                  this.focusBlank = hitx.blank();
                  this.focusSlot = hitx.slot();
               }

               if (hitx.blank() != null && hitx.blank().number()) {
                  this.beginScrub(hitx.slot(), hitx.blank(), mx);
               }

               this.act(hitx);
               return true;
            }
         }

         this.pendingClear = -1;
         return false;
      }
   }

   private boolean stepDownAt(int mx, int my) {
      if (this.pickerBlank == null && this.namingSlot < 0) {
         mx = this.unscale(mx, this.uiPivotX);
         my = this.unscale(my, this.uiPivotY);

         for (CommandRulesScreen.Hit hit : this.hits) {
            if (hit.blank() != null && hit.blank().number() && hit.has(mx, my)) {
               this.commitNumber();
               this.focusSlot = hit.slot();
               this.focusBlank = hit.blank();
               this.step(hit.slot(), hit.blank(), -1);
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private void act(CommandRulesScreen.Hit hit) {
      switch (hit.act()) {
         case PICK:
            this.openCard(hit.slot(), hit.blank());
            break;
         case PICK_WORD:
            this.pick(hit.slot(), hit.blank(), hit.id());
            break;
         case PICK_REFUSED:
            this.note = hit.id();
            this.noteTicks = 60;
            break;
         case NUMBER_UP:
            this.step(hit.slot(), hit.blank(), 1);
            break;
         case DECREE:
            this.decree(hit.slot());
            break;
         case REVERT:
            this.revert(hit.slot());
            break;
         case CLEAR:
            this.pendingClear = hit.slot();
            break;
         case CLEAR_YES:
            this.clear(hit.slot());
            break;
         case CLEAR_NO:
            this.pendingClear = -1;
            break;
         case SWITCH:
            this.toggle(hit.slot());
            break;
         case PRESET:
            this.applyStarter(hit.slot(), hit.id());
            break;
         case STARTERS:
            startersOpen = !startersOpen;
            break;
         case NAME:
            this.startNaming(hit.slot());
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (scrollY != 0.0) {
         int mx = this.unscale((int)mouseX, this.uiPivotX);
         int my = this.unscale((int)mouseY, this.uiPivotY);
         int dir = (int)Math.signum(scrollY);
         if (this.pickerBlank != null) {
            if (mx >= this.cardX0 && mx <= this.cardX1 && my >= this.cardY0 && my <= this.cardY1) {
               this.pickerScroll = Math.max(0, this.pickerScroll - dir);
            }

            return true;
         }

         for (CommandRulesScreen.Hit hit : this.hits) {
            if (hit.blank() != null && hit.has(mx, my)) {
               this.commitNumber();
               this.focusSlot = hit.slot();
               this.focusBlank = hit.blank();
               if (hit.blank().number()) {
                  this.step(hit.slot(), hit.blank(), dir * (this.shiftDown() ? 5 : 1));
               } else {
                  this.cycleWord(hit.slot(), hit.blank(), dir);
               }

               return true;
            }
         }
      }

      return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
   }

   private boolean shiftDown() {
      long window = this.minecraft.getWindow().getWindow();
      return InputConstants.isKeyDown(window, 340) || InputConstants.isKeyDown(window, 344);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0 && this.handleClick((int)mouseX, (int)mouseY)) {
         return true;
      } else {
         return button == 1 && this.stepDownAt((int)mouseX, (int)mouseY) ? true : super.mouseClicked(mouseX, mouseY, button);
      }
   }

   public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
      return this.scrub((int)mouseX) ? true : super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      this.endScrub();
      return super.mouseReleased(mouseX, mouseY, button);
   }

   private void beginScrub(int slot, @Nullable CommandRulesScreen.Blank blank, int mx) {
      this.dragSlot = slot;
      this.dragBlank = blank;
      this.dragStartX = mx;
      Rule rule = this.draft[slot];
      this.dragStartValue = blank != CommandRulesScreen.Blank.SECONDS
         ? numberOf(rule, blank)
         : (rule.infinite() ? CommandCost.durationCap(this.mastery()) : rule.seconds());
   }

   private boolean scrub(int mouseX) {
      if (this.dragBlank != null && this.dragSlot >= 0 && this.pickerBlank == null) {
         int moved = (this.unscale(mouseX, this.uiPivotX) - this.dragStartX) / 6;
         if (moved == 0) {
            return true;
         } else {
            Rule rule = this.draft[this.dragSlot];
            int wanted = this.dragStartValue + moved;
            if (this.dragBlank != CommandRulesScreen.Blank.SECONDS) {
               this.draft[this.dragSlot] = this.withNumber(rule, this.dragBlank, wanted);
            } else {
               int cap = CommandCost.durationCap(this.mastery());
               int seconds = wanted > cap && this.foreverAllowed(rule) ? -1 : Mth.clamp(wanted, 1, cap);
               this.draft[this.dragSlot] = rule.withSeconds(seconds);
            }

            return true;
         }
      } else {
         return false;
      }
   }

   private void endScrub() {
      this.dragBlank = null;
      this.dragSlot = -1;
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      return this.handleKey(keyCode) ? true : super.keyPressed(keyCode, scanCode, modifiers);
   }

   public boolean charTyped(char codepoint, int modifiers) {
      return this.handleChar(codepoint) ? true : super.charTyped(codepoint, modifiers);
   }

   private boolean handleKey(int key) {
      int mastery = this.mastery();
      if (this.namingSlot >= 0) {
         return this.nameKey(key);
      } else if (this.pickerBlank != null) {
         return this.cardKey(key);
      } else if (this.closesOn(key)) {
         this.onClose();
         return true;
      } else if (key == 258) {
         this.commitNumber();
         this.moveFocus(this.shiftDown() ? -1 : 1, mastery);
         return true;
      } else if (key != 257 && key != 335) {
         if (key == 259) {
            if (!this.numberBuffer.isEmpty()) {
               this.numberBuffer = this.numberBuffer.substring(0, this.numberBuffer.length() - 1);
            }

            return true;
         } else if (key == 263 || key == 264) {
            return this.nudge(-1, mastery);
         } else if (key != 262 && key != 265) {
            int line = lineKey(key);
            if (line >= 0 && line < 5 && !this.onNumber() && mastery >= CommandPower.slotUnlockLevel(line)) {
               this.focusSlot = line;
               this.focusBlank = CommandRulesScreen.Blank.TARGET;
               return true;
            } else {
               return false;
            }
         } else {
            return this.nudge(1, mastery);
         }
      } else {
         if (!this.numberBuffer.isEmpty()) {
            this.commitNumber();
         } else if (this.draft[this.focusSlot].resolves()
            && fits(this.draft[this.focusSlot], mastery)
            && !this.draft[this.focusSlot].equals(CommandData.of(this.minecraft.player).slot(this.focusSlot).rule())) {
            this.decree(this.focusSlot);
         }

         return true;
      }
   }

   private boolean cardKey(int key) {
      List<? extends Word> shown = this.filtered(book(this.pickerBlank));
      int cols = Math.max(1, this.cardColumns(book(this.pickerBlank), this.draft[this.pickerSlot], this.mastery()));
      switch (key) {
         case 256:
            this.closeCard();
            return true;
         case 257:
         case 335:
            if (this.pickerIndex >= 0 && this.pickerIndex < shown.size()) {
               Word word = shown.get(this.pickerIndex);
               String reason = whyNot(word, this.draft[this.pickerSlot], this.mastery());
               if (reason == null) {
                  this.pick(this.pickerSlot, this.pickerBlank, word.id());
               } else {
                  this.note = reason;
                  this.noteTicks = 60;
               }
            }

            return true;
         case 258:
            this.closeCard();
            return true;
         case 259:
            if (!this.filter.isEmpty()) {
               this.filter = this.filter.substring(0, this.filter.length() - 1);
               this.pickerIndex = 0;
               this.pickerScroll = 0;
            }

            return true;
         case 262:
            return this.moveCard(1, shown.size());
         case 263:
            return this.moveCard(-1, shown.size());
         case 264:
            return this.moveCard(cols, shown.size());
         case 265:
            return this.moveCard(-cols, shown.size());
         default:
            return false;
      }
   }

   private boolean moveCard(int delta, int size) {
      if (size <= 0) {
         return true;
      } else {
         this.pickerIndex = Mth.clamp(this.pickerIndex + delta, 0, size - 1);
         int cols = Math.max(1, this.cardColumns(book(this.pickerBlank), this.draft[this.pickerSlot], this.mastery()));
         int row = this.pickerIndex / cols;
         if (row < this.pickerScroll) {
            this.pickerScroll = row;
         } else if (row >= this.pickerScroll + this.cardVisibleRows) {
            this.pickerScroll = row - this.cardVisibleRows + 1;
         }

         return true;
      }
   }

   private boolean onNumber() {
      return this.focusBlank != null
         && this.focusBlank.number()
         && (this.focusBlank != CommandRulesScreen.Blank.SECONDS || !this.draft[this.focusSlot].instant());
   }

   private boolean closesOn(int key) {
      if (!this.numberBuffer.isEmpty()) {
         return false;
      } else {
         Key bound = AwakenedKeybinds.SECONDARY_ABILITY.getKey();
         return bound.getType() == Type.KEYSYM && bound.getValue() != InputConstants.UNKNOWN.getValue() && bound.getValue() == key;
      }
   }

   private static int lineKey(int key) {
      if (key >= 49 && key <= 57) {
         return key - 49;
      } else {
         return key >= 321 && key <= 329 ? key - 321 : -1;
      }
   }

   private boolean nudge(int dir, int mastery) {
      if (this.focusBlank != null && this.focusSlot >= 0 && this.focusSlot < 5 && mastery >= CommandPower.slotUnlockLevel(this.focusSlot)) {
         this.commitNumber();
         if (this.onNumber()) {
            this.step(this.focusSlot, this.focusBlank, dir * (this.shiftDown() ? 5 : 1));
         } else if (!this.focusBlank.number()) {
            this.cycleWord(this.focusSlot, this.focusBlank, dir);
         }

         return true;
      } else {
         return false;
      }
   }

   private boolean handleChar(char c) {
      if (this.namingSlot >= 0) {
         if (c >= ' ' && c != 167 && this.nameBuffer.length() < 16) {
            this.nameBuffer = this.nameBuffer + c;
         }

         return true;
      } else if (this.pickerBlank != null) {
         if (c >= ' ') {
            this.filter = this.filter + c;
            this.pickerIndex = 0;
            this.pickerScroll = 0;
         }

         return true;
      } else if (this.onNumber() && c >= '0' && c <= '9' && this.numberBuffer.length() < 3) {
         this.numberBuffer = this.numberBuffer + c;
         return true;
      } else {
         return false;
      }
   }

   private void commitNumber() {
      if (!this.numberBuffer.isEmpty()) {
         int typed;
         try {
            typed = Integer.parseInt(this.numberBuffer);
         } catch (NumberFormatException var3) {
            this.numberBuffer = "";
            return;
         }

         this.numberBuffer = "";
         Rule rule = this.draft[this.focusSlot];
         if (this.focusBlank == CommandRulesScreen.Blank.SECONDS) {
            this.draft[this.focusSlot] = rule.withSeconds(Mth.clamp(typed, 1, CommandCost.durationCap(this.mastery())));
         } else if (this.focusBlank != null && this.focusBlank.number()) {
            this.draft[this.focusSlot] = this.withNumber(rule, this.focusBlank, typed);
         }
      }
   }

   private void moveFocus(int dir, int mastery) {
      List<CommandRulesScreen.Blank> line = this.blanks(this.focusSlot, mastery);
      int at = this.focusBlank == null ? -1 : line.indexOf(this.focusBlank);
      int next = at + dir;
      if (next >= 0 && next < line.size()) {
         this.focusBlank = line.get(next);
      } else {
         for (int step = 1; step <= 5; step++) {
            int slot = Math.floorMod(this.focusSlot + dir * step, 5);
            if (mastery >= CommandPower.slotUnlockLevel(slot)) {
               List<CommandRulesScreen.Blank> other = this.blanks(slot, mastery);
               if (!other.isEmpty()) {
                  this.focusSlot = slot;
                  this.focusBlank = dir > 0 ? other.get(0) : other.get(other.size() - 1);
                  return;
               }
            }
         }
      }
   }

   private List<CommandRulesScreen.Blank> blanks(int slot, int mastery) {
      List<CommandRulesScreen.Blank> out = new ArrayList<>(5);

      for (CommandRulesScreen.Piece piece : this.sentencePieces(slot, this.draft[slot], mastery)) {
         if (piece.blank() != null && !out.contains(piece.blank())) {
            out.add(piece.blank());
         }
      }

      return out;
   }

   private void cycleWord(int slot, CommandRulesScreen.Blank blank, int dir) {
      List<? extends Word> words = book(blank);
      if (!words.isEmpty()) {
         String current = chosenId(this.draft[slot], blank);
         int at = -1;

         for (int i = 0; i < words.size(); i++) {
            if (words.get(i).id().equals(current)) {
               at = i;
               break;
            }
         }

         for (int step = 1; step <= words.size(); step++) {
            Word word = words.get(Math.floorMod(at + dir * step, words.size()));
            if (whyNot(word, this.draft[slot], this.mastery()) == null) {
               this.pick(slot, blank, word.id());
               this.focusBlank = blank;
               return;
            }
         }
      }
   }

   public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      g.fill(0, 0, this.width, this.height, 805306368);
      int band = Math.max(1, this.height / 4);
      g.fillGradient(0, 0, this.width, band, 1879048192, 0);
      g.fillGradient(0, this.height - band, this.width, this.height, 0, 1879048192);
   }

   private int unscale(int coord, int pivot) {
      return Math.round((float)pivot + (float)(coord - pivot) / this.uiScale);
   }

   public void onClose() {
      this.commitName();

      while (AwakenedKeybinds.SECONDARY_ABILITY.consumeClick()) {
      }

      super.onClose();
   }

   private void decree(int slot) {
      Rule rule = this.draft[slot];
      this.pendingClear = -1;
      this.flashSlot = slot;
      this.flashTicks = 10;
      send(new CommandRulePayload(slot, rule));
   }

   private void revert(int slot) {
      this.draft[slot] = this.known[slot] == null ? Rule.EMPTY : this.known[slot];
      this.pendingClear = -1;
      if (this.pickerSlot == slot) {
         this.closeCard();
      }
   }

   private void clear(int slot) {
      this.draft[slot] = Rule.EMPTY;
      this.known[slot] = Rule.EMPTY;
      this.sentNames[slot] = "";
      this.pendingClear = -1;
      if (this.pickerSlot == slot) {
         this.closeCard();
      }

      send(CommandRulePayload.clear(slot));
   }

   private void toggle(int slot) {
      Power power = PowerRegistry.get("command");
      if (power != null) {
         for (int i = 0; i < power.abilityCount(); i++) {
            if (power.ability(i) instanceof RuleToggleAbility toggle && toggle.slotIndex() == slot) {
               send(new ActivateAbilityPayload(i));
               return;
            }
         }
      }
   }

   private static void send(CustomPacketPayload payload) {
      PacketDistributor.sendToServer(payload, new CustomPacketPayload[0]);
   }

   private static enum Act {
      PICK,
      PICK_WORD,
      PICK_REFUSED,
      NUMBER_UP,
      DECREE,
      REVERT,
      CLEAR,
      CLEAR_YES,
      CLEAR_NO,
      SWITCH,
      PRESET,
      STARTERS,
      NAME;
   }

   private static enum Blank {
      TARGET,
      EFFECT,
      TRIGGER,
      LEVEL,
      RADIUS,
      SECONDS;

      boolean number() {
         return this == LEVEL || this == RADIUS || this == SECONDS;
      }
   }

   private static enum ChipState {
      EMPTY,
      FILLED,
      LOCKED,
      BAD;
   }

   private static enum Col {
      SUBJECT,
      VERB,
      LEVEL,
      WITHIN,
      RADIUS,
      FOR,
      SECONDS,
      TRIGGER;
   }

   private static record Grid(float scale, int[] x, int[] w, boolean shared) {
      int x(CommandRulesScreen.Col col) {
         return this.x[col.ordinal()];
      }

      int w(CommandRulesScreen.Col col) {
         return this.w[col.ordinal()];
      }
   }

   private static record Hit(int x0, int y0, int x1, int y1, CommandRulesScreen.Act act, int slot, @Nullable CommandRulesScreen.Blank blank, String id) {
      boolean has(int mx, int my) {
         return mx >= this.x0 && mx <= this.x1 && my >= this.y0 && my <= this.y1;
      }
   }

   private static record Piece(
      CommandRulesScreen.PieceKind kind,
      CommandRulesScreen.Col col,
      String text,
      String hint,
      @Nullable CommandRulesScreen.Blank blank,
      CommandRulesScreen.Act act,
      CommandRulesScreen.ChipState state
   ) {
   }

   private static enum PieceKind {
      CHIP,
      NUMBER,
      TEXT;
   }

   private static record Reveal(String text, int x, boolean bad) {
   }
}
