package com.awakenedmod.awakened.client.screen;

import com.awakenedmod.awakened.client.ClientPowerState;
import com.awakenedmod.awakened.client.MinionView;
import com.awakenedmod.awakened.client.SoulIcons;
import com.awakenedmod.awakened.content.summoning.SoulData;
import com.awakenedmod.awakened.content.summoning.SummonAttachments;
import com.awakenedmod.awakened.content.summoning.SummonSouls;
import com.awakenedmod.awakened.core.net.SoulMenuPayload;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public final class SoulMenuScreen extends AwakenedScreen {
   private static final int PANEL_W = 410;
   private static final int PAD = 12;
   private static final int LINE = 10;
   private static final int SCREEN_MARGIN = 16;
   private static final int COLS = 3;
   private static final int CELL_W = 64;
   private static final int CELL_H = 58;
   private static final int CELL_GAP = 6;
   private static final int GRID_W = 204;
   private static final int ROW_H = 64;
   private static final int MIN_GRID_ROWS = 2;
   private static final int BTN_H = 13;
   private static final int BTN_W = 18;
   private static final int BULK = 9999;
   private static final int GRID_GAP = 14;
   private static final int SIDEBAR_W = 168;
   private static final int STAT_ROW_H = 19;
   private static final int STAT_ROW_GAP = 3;
   private static final int STATS_PAD = 4;
   private static final int STATS_BLOCK_H = 93;
   private static final int BTN_ROW_H = 15;
   private static final int SIDEBAR_GAP = 6;
   private static final int BODY_H = Math.max(133, 128);
   private static final int TITLE_SCALE_PCT = 150;
   private static final int TITLE_H = 13;
   private static final int SUBTITLE_H = 10;
   private static final int BAR_H = 5;
   private static final int BAR_LABEL_H = 10;
   private static final int HEADER_H = 67;
   private static final int LEGEND_LINES = 5;
   private static final int LEGEND_H = 77;
   private static final int BG = -435023338;
   private static final int TEXT = -1;
   private static final int DIM = -5723984;
   private static final int DIM_WEAK = -9539976;
   private static final int GOOD = -8594564;
   private static final int WARN = -2056144;
   private static final int RED = -38037;
   private static final int RED_IDLE = -7714230;
   private static final int LOCK_BORDER = -5228488;
   private static final int LOCK_BG = 1428818442;
   private static final int GOLD = -13988;
   private static final int CELL_BG = -1728053248;
   private static final int CELL_BORDER = -14671832;
   private static final int BTN_BG = -870704606;
   private static final int STATS_BG = 1711276032;
   private static final int DORMANT = -6657188;
   private final List<SoulMenuScreen.Cell> cells = new ArrayList<>();
   private int scrollRow;
   private boolean canScroll;
   private int maxScrollRow;
   private int barX0;
   private int barX1;
   private int barTrackY0;
   private int barTrackY1;
   private int barThumbY0;
   private int barThumbY1;
   private boolean draggingBar;
   private double dragOffset;
   private String pendingDiscard;
   private int pendingAmount;
   private int lockX0;
   private int lockY0;
   private int lockX1;
   private int lockY1;
   private int blacklistBtnX0;
   private int blacklistBtnY0;
   private int blacklistBtnX1;
   private int blacklistBtnY1;
   private int helpX0;
   private int helpY0;
   private int helpX1;
   private int helpY1;
   private int legendHideX0;
   private int legendHideY0;
   private int legendHideX1;
   private int legendHideY1;
   private static boolean legendExpanded = true;
   private boolean legendShown;
   private boolean legendFitsBelow;
   public static int openCount;
   public static final int NUDGE_OPENS = 5;
   private Component pendingTooltip;
   private int pendingTooltipX;
   private int pendingTooltipY;

   public SoulMenuScreen() {
      super(Component.translatable("awakened.souls.title"));
      openCount++;
   }

   public boolean isPauseScreen() {
      return false;
   }

   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      super.render(g, mouseX, mouseY, partialTick);
      if (this.minecraft != null && this.minecraft.player != null) {
         int theme = ClientPowerState.get().awakened() ? -13049400 : -13056257;
         SoulData souls = (SoulData)this.minecraft.player.getData(SummonAttachments.SOULS);
         Map<String, Integer> banked = souls.souls();
         if (this.pendingDiscard != null && !banked.containsKey(this.pendingDiscard)) {
            this.pendingDiscard = null;
         }

         int cap = SummonSouls.capForLevel(ClientPowerState.get().masteryLevel());
         int totalRows = Math.max(1, (banked.size() + 3 - 1) / 3);
         int maxVisibleRows = Math.max(1, BODY_H / 64);
         int visibleRows = Math.min(totalRows, maxVisibleRows);
         this.maxScrollRow = totalRows - visibleRows;
         this.canScroll = this.maxScrollRow > 0;
         this.scrollRow = Mth.clamp(this.scrollRow, 0, this.maxScrollRow);
         int bareH = 67 + BODY_H + 12;
         this.legendFitsBelow = bareH + 77 + 32 <= this.height;
         int panelH = bareH + (legendExpanded && this.legendFitsBelow ? 77 : 0);
         int x = (this.width - 410) / 2;
         int y = Math.max(16, (this.height - panelH) / 2);
         g.fill(x - 1, y - 1, x + 410 + 1, y + panelH + 1, theme);
         g.fill(x, y, x + 410, y + panelH, -435023338);
         g.fill(x, y, x + 410, y + 67 - 4, theme & 16777215 | 637534208);
         g.fill(x, y, x + 410, y + 1, 1073741824);
         g.fill(x, y, x + 1, y + panelH, 1073741824);
         g.fill(x, y + panelH - 1, x + 410, y + panelH, 587202559);
         g.fill(x + 410 - 1, y, x + 410, y + panelH, 587202559);
         int hy = this.drawHeader(g, souls, banked, cap, theme, x, y, mouseX, mouseY);
         int gridX = x + 12;
         int gridY = hy;
         int sidebarX = gridX + 204 + 14;
         this.cells.clear();
         if (banked.isEmpty()) {
            List<FormattedCharSequence> lines = this.font.split(Component.translatable("awakened.souls.empty"), 204);
            int cy0 = hy + BODY_H / 2 - lines.size() * 10 / 2;

            for (FormattedCharSequence line : lines) {
               g.drawString(this.font, line, gridX, cy0, -5723984);
               cy0 += 10;
            }
         } else {
            g.enableScissor(gridX - 1, hy - 1, gridX + 204 + 1, hy + BODY_H + 1);
            int i = 0;

            for (Entry<String, Integer> entry : banked.entrySet()) {
               int row = i / 3;
               if (row >= this.scrollRow && row < this.scrollRow + visibleRows) {
                  int col = i % 3;
                  int x0 = gridX + col * 70;
                  int y0 = gridY + (row - this.scrollRow) * 64;
                  this.cells.add(this.drawCell(g, souls, entry.getKey(), entry.getValue(), x0, y0, theme, mouseX, mouseY));
               }

               i++;
            }

            g.disableScissor();
            this.drawScrollbar(g, gridX, gridY, totalRows, visibleRows, theme, mouseX, mouseY);
         }

         this.drawSidebar(g, souls, sidebarX, gridY, theme, mouseX, mouseY);
         this.legendShown = legendExpanded && this.legendFitsBelow;
         if (this.legendShown) {
            this.drawLegend(g, x, y + 67 + BODY_H, y + panelH, theme, mouseX, mouseY);
         }

         this.flushTooltip(g);
      }
   }

   private int drawHeader(GuiGraphics g, SoulData souls, Map<String, Integer> banked, int cap, int theme, int x, int y, int mouseX, int mouseY) {
      int cx = x + 12;
      int hy = y + 12;
      this.helpX1 = x + 410 - 12;
      this.helpX0 = this.helpX1 - 11;
      this.helpY0 = hy - 1;
      this.helpY1 = hy + 10;
      boolean helpHover = this.panelButton(g, this.helpX0, this.helpY0, this.helpX1, this.helpY1, "?", theme, mouseX, mouseY, true);
      this.tooltip(
         g,
         Component.translatable(this.legendFitsBelow && legendExpanded ? "awakened.souls.legend_hide" : "awakened.souls.legend_show"),
         mouseX,
         mouseY,
         helpHover
      );
      this.drawScaledTitle(g, cx, hy, theme);
      hy += 15;
      g.drawString(this.font, Component.translatable("awakened.souls.subtitle"), cx, hy, -5723984);
      hy += 14;
      int barX1 = x + 410 - 12;
      int total = souls.total();
      float frac = cap > 0 ? Mth.clamp((float)total / (float)cap, 0.0F, 1.0F) : 0.0F;
      int barColor = frac >= 0.9F ? -38037 : (frac >= 0.7F ? -2056144 : -8594564);
      g.fill(cx, hy, barX1, hy + 5, 872415231);
      int fillW = Math.round((float)(barX1 - cx) * frac);
      if (fillW > 0) {
         g.fill(cx, hy, cx + fillW, hy + 5, barColor);
      }

      hy += 8;
      String capLabel = Component.translatable("awakened.souls.capacity", new Object[]{total, cap}).getString();
      g.drawString(this.font, capLabel, barX1 - this.font.width(capLabel), hy, -5723984);
      return hy + 18;
   }

   private void drawScaledTitle(GuiGraphics g, int x, int y, int color) {
      float scale = 1.5F;
      Component title = Component.translatable("awakened.souls.title");
      g.pose().pushPose();
      g.pose().translate((float)x, (float)y, 0.0F);
      g.pose().scale(scale, scale, 1.0F);
      g.drawString(this.font, title, 0, 0, color);
      g.pose().popPose();
   }

   private void drawSidebar(GuiGraphics g, SoulData souls, int sx, int sy, int theme, int mouseX, int mouseY) {
      int sw = 168;
      g.fill(sx - 4, sy - 2, sx + sw, sy + 93, 1711276032);
      int cy = sy + 4;
      int fielded = MinionView.fielded(this.minecraft);
      int minionCap = SummonSouls.maxMinionsForLevel(ClientPowerState.get().masteryLevel());
      String active = souls.active();
      boolean activeIsTrophy = !active.isEmpty() && SummonSouls.isTrophy(active);
      boolean activeIsDormant = !active.isEmpty() && SummonSouls.isDormant(active);
      boolean activeZeroOwned = !active.isEmpty() && !activeIsDormant && souls.count(active) <= 0;
      Component activeVal;
      int activeColor;
      if (active.isEmpty()) {
         activeVal = Component.translatable("awakened.souls.val_none");
         activeColor = -5723984;
      } else if (activeIsDormant) {
         activeVal = SummonSouls.displayName(active).copy().append(" ").append(Component.translatable("awakened.souls.dormant"));
         activeColor = -6657188;
      } else if (activeZeroOwned) {
         activeVal = SummonSouls.displayName(active).copy().append(Component.translatable("awakened.souls.active_zero"));
         activeColor = -2056144;
      } else if (activeIsTrophy) {
         activeVal = SummonSouls.displayName(active).copy().append(" ").append(Component.translatable("awakened.souls.trophy"));
         activeColor = -13988;
      } else {
         activeVal = SummonSouls.displayName(active);
         activeColor = theme;
      }

      cy = this.statRow(g, "awakened.souls.lbl_active", "awakened.souls.tip_active", activeVal, activeColor, sx, cy, sw, mouseX, mouseY);
      boolean fieldFull = fielded >= minionCap;
      cy = this.statRow(
         g,
         "awakened.souls.lbl_field",
         "awakened.souls.tip_field",
         Component.literal(fielded + " / " + minionCap),
         fieldFull ? -2056144 : -1,
         sx,
         cy,
         sw,
         mouseX,
         mouseY
      );
      int minionPowerPct = Math.round((MasteryScaling.massBonus(ClientPowerState.get().masteryLevel()) + ClientPowerState.get().abilityPower()) * 100.0F);
      Component powerVal = minionPowerPct > 0
         ? Component.translatable("awakened.souls.power_pct", new Object[]{minionPowerPct})
         : Component.translatable("awakened.souls.power_none");
      cy = this.statRow(
         g, "awakened.souls.lbl_power", "awakened.souls.tip_power", powerVal, minionPowerPct > 0 ? -8594564 : -5723984, sx, cy, sw, mouseX, mouseY
      );
      boolean anyLoadout = souls.loadout().keySet().stream().anyMatch(t -> souls.effectiveLoadout(t) > 0);
      String loadoutText = anyLoadout ? this.loadoutSummary(souls) : Component.translatable("awakened.souls.loadout_none").getString();
      String loadoutClamped = this.clamp(loadoutText, sw);
      cy = this.statRow(
         g, "awakened.souls.lbl_wave", "awakened.souls.tip_wave", Component.literal(loadoutClamped), anyLoadout ? -1 : -5723984, sx, cy, sw, mouseX, mouseY
      );
      int by = sy + 93 + 6;
      boolean locked = souls.bankLocked();
      this.lockX0 = sx - 2;
      this.lockY0 = by;
      this.lockX1 = sx + sw;
      this.lockY1 = by + 15;
      boolean hoverLock = mouseX >= this.lockX0 && mouseX <= this.lockX1 && mouseY >= this.lockY0 && mouseY <= this.lockY1;
      int lockBorder = locked ? -5228488 : (hoverLock ? theme : -14671832);
      int lockBg = locked ? 1428818442 : (hoverLock ? -869651912 : -870704606);
      g.fill(this.lockX0 - 1, this.lockY0 - 1, this.lockX1 + 1, this.lockY1 + 1, lockBorder);
      g.fill(this.lockX0, this.lockY0, this.lockX1, this.lockY1, lockBg);
      this.drawSwitch(g, this.lockX0 + 3, (this.lockY0 + this.lockY1) / 2, locked);
      String lockText = Component.translatable(locked ? "awakened.souls.lock_on" : "awakened.souls.lock_off").getString();
      g.drawString(this.font, lockText, this.lockX0 + 24, (this.lockY0 + this.lockY1) / 2 - 4, locked ? -38037 : (hoverLock ? -1 : -5723984));
      this.tooltip(g, Component.translatable("awakened.souls.tip_lock"), mouseX, mouseY, hoverLock);
      by = this.lockY1 + 4;
      this.blacklistBtnX0 = sx - 2;
      this.blacklistBtnY0 = by;
      this.blacklistBtnX1 = sx + sw;
      this.blacklistBtnY1 = by + 15;
      String blacklistText = Component.translatable("awakened.souls.blacklist_btn", new Object[]{souls.blacklist().size()}).getString();
      boolean hoverBlacklist = this.panelButton(
         g, this.blacklistBtnX0, this.blacklistBtnY0, this.blacklistBtnX1, this.blacklistBtnY1, blacklistText, theme, mouseX, mouseY, true
      );
      this.tooltip(g, Component.translatable("awakened.souls.tip_blacklist_btn"), mouseX, mouseY, hoverBlacklist);
   }

   private int statRow(GuiGraphics g, String labelKey, String tipKey, Component value, int valueColor, int x, int y, int w, int mouseX, int mouseY) {
      g.drawString(this.font, Component.translatable(labelKey), x, y, -5723984);
      String clamped = this.clamp(value.getString(), w);
      g.drawString(this.font, clamped, x, y + 9, valueColor);
      boolean hover = mouseX >= x - 2 && mouseX <= x + w && mouseY >= y && mouseY <= y + 19;
      this.tooltip(g, Component.translatable(tipKey), mouseX, mouseY, hover);
      return y + 19 + 3;
   }

   private void drawSwitch(GuiGraphics g, int x0, int centerY, boolean on) {
      int swW = 16;
      int swH = 6;
      int swY0 = centerY - swH / 2;
      g.fill(x0, swY0, x0 + swW, swY0 + swH, on ? -5228488 : -12961212);
      int knobW = 7;
      int knobX = on ? x0 + swW - knobW : x0;
      g.fill(knobX, swY0 - 1, knobX + knobW, swY0 + swH + 1, on ? -14393 : -3355444);
   }

   private void drawLegend(GuiGraphics g, int x, int top, int bottom, int theme, int mouseX, int mouseY) {
      int cx = x + 12;
      int textW = 386;
      g.enableScissor(x, top, x + 410, bottom);
      int cy = top + 6;
      g.fill(cx, cy, x + 410 - 12, cy + 1, 872415231);
      cy += 4;
      g.drawString(this.font, Component.translatable("awakened.souls.legend_title"), cx, cy, -1);
      String hide = Component.translatable("awakened.souls.legend_hide").getString();
      this.legendHideX1 = x + 410 - 12;
      this.legendHideX0 = this.legendHideX1 - this.font.width(hide) - 8;
      this.legendHideY0 = cy - 1;
      this.legendHideY1 = cy + 9;
      this.panelButton(g, this.legendHideX0, this.legendHideY0, this.legendHideX1, this.legendHideY1, hide, theme, mouseX, mouseY, true);
      cy += 10;
      int keyW = 96;
      int ctrlW = textW - keyW;
      cy = this.drawRow(
         g, Component.translatable("awakened.souls.k_left"), -1, keyW, Component.translatable("awakened.souls.d_active"), -5723984, cx, cy, ctrlW
      );
      cy = this.drawRow(
         g, Component.translatable("awakened.souls.k_scroll"), -1, keyW, Component.translatable("awakened.souls.d_count"), -5723984, cx, cy, ctrlW
      );
      cy = this.drawRow(g, Component.translatable("awakened.souls.k_right"), -1, keyW, Component.translatable("awakened.souls.d_all"), -5723984, cx, cy, ctrlW);
      cy = this.drawRow(
         g, Component.translatable("awakened.souls.k_discard"), -1, keyW, Component.translatable("awakened.souls.d_discard"), -5723984, cx, cy, ctrlW
      );
      this.drawRow(g, Component.translatable("awakened.souls.k_ban"), -1, keyW, Component.translatable("awakened.souls.d_ban"), -5723984, cx, cy, ctrlW);
      g.disableScissor();
   }

   private void drawScrollbar(GuiGraphics g, int gridX, int gridY, int totalRows, int visibleRows, int theme, int mouseX, int mouseY) {
      if (!this.canScroll) {
         this.barTrackY0 = this.barTrackY1 = 0;
      } else {
         this.barX0 = gridX + 204 + 4;
         this.barX1 = this.barX0 + 3;
         this.barTrackY0 = gridY;
         this.barTrackY1 = gridY + BODY_H;
         int trackH = this.barTrackY1 - this.barTrackY0;
         int thumbH = Math.max(16, trackH * visibleRows / totalRows);
         this.barThumbY0 = this.barTrackY0 + (trackH - thumbH) * this.scrollRow / this.maxScrollRow;
         this.barThumbY1 = this.barThumbY0 + thumbH;
         g.fill(this.barX0, this.barTrackY0, this.barX1, this.barTrackY1, 872415231);
         boolean hover = this.draggingBar || mouseX >= this.barX0 && mouseX <= this.barX1 && mouseY >= this.barThumbY0 && mouseY <= this.barThumbY1;
         g.fill(this.barX0, this.barThumbY0, this.barX1, this.barThumbY1, hover ? theme : -9803150);
      }
   }

   private int drawRow(GuiGraphics g, Component left, int leftColor, int leftW, Component right, int rightColor, int x, int y, int rightMaxW) {
      g.drawString(this.font, left, x, y, leftColor);
      int n = 0;

      for (FormattedCharSequence line : this.font.split(right, rightMaxW)) {
         g.drawString(this.font, line, x + leftW, y + n * 10, rightColor);
         n++;
      }

      return y + Math.max(1, n) * 10;
   }

   private SoulMenuScreen.Cell drawCell(GuiGraphics g, SoulData souls, String type, int count, int x0, int y0, int theme, int mouseX, int mouseY) {
      int btnY0 = y0 + 58 - 13;
      int trashX1 = x0 + 64;
      int trashX0 = trashX1 - 11;
      int trashY1 = y0 + 10;
      int banX1 = x0 + 11;
      int banY1 = y0 + 10;
      int promptY0 = y0 + 26;
      int promptY1 = y0 + 58 - 1;
      int confirmX0 = x0 + 1;
      int confirmX1 = x0 + 32 - 1;
      int cancelX0 = x0 + 32 + 1;
      int cancelX1 = x0 + 64 - 1;
      int minusX0 = x0 + 1;
      int minusX1 = minusX0 + 18;
      int plusX1 = x0 + 64 - 1;
      int plusX0 = plusX1 - 18;
      if (type.equals(this.pendingDiscard)) {
         g.fill(x0 - 1, y0 - 1, x0 + 64 + 1, y0 + 58 + 1, -5228488);
         g.fill(x0, y0, x0 + 64, y0 + 58, -433911286);
         String q = this.font.plainSubstrByWidth(Component.translatable("awakened.souls.discard_q").getString(), 60);
         g.drawString(this.font, q, x0 + (64 - this.font.width(q)) / 2, y0 + 8, -30070);
         String cs = "x" + (this.pendingAmount <= 0 ? count : Math.min(this.pendingAmount, count));
         g.drawString(this.font, cs, x0 + (64 - this.font.width(cs)) / 2, y0 + 17, -1);
         this.glyphButton(g, confirmX0, promptY0, confirmX1, promptY1, Component.translatable("awakened.souls.yes").getString(), -38037, mouseX, mouseY);
         this.glyphButton(g, cancelX0, promptY0, cancelX1, promptY1, Component.translatable("awakened.souls.no").getString(), -5723984, mouseX, mouseY);
         return new SoulMenuScreen.Cell(
            x0,
            y0,
            x0 + 64,
            btnY0,
            type,
            minusX0,
            minusX1,
            plusX0,
            plusX1,
            btnY0,
            y0 + 58,
            trashX0,
            trashX1,
            y0,
            trashY1,
            x0,
            banX1,
            y0,
            banY1,
            confirmX0,
            confirmX1,
            cancelX0,
            cancelX1,
            promptY0,
            promptY1
         );
      } else {
         boolean active = type.equals(souls.active());
         boolean trophy = SummonSouls.isTrophy(type);
         boolean dormant = SummonSouls.isDormant(type);
         boolean blacklisted = souls.isBlacklisted(type);
         boolean hoverSelect = mouseX >= x0 && mouseX <= x0 + 64 && mouseY >= y0 && mouseY < btnY0;
         int border = blacklisted ? -38037 : (active ? theme : (hoverSelect ? -9408392 : (dormant ? -6657188 : (trophy ? -13988 : -14671832))));
         int bg = blacklisted ? -1725298166 : (active ? -871491046 : (hoverSelect ? -1441129950 : -1728053248));
         g.fill(x0 - 1, y0 - 1, x0 + 64 + 1, y0 + 58 + 1, border);
         g.fill(x0, y0, x0 + 64, y0 + 58, bg);
         if (hoverSelect && !active) {
            g.fill(x0, y0, x0 + 64, y0 + 1, 1090519039);
            g.fill(x0, y0, x0 + 1, y0 + 58, 1090519039);
         }

         int iconX = x0 + 24;
         int iconY = y0 + 8;
         ItemStack icon = SoulIcons.of(type);
         g.renderItem(icon, iconX, iconY);
         g.renderItemDecorations(this.font, icon, iconX, iconY, String.valueOf(count));
         String shortName = this.font.plainSubstrByWidth(SummonSouls.displayName(type).getString(), 60);
         int nameColor = dormant ? -6657188 : (trophy ? -13988 : -1);
         g.drawString(this.font, shortName, x0 + (64 - this.font.width(shortName)) / 2, iconY + 19, nameColor);
         boolean hoverTrash = mouseX >= trashX0 && mouseX <= trashX1 && mouseY >= y0 && mouseY <= trashY1;
         this.drawDiscardIcon(g, trashX1 - 6, y0 + 6, hoverTrash ? -38037 : -7714230);
         this.tooltip(g, Component.translatable("awakened.souls.tip_discard", new Object[]{SummonSouls.displayName(type)}), mouseX, mouseY, hoverTrash);
         boolean hoverBan = mouseX >= x0 && mouseX <= banX1 && mouseY >= y0 && mouseY <= banY1;
         this.drawBanIcon(g, x0 + 6, y0 + 6, blacklisted ? -38037 : (hoverBan ? -38037 : -7714230));
         this.tooltip(
            g,
            Component.translatable(blacklisted ? "awakened.souls.tip_unban" : "awakened.souls.tip_ban", new Object[]{SummonSouls.displayName(type)}),
            mouseX,
            mouseY,
            hoverBan
         );
         int load = souls.effectiveLoadout(type);
         this.button(g, minusX0, btnY0, minusX1, y0 + 58, "-", load > 0, mouseX, mouseY);
         this.button(g, plusX0, btnY0, plusX1, y0 + 58, "+", true, mouseX, mouseY);
         String lab = "W" + load;
         g.drawString(this.font, lab, (minusX1 + plusX0) / 2 - this.font.width(lab) / 2, btnY0 + 3, load > 0 ? theme : -5723984);
         return new SoulMenuScreen.Cell(
            x0,
            y0,
            x0 + 64,
            btnY0,
            type,
            minusX0,
            minusX1,
            plusX0,
            plusX1,
            btnY0,
            y0 + 58,
            trashX0,
            trashX1,
            y0,
            trashY1,
            x0,
            banX1,
            y0,
            banY1,
            confirmX0,
            confirmX1,
            cancelX0,
            cancelX1,
            promptY0,
            promptY1
         );
      }
   }

   private void drawBanIcon(GuiGraphics g, int cx, int cy, int color) {
      int r = 5;

      for (int dy = -r; dy <= r; dy++) {
         for (int dx = -r; dx <= r; dx++) {
            double dist = Math.sqrt((double)(dx * dx + dy * dy));
            boolean ring = dist >= (double)r - 0.7 && dist <= (double)r + 0.1;
            boolean slash = dx + dy == 0 && dist <= (double)r - 0.7;
            if (ring || slash) {
               g.fill(cx + dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
            }
         }
      }
   }

   private void drawDiscardIcon(GuiGraphics g, int cx, int cy, int color) {
      int r = 5;

      for (int dy = -r; dy <= r; dy++) {
         for (int dx = -r; dx <= r; dx++) {
            double dist = Math.sqrt((double)(dx * dx + dy * dy));
            if (!(dist > (double)r) && (dx + dy == 0 || dx - dy == 0)) {
               g.fill(cx + dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
            }
         }
      }
   }

   private void glyphButton(GuiGraphics g, int x0, int y0, int x1, int y1, String label, int color, int mouseX, int mouseY) {
      boolean hover = mouseX >= x0 && mouseX <= x1 && mouseY >= y0 && mouseY <= y1;
      g.fill(x0, y0, x1, y1, hover ? -869651912 : -870704606);
      g.drawString(this.font, label, (x0 + x1) / 2 - this.font.width(label) / 2, (y0 + y1) / 2 - 4, hover ? -1 : color);
   }

   private void button(GuiGraphics g, int x0, int y0, int x1, int y1, String label, boolean enabled, int mouseX, int mouseY) {
      boolean hover = enabled && mouseX >= x0 && mouseX <= x1 && mouseY >= y0 && mouseY <= y1;
      g.fill(x0, y0, x1, y1, hover ? -869651912 : -870704606);
      g.drawString(this.font, label, (x0 + x1) / 2 - this.font.width(label) / 2, y0 + 3, !enabled ? -9539976 : (hover ? -1 : -5723984));
   }

   private boolean panelButton(GuiGraphics g, int x0, int y0, int x1, int y1, String label, int theme, int mouseX, int mouseY, boolean centered) {
      boolean hover = mouseX >= x0 && mouseX <= x1 && mouseY >= y0 && mouseY <= y1;
      g.fill(x0 - 1, y0 - 1, x1 + 1, y1 + 1, hover ? theme : -14671832);
      g.fill(x0, y0, x1, y1, hover ? -869651912 : -870704606);
      int tx = centered ? x0 + Math.round((float)(x1 - x0 - this.font.width(label)) / 2.0F) : x0 + 3;
      int ty = y0 + Math.round((float)(y1 - y0 - 9) / 2.0F);
      g.drawString(this.font, label, tx, ty, hover ? -1 : -5723984);
      return hover;
   }

   private void tooltip(GuiGraphics g, Component text, int mouseX, int mouseY, boolean hovering) {
      if (hovering) {
         this.pendingTooltip = text;
         this.pendingTooltipX = mouseX;
         this.pendingTooltipY = mouseY;
      }
   }

   private void flushTooltip(GuiGraphics g) {
      if (this.pendingTooltip != null) {
         g.renderTooltip(this.font, this.pendingTooltip, this.pendingTooltipX, this.pendingTooltipY);
         this.pendingTooltip = null;
      }
   }

   private String clamp(String s, int maxW) {
      return this.font.width(s) <= maxW ? s : this.font.plainSubstrByWidth(s, Math.max(0, maxW - this.font.width("…"))) + "…";
   }

   private String loadoutSummary(SoulData souls) {
      StringBuilder sb = new StringBuilder();

      for (String type : souls.loadout().keySet()) {
         int eff = souls.effectiveLoadout(type);
         if (eff > 0) {
            if (sb.length() > 0) {
               sb.append(", ");
            }

            sb.append(eff).append("× ").append(SummonSouls.displayName(type).getString());
         }
      }

      return sb.length() == 0 ? Component.translatable("awakened.souls.loadout_none").getString() : sb.toString();
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      return this.handleClick((int)mouseX, (int)mouseY, button) ? true : super.mouseClicked(mouseX, mouseY, button);
   }

   public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
      if (this.draggingBar) {
         this.scrollToThumbTop(mouseY - this.dragOffset);
         return true;
      } else {
         return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
      }
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      this.draggingBar = false;
      return super.mouseReleased(mouseX, mouseY, button);
   }

   private void scrollToThumbTop(double thumbTopY) {
      int span = this.barTrackY1 - this.barTrackY0 - (this.barThumbY1 - this.barThumbY0);
      if (span > 0) {
         double t = (thumbTopY - (double)this.barTrackY0) / (double)span;
         this.scrollRow = Mth.clamp((int)Math.round(t * (double)this.maxScrollRow), 0, this.maxScrollRow);
      }
   }

   private boolean handleClick(int mx, int my, int button) {
      if (this.pendingDiscard != null) {
         if (button == 0) {
            for (SoulMenuScreen.Cell cell : this.cells) {
               if (cell.type().equals(this.pendingDiscard)
                  && mx >= cell.confirmX0()
                  && mx <= cell.confirmX1()
                  && my >= cell.promptY0()
                  && my <= cell.promptY1()) {
                  this.send(2, cell.type(), this.pendingAmount);
                  break;
               }
            }
         }

         this.pendingDiscard = null;
         return true;
      } else if (button != 0) {
         return this.handleRightClick(mx, my, button);
      } else if (mx >= this.helpX0 && mx <= this.helpX1 && my >= this.helpY0 && my <= this.helpY1) {
         if (this.legendFitsBelow) {
            legendExpanded = !legendExpanded;
         } else {
            this.minecraft.setScreen(new SoulControlsScreen());
         }

         return true;
      } else if (this.legendShown && mx >= this.legendHideX0 && mx <= this.legendHideX1 && my >= this.legendHideY0 && my <= this.legendHideY1) {
         legendExpanded = false;
         return true;
      } else if (mx >= this.lockX0 && mx <= this.lockX1 && my >= this.lockY0 && my <= this.lockY1) {
         this.send(3, "", 0);
         return true;
      } else if (mx >= this.blacklistBtnX0 && mx <= this.blacklistBtnX1 && my >= this.blacklistBtnY0 && my <= this.blacklistBtnY1) {
         this.minecraft.setScreen(new BlacklistScreen());
         return true;
      } else if (this.canScroll && mx >= this.barX0 && mx <= this.barX1 && my >= this.barTrackY0 && my <= this.barTrackY1) {
         if (my >= this.barThumbY0 && my <= this.barThumbY1) {
            this.dragOffset = (double)(my - this.barThumbY0);
         } else {
            this.dragOffset = (double)(this.barThumbY1 - this.barThumbY0) / 2.0;
            this.scrollToThumbTop((double)my - this.dragOffset);
         }

         this.draggingBar = true;
         return true;
      } else {
         int step = this.shiftDown() ? 5 : 1;

         for (SoulMenuScreen.Cell cellx : this.cells) {
            if (mx >= cellx.trashX0() && mx <= cellx.trashX1() && my >= cellx.trashY0() && my <= cellx.trashY1()) {
               this.pendingDiscard = cellx.type();
               this.pendingAmount = this.shiftDown() ? 0 : 1;
               return true;
            }

            if (mx >= cellx.banX0() && mx <= cellx.banX1() && my >= cellx.banY0() && my <= cellx.banY1()) {
               SoulData souls = (SoulData)this.minecraft.player.getData(SummonAttachments.SOULS);
               this.send(souls.isBlacklisted(cellx.type()) ? 5 : 4, cellx.type(), 0);
               return true;
            }

            if (my >= cellx.btnY0() && my <= cellx.btnY1()) {
               if (mx >= cellx.minusX0() && mx <= cellx.minusX1()) {
                  this.send(1, cellx.type(), -step);
                  return true;
               }

               if (mx >= cellx.plusX0() && mx <= cellx.plusX1()) {
                  this.send(1, cellx.type(), step);
                  return true;
               }
            }

            if (mx >= cellx.x0() && mx <= cellx.x1() && my >= cellx.y0() && my < cellx.selectY1()) {
               this.send(0, cellx.type(), 0);
               return true;
            }
         }

         return false;
      }
   }

   private boolean handleRightClick(int mx, int my, int button) {
      if (button != 1) {
         return false;
      } else {
         for (SoulMenuScreen.Cell cell : this.cells) {
            if (mx >= cell.x0() && mx <= cell.x1() && my >= cell.y0() && my <= cell.btnY1()) {
               this.send(1, cell.type(), this.shiftDown() ? -9999 : 9999);
               return true;
            }
         }

         return false;
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (scrollY != 0.0 && this.canScroll) {
         this.scrollRow = Mth.clamp(this.scrollRow - (int)Math.signum(scrollY), 0, this.maxScrollRow);
         return true;
      } else {
         return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
      }
   }

   private void send(int action, String type, int amount) {
      PacketDistributor.sendToServer(new SoulMenuPayload(action, type, amount), new CustomPacketPayload[0]);
   }

   private boolean shiftDown() {
      long window = this.minecraft.getWindow().getWindow();
      return InputConstants.isKeyDown(window, 340) || InputConstants.isKeyDown(window, 344);
   }

   private static record Cell(
      int x0,
      int y0,
      int x1,
      int selectY1,
      String type,
      int minusX0,
      int minusX1,
      int plusX0,
      int plusX1,
      int btnY0,
      int btnY1,
      int trashX0,
      int trashX1,
      int trashY0,
      int trashY1,
      int banX0,
      int banX1,
      int banY0,
      int banY1,
      int confirmX0,
      int confirmX1,
      int cancelX0,
      int cancelX1,
      int promptY0,
      int promptY1
   ) {
   }
}
