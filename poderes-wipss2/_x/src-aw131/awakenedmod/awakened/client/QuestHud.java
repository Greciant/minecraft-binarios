package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.client.screen.GuiPanels;
import com.awakenedmod.awakened.core.power.Power;
import com.awakenedmod.awakened.core.power.PowerRegistry;
import com.awakenedmod.awakened.core.quest.Quest;
import com.awakenedmod.awakened.core.quest.QuestRegistry;
import com.awakenedmod.awakened.core.quest.TrialConfig;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw.Layer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

public final class QuestHud implements Layer, FreeHud {
   private static final int PAD_X = 4;
   private static final int PAD_Y = 4;
   private static final int LINE_GAP = 2;
   private static final int LINE_H = 9;
   private static final int CHIP_W = 15;
   private static final int CHIP_H = 11;
   private static final int BAR_H = 4;
   private static final int MIN_CONTENT_W = 104;
   private static final int MAX_CONTENT_W = 148;
   private static final int PREVIEW_CONTENT_W = 124;
   private static final int BACKDROP = 1711802892;
   private static final int SHADOW = 805306368;
   private static final int TEXT = -1;
   private static final int DIM = -5723984;
   private static final int GOOD = -8585348;
   private static final long FLASH_MS = 320L;
   private static String lastId = "";
   private static int lastProgress;
   private static long flashStart = Long.MIN_VALUE;
   public static final QuestHud TRACKER = new QuestHud();

   private QuestHud() {
   }

   @Override
   public String hudId() {
      return "quest_tracker";
   }

   @Override
   public boolean hudRelevant() {
      return TrialConfig.enabled();
   }

   @Override
   public HudSettings hudSettings() {
      return ClientConfig.HUD_QUEST_TRACKER;
   }

   @Override
   public HudPlacement.Box defaultBox(int screenW, int screenH, int w, int h) {
      return new HudPlacement.Box(2, 2, w, h);
   }

   @Override
   public int hudWidth() {
      return boxWidth(Minecraft.getInstance().font, pinned());
   }

   @Override
   public int hudHeight() {
      return 28;
   }

   @Nullable
   public static Quest pinned() {
      String id = ClientPowerState.get().pinnedQuest();
      return id != null && !id.isEmpty() ? QuestRegistry.byId(id) : null;
   }

   public void render(GuiGraphics g, DeltaTracker deltaTracker) {
      Minecraft mc = Minecraft.getInstance();
      ClientPowerState state = ClientPowerState.get();
      if (mc.player != null && !mc.options.hideGui && state.awakened() && this.hudVisible() && this.hudRelevant()) {
         Quest quest = pinned();
         if (quest != null) {
            drawInto(g, this.layout(g.guiWidth(), g.guiHeight()), trackProgress(quest.id(), progressOf(quest, state)));
         }
      }
   }

   @Override
   public void drawPreview(GuiGraphics g, HudPlacement.Box box) {
      if (!drawInto(g, box, 0.0F)) {
         int accent = themeColor(null);
         backdrop(g, box, accent, 0.0F);
         Font font = Minecraft.getInstance().font;
         List<FormattedCharSequence> lines = font.split(Component.translatable("awakened.hud.quest_tracker_empty"), box.width() - 8);

         for (int i = 0; i < lines.size() && i < 2; i++) {
            g.drawString(font, lines.get(i), box.x() + 4, box.y() + 4 + i * 11, -5723984);
         }
      }
   }

   private static boolean drawInto(GuiGraphics g, HudPlacement.Box box, float flash) {
      Quest quest = pinned();
      if (quest == null) {
         return false;
      } else {
         ClientPowerState state = ClientPowerState.get();
         draw(g, Minecraft.getInstance().font, quest, box, progressOf(quest, state), state.quest(quest.id()).complete(), themeColor(quest), flash);
         return true;
      }
   }

   private static int themeColor(@Nullable Quest quest) {
      Power power = PowerRegistry.get(ClientPowerState.get().powerId());
      if (power != null) {
         return power.themeColor();
      } else {
         return quest != null ? quest.tier().color() : -13056257;
      }
   }

   private static int progressOf(Quest quest, ClientPowerState state) {
      ClientPowerState.QuestView view = state.quest(quest.id());
      return view.complete() ? quest.required() : Math.min(quest.required(), view.progress());
   }

   private static float trackProgress(String id, int progress) {
      if (!id.equals(lastId)) {
         lastId = id;
         lastProgress = progress;
         flashStart = Long.MIN_VALUE;
      } else if (progress > lastProgress) {
         lastProgress = progress;
         flashStart = Util.getMillis();
      } else if (progress < lastProgress) {
         lastProgress = progress;
      }

      long elapsed = Util.getMillis() - flashStart;
      if (flashStart != Long.MIN_VALUE && elapsed >= 0L && elapsed < 320L) {
         float t = 1.0F - (float)elapsed / 320.0F;
         return t * t;
      } else {
         return 0.0F;
      }
   }

   private static void backdrop(GuiGraphics g, HudPlacement.Box box, int accent, float flash) {
      int lit = GuiPanels.lighten(accent, 0.45F * flash);
      g.fill(box.x() - 1, box.y() - 1, box.right() + 1, box.bottom() + 1, 805306368);
      g.fill(box.x(), box.y(), box.right(), box.bottom(), GuiPanels.withAlpha(1711802892, 102 + Math.round(64.0F * flash)));
      g.fill(box.x(), box.y(), box.x() + 1, box.bottom(), GuiPanels.withAlpha(lit, 136 + Math.round(96.0F * flash)));
   }

   private static void draw(GuiGraphics g, Font font, Quest quest, HudPlacement.Box box, int progress, boolean done, int theme, float flash) {
      int x = box.x();
      int y = box.y();
      int contentX = x + 4;
      int contentW = box.width() - 8;
      int accent = done ? -8585348 : theme;
      int lit = GuiPanels.lighten(accent, 0.45F * flash);
      backdrop(g, box, accent, flash);
      int line1Y = y + 4;
      GuiPanels.chip(g, font, contentX, line1Y - 2, 15, 11, quest.tier().glyph(), quest.tier().color(), true);
      int titleX = contentX + 15 + 4;
      int titleW = Math.max(10, contentX + contentW - titleX);
      g.drawString(font, GuiPanels.clip(font, quest.title().getString(), titleW), titleX, line1Y, done ? -8585348 : -1);
      int line2Y = line1Y + 9 + 2;
      String count = done ? Component.translatable("awakened.screen.quest_done").getString() : GuiPanels.questCount(progress, quest.required());
      int countW = font.width(count);
      int barX = contentX + 1;
      int barW = Math.max(8, contentX + contentW - countW - 5 - barX);
      float frac = done ? 1.0F : (quest.required() <= 0 ? 0.0F : (float)progress / (float)quest.required());
      GuiPanels.glossyBar(g, barX, line2Y + 2, barW, 4, frac, lit);
      GuiPanels.rightString(g, font, count, contentX + contentW, line2Y, done ? -8585348 : (flash > 0.0F ? GuiPanels.lighten(theme, 0.5F * flash) : -5723984));
   }

   private static int boxWidth(Font font, @Nullable Quest quest) {
      if (quest == null) {
         return 132;
      } else {
         int needed = 19 + font.width(quest.title().getString());
         return Math.max(104, Math.min(148, needed)) + 8;
      }
   }
}
