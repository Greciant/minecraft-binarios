package com.awakenedmod.awakened.client.screen.card;

import com.awakenedmod.awakened.client.screen.GuiPanels;
import net.minecraft.network.chat.Component;

public final class CardChrome {
   public static final int BAND_H = 32;
   public static final int BUTTON = 20;

   private CardChrome() {
   }

   public static void face(CardGfx gfx, int x, int y, int w, int h) {
      gfx.fill(x, y, w, h, -13751512);
      gfx.fill(x + 2, y + 2, w - 4, 32, gfx.band);
      gfx.fill(x + 2, y + 2, w - 4, 2, GuiPanels.lighten(gfx.band, 0.15F));
      gfx.fill(x + 2, y + 32, w - 4, 2, GuiPanels.darken(gfx.band, 0.15F));
   }

   public static int titleCentred(CardGfx gfx, Component title, int x, int y, int w) {
      Component t = CardGfx.bold(title);
      int tx = x + w / 2 - gfx.width(t) * 3 / 2;
      gfx.scaled(t, tx, titleY(y), CardStyle.titleInk(gfx.band), 3);
      return tx;
   }

   public static int title(CardGfx gfx, Component title, int tx, int y) {
      return gfx.scaled(CardGfx.bold(title), tx, titleY(y), CardStyle.titleInk(gfx.band), 3);
   }

   private static int titleY(int y) {
      return y + 2 + 4 + 1;
   }

   public static int buttonX(int x, int w) {
      int gm = 4;
      return x + w - 4 - gm - 20;
   }

   public static int buttonY(int y) {
      return y + 4 + 4;
   }

   public static int button(CardGfx gfx, int x, int y, int w, boolean open, boolean hover) {
      int gx = buttonX(x, w);
      int gy = buttonY(y);
      gfx.fill(gx, gy, 20, 20, -15461872);
      gfx.fill(gx + 1, gy + 1, 18, 18, open ? -13751512 : (hover ? -12041411 : -12962254));
      return open ? gfx.band : -1185569;
   }

   public static void frame(CardGfx gfx, int x, int y, int w, int h) {
      gfx.frame(x, y, w, h, -15461872);
      gfx.fill(x + 2, y + h - 4, w - 4, 2, -12238791);
      gfx.fill(x + w - 4, y + 2, 2, h - 4, -12238791);
   }
}
