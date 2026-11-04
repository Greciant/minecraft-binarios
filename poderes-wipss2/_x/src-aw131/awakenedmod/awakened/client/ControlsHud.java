package com.awakenedmod.awakened.client;

import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw.Layer;
import net.minecraft.network.chat.Component;

public abstract class ControlsHud implements Layer, FreeHud {
   private static final int PAD = 5;
   private static final int LINE_H = 11;
   private static final int TITLE_GAP = 3;
   private static final int COL_GAP = 8;
   private static final int BG = -1341124588;
   private static final int BORDER = -2136962460;
   private static final int TITLE = -1521270;
   private static final int KEY = -1;
   private static final int ACTION = -4671304;

   protected abstract Component title();

   protected abstract List<ControlsHud.Row> rows();

   protected abstract boolean showing();

   @Override
   public int hudWidth() {
      Font font = Minecraft.getInstance().font;
      int keyW = 0;
      int actionW = 0;

      for (ControlsHud.Row row : this.rows()) {
         keyW = Math.max(keyW, font.width(row.key()));
         actionW = Math.max(actionW, font.width(row.action()));
      }

      return 10 + Math.max(font.width(this.title()), keyW + 8 + actionW);
   }

   @Override
   public int hudHeight() {
      return 24 + this.rows().size() * 11 - 2;
   }

   @Override
   public HudPlacement.Box defaultBox(int screenW, int screenH, int w, int h) {
      return new HudPlacement.Box(screenW - 2 - w, screenH - 2 - h, w, h);
   }

   public void render(GuiGraphics g, DeltaTracker deltaTracker) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && !mc.options.hideGui && this.hudVisible() && this.showing()) {
         this.draw(g, this.layout(g.guiWidth(), g.guiHeight()));
      }
   }

   @Override
   public void drawPreview(GuiGraphics g, HudPlacement.Box box) {
      this.draw(g, box);
   }

   private void draw(GuiGraphics g, HudPlacement.Box box) {
      Font font = Minecraft.getInstance().font;
      List<ControlsHud.Row> rows = this.rows();
      g.fill(box.x(), box.y(), box.right(), box.bottom(), -1341124588);
      g.renderOutline(box.x(), box.y(), box.width(), box.height(), -2136962460);
      int x = box.x() + 5;
      int y = box.y() + 5;
      g.drawString(font, this.title(), x, y, -1521270, true);
      y += 14;
      int keyW = 0;

      for (ControlsHud.Row row : rows) {
         keyW = Math.max(keyW, font.width(row.key()));
      }

      for (ControlsHud.Row row : rows) {
         g.drawString(font, row.key(), x + keyW - font.width(row.key()), y, -1, true);
         g.drawString(font, row.action(), x + keyW + 8, y, -4671304, true);
         y += 11;
      }
   }

   public static record Row(Component key, Component action) {
      public static ControlsHud.Row of(KeyMapping key, String actionKey) {
         return new ControlsHud.Row(key.getTranslatedKeyMessage(), Component.translatable(actionKey));
      }
   }
}
