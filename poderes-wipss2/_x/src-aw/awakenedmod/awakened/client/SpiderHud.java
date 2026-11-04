package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.content.spider.WebShotAbility;
import com.awakenedmod.awakened.content.spider.WebType;
import com.awakenedmod.awakened.content.spider.WebWingsAbility;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw.Layer;
import net.minecraft.network.chat.Component;

public final class SpiderHud implements Layer, FreeHud {
   private static final int PANEL = -938470376;
   private static final int FRAME = 1426063360;
   private static final int THEME = -3001542;
   private static final int TEXT = -1;
   private static final int DIM = -4806745;
   private static final int COST_OK = -3610976;
   private static final int COST_LOW = -1949638;
   private static final int WINGS_ON = -8396662;
   private static final int WINGS_OFF = 1429221424;
   private static final int PANEL_W = 88;
   private static final int PANEL_H = 22;
   private static final int SWATCH = 12;
   private static final int TEXT_X = 20;
   private static final int BADGE_H = 11;
   private static final int BADGE_GAP = 5;
   private static final int BADGE_PAD = 14;
   private static final int HOTBAR_HALF = 91;
   private static final int HOTBAR_GAP = 4;
   private static final int BOTTOM_GAP = 3;
   public static final SpiderHud CHIP = new SpiderHud();

   private SpiderHud() {
   }

   @Override
   public String hudId() {
      return "spider_web";
   }

   @Override
   public HudSettings hudSettings() {
      return ClientConfig.HUD_SPIDER_WEB;
   }

   @Override
   public boolean hudRelevant() {
      return "spider".equals(ClientPowerState.get().powerId());
   }

   @Override
   public int hudWidth() {
      return Math.max(88, badgeWidth(Minecraft.getInstance().font));
   }

   @Override
   public int hudHeight() {
      return 22 + (wingsShown() ? 16 : 0);
   }

   @Override
   public HudPlacement.Box defaultBox(int screenW, int screenH, int w, int h) {
      int chipX = Math.max(4, screenW / 2 - 91 - 88 - 4);
      return new HudPlacement.Box(chipX + 88 - w, screenH - 3 - h, w, h);
   }

   public void render(GuiGraphics g, DeltaTracker deltaTracker) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.level != null && !mc.options.hideGui && "spider".equals(ClientPowerState.get().powerId()) && this.hudVisible()) {
         draw(g, this.layout(g.guiWidth(), g.guiHeight()));
      }
   }

   @Override
   public void drawPreview(GuiGraphics g, HudPlacement.Box box) {
      draw(g, box);
   }

   private static void draw(GuiGraphics g, HudPlacement.Box box) {
      Font font = Minecraft.getInstance().font;
      ClientPowerState power = ClientPowerState.get();
      WebType type = WebType.byKey(SpiderClientState.loadedType());
      int x = box.right() - 88;
      int y = box.bottom() - 22;
      g.fill(x - 2, y - 2, x + 88 + 2, y + 22 + 2, 1426063360);
      g.fill(x - 1, y - 1, x + 88 + 1, y + 22 + 1, -3001542);
      g.fill(x, y, x + 88, y + 22, -938470376);
      g.fill(x + 4, y + 5, x + 4 + 12, y + 5 + 12, type.themeColor());
      float shotCost = WebShotAbility.perShotCost(type);
      boolean affordable = power.infiniteStamina() || power.stamina() >= shotCost;
      String costStr = SpiderWheel.formatCost(shotCost);
      g.drawString(font, costStr, x + 88 - 4 - font.width(costStr), y + 3, affordable ? -3610976 : -1949638);
      g.drawString(font, Component.translatable("awakened.spider.hud_loaded"), x + 20, y + 3, -4806745);
      String name = font.plainSubstrByWidth(type.displayName().getString(), 66);
      g.drawString(font, name, x + 20, y + 12, -1);
      if (wingsShown()) {
         boolean on = SpiderSwingController.wingsEnabled();
         Component wings = wingsLabel(on);
         int bw = font.width(wings) + 14;
         int bx = box.right() - bw;
         int by = y - 5 - 11;
         g.fill(bx - 2, by - 2, bx + bw + 2, by + 11 + 2, 1426063360);
         g.fill(bx - 1, by - 1, bx + bw + 1, by + 11 + 1, on ? -8396662 : 1429221424);
         g.fill(bx, by, bx + bw, by + 11, -938470376);
         g.fill(bx + 4, by + 3, bx + 9, by + 8, on ? -8396662 : -4806745);
         g.drawString(font, wings, bx + 12, by + 2, on ? -1 : -4806745);
      }
   }

   private static boolean wingsShown() {
      return WebWingsAbility.isUnlocked(ClientPowerState.get().masteryLevel());
   }

   private static Component wingsLabel(boolean on) {
      return Component.translatable(on ? "awakened.spider.hud_wings_on" : "awakened.spider.hud_wings_off");
   }

   private static int badgeWidth(Font font) {
      return wingsShown() ? Math.max(font.width(wingsLabel(true)), font.width(wingsLabel(false))) + 14 : 0;
   }
}
