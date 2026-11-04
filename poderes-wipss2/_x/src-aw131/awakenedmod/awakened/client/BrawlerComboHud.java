package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.client.screen.GuiPanels;
import com.awakenedmod.awakened.content.brawler.BrawlerTuning;
import com.awakenedmod.awakened.core.power.Power;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw.Layer;
import net.minecraft.network.chat.Component;

public final class BrawlerComboHud implements Layer, FreeHud {
   static final float NUMBER_SCALE = 2.4F;
   static final float RANK_SCALE = 1.3F;
   private static final int PAD = 3;
   private static final int LINE_H = 9;
   private static final int BAR_W = 44;
   private static final int BAR_H = 2;
   private static final int BAR_TRACK = 1711276032;
   private static final int WHITE = 16777215;
   private static final int OFFSET_X = 40;
   private static final int OFFSET_Y = -28;
   private static final int PREVIEW_COUNT = 12;
   public static final BrawlerComboHud COUNTER = new BrawlerComboHud();

   private BrawlerComboHud() {
   }

   @Override
   public String hudId() {
      return "brawler_combo";
   }

   @Override
   public HudSettings hudSettings() {
      return ClientConfig.HUD_BRAWLER_COMBO;
   }

   @Override
   public boolean hudRelevant() {
      return "brawler".equals(ClientPowerState.get().powerId());
   }

   @Override
   public int hudWidth() {
      Font font = Minecraft.getInstance().font;
      int w = Math.round((float)font.width("999") * 2.4F * 1.6F);
      w = Math.max(w, Math.round((float)widestRank(font) * 1.3F));
      w = Math.max(w, Math.round((float)font.width(text("awakened.brawler.hud.summary", 99)) * 1.2F));
      w = Math.max(w, Math.round((float)font.width(text("awakened.brawler.hud.spent")) * 1.3F));
      w = Math.max(w, font.width(text("awakened.brawler.hud.new_best")));
      w = Math.max(w, font.width(text("awakened.brawler.hud.dmg", 70)));
      w = Math.max(w, font.width(text("awakened.brawler.hud.free")));
      return Math.max(w, 44) + 6;
   }

   @Override
   public int hudHeight() {
      return 3 + numberRegionH() + 9 + rankRegionH() + 2 + 2 + 9 + 9 + 3;
   }

   private static int numberRegionH() {
      return Math.round(21.6F);
   }

   private static int rankRegionH() {
      return Math.round(11.7F);
   }

   @Override
   public HudPlacement.Box defaultBox(int screenW, int screenH, int w, int h) {
      return new HudPlacement.Box(screenW / 2 + 40, screenH / 2 + -28 - h / 2, w, h);
   }

   public void render(GuiGraphics g, DeltaTracker deltaTracker) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && mc.player != null && !mc.options.hideGui && this.hudVisible() && this.hudRelevant()) {
         BrawlerComboAnimator anim = BrawlerComboAnimator.frame();
         if (anim.mode() != BrawlerComboAnimator.Mode.HIDDEN) {
            HudPlacement.Box box = this.layout(g.guiWidth(), g.guiHeight());
            switch (anim.mode()) {
               case LIVE:
                  drawLive(
                     g, mc.font, box, BrawlerComboState.count(), BrawlerComboState.tier(), BrawlerComboState.windowFraction(), BrawlerComboState.paused(), anim
                  );
                  break;
               case DROP_FADE:
                  drawDropFade(g, mc.font, box, BrawlerComboState.endedCombo(), anim);
                  break;
               case SUMMARY:
                  drawSummary(g, mc.font, box, BrawlerComboState.endedCombo(), anim);
                  break;
               case SPENT:
                  drawSpent(g, mc.font, box, BrawlerComboState.spentCount(), anim);
            }
         }
      }
   }

   @Override
   public void drawPreview(GuiGraphics g, HudPlacement.Box box) {
      int count = Math.max(12, BrawlerComboState.count());
      drawLive(g, Minecraft.getInstance().font, box, count, BrawlerTuning.tier(count), 0.7F, false, BrawlerComboAnimator.REST);
   }

   private static void drawLive(
      GuiGraphics g, Font font, HudPlacement.Box box, int count, int tier, float windowFraction, boolean paused, BrawlerComboAnimator anim
   ) {
      int color = tierColor(tier);
      int right = box.right() - 3;
      float bx = anim.blockX();
      float by = anim.blockY();
      int y = box.y() + 3;
      String number = Integer.toString(count);
      int fill = anim.flash() > 0.01F ? GuiPanels.lighten(color, anim.flash()) : color;
      HudText.outlined(
         g,
         font,
         number,
         (float)right + bx + anim.numberDx(),
         (float)y + (float)numberRegionH() / 2.0F + by + anim.numberDy(),
         2.4F * anim.numberScale(),
         -font.width(number),
         -4,
         fill,
         anim.outline()
      );
      y += numberRegionH();
      String hits = text("awakened.brawler.hud.hits");
      HudText.scaled(g, font, hits, (float)right + bx, (float)y + by, 1.0F, -font.width(hits), 0, GuiPanels.withAlpha(color, 178), true);
      y += 9;
      if (tier >= 2) {
         String rank = text("awakened.brawler.tier." + BrawlerTuning.TIER_KEY[tier]);
         int rankFill = anim.rankFlash() > 0.01F ? GuiPanels.lighten(color, anim.rankFlash()) : color;
         HudText.scaled(
            g, font, rank, (float)right + bx, (float)y + (float)rankRegionH() / 2.0F + by, 1.3F * anim.rankScale(), -font.width(rank), -4, rankFill, true
         );
      }

      y += rankRegionH();
      drawBar(g, font, right + Math.round(bx), y + Math.round(by), windowFraction, paused, anim.barWhite() ? -1 : color, color);
      y += 4;
      int pct = Math.round(BrawlerTuning.damageBonus(count) * 100.0F);
      if (pct > 0) {
         String dmg = text("awakened.brawler.hud.dmg", pct);
         HudText.scaled(g, font, dmg, (float)right + bx, (float)y + by, 1.0F, -font.width(dmg), 0, GuiPanels.withAlpha(16777215, 153), true);
      }

      y += 9;
      long now = Util.getMillis();
      if (anim != BrawlerComboAnimator.REST && BrawlerComboState.freeShowing(now) && Power.hudBlinkOn()) {
         String free = text("awakened.brawler.hud.free");
         HudText.scaled(g, font, free, (float)right + bx, (float)y + by, 1.0F, -font.width(free), 0, -15053, true);
      }
   }

   private static void drawBar(GuiGraphics g, Font font, int right, int y, float windowFraction, boolean paused, int fillColor, int glyphColor) {
      int barX0 = right - 44;
      g.fill(barX0, y, right, y + 2, 1711276032);
      int filled = Math.round(44.0F * Math.max(0.0F, Math.min(1.0F, windowFraction)));
      if (filled > 0) {
         g.fill(right - filled, y, right, y + 2, fillColor);
      }

      if (paused) {
         g.drawString(font, "II", barX0 - font.width("II") - 2, y - 3, GuiPanels.withAlpha(glyphColor, 230), true);
      }
   }

   private static void drawDropFade(GuiGraphics g, Font font, HudPlacement.Box box, int ended, BrawlerComboAnimator anim) {
      int a = alphaByte(anim.alpha());
      if (a > 0) {
         int right = box.right() - 3;
         int y = box.y() + 3;
         String number = Integer.toString(ended);
         HudText.outlined(
            g,
            font,
            number,
            (float)right,
            (float)y + (float)numberRegionH() / 2.0F,
            2.4F,
            -font.width(number),
            -4,
            GuiPanels.withAlpha(10132122, a),
            GuiPanels.withAlpha(1052688, a)
         );
         y += numberRegionH();
         String hits = text("awakened.brawler.hud.hits");
         g.drawString(font, hits, right - font.width(hits), y, GuiPanels.withAlpha(10132122, a * 7 / 10), true);
      }
   }

   private static void drawSummary(GuiGraphics g, Font font, HudPlacement.Box box, int ended, BrawlerComboAnimator anim) {
      int a = alphaByte(anim.alpha());
      if (a > 0) {
         int right = box.right() - 3;
         int y = box.y() + 3;
         int summaryH = Math.round(10.8F);
         int summaryTop = y + numberRegionH() - summaryH;
         if (anim.newBest()) {
            String best = text("awakened.brawler.hud.new_best");
            g.drawString(font, best, right - font.width(best), summaryTop - 9, GuiPanels.withAlpha(16762163, a), true);
         }

         String summary = text("awakened.brawler.hud.summary", ended);
         HudText.outlined(
            g,
            font,
            summary,
            (float)right,
            (float)summaryTop + (float)summaryH / 2.0F,
            1.2F,
            -font.width(summary),
            -4,
            GuiPanels.withAlpha(10132122, a),
            GuiPanels.withAlpha(1052688, a)
         );
      }
   }

   private static void drawSpent(GuiGraphics g, Font font, HudPlacement.Box box, int spent, BrawlerComboAnimator anim) {
      int right = box.right() - 3;
      int y = box.y() + 3;
      int a = alphaByte(anim.blowAlpha());
      if (a > 0 && spent > 0) {
         String number = Integer.toString(spent);
         HudText.outlined(
            g,
            font,
            number,
            (float)right,
            (float)y + (float)numberRegionH() / 2.0F,
            2.4F * anim.blowScale(),
            -font.width(number),
            -4,
            GuiPanels.withAlpha(16762163, a),
            GuiPanels.withAlpha(1052688, a)
         );
      }

      y += numberRegionH() + 9;
      if (anim.spentLabel()) {
         String label = text("awakened.brawler.hud.spent");
         HudText.scaled(g, font, label, (float)right, (float)y + (float)rankRegionH() / 2.0F, 1.3F, -font.width(label), -4, -15053, true);
      }
   }

   private static int tierColor(int tier) {
      int i = Math.max(0, Math.min(tier, BrawlerTuning.TIER_COLOR.length - 1));
      return 0xFF000000 | BrawlerTuning.TIER_COLOR[i];
   }

   private static int alphaByte(float alpha) {
      int a = Math.round(Math.max(0.0F, Math.min(1.0F, alpha)) * 255.0F);
      return a < 8 ? 0 : a;
   }

   private static String text(String key, Object... args) {
      return Component.translatable(key, args).getString();
   }

   private static int widestRank(Font font) {
      int widest = font.width(text("awakened.brawler.hud.hits"));

      for (int i = 2; i < BrawlerTuning.TIER_KEY.length; i++) {
         widest = Math.max(widest, font.width(text("awakened.brawler.tier." + BrawlerTuning.TIER_KEY[i])));
      }

      return widest;
   }
}
