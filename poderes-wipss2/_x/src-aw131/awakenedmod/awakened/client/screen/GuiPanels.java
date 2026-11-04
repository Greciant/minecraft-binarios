package com.awakenedmod.awakened.client.screen;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

public final class GuiPanels {
   public static final String ELLIPSIS = "…";
   private static List<FormattedCharSequence> pendingTooltip;

   private GuiPanels() {
   }

   public static int withAlpha(int rgb, int alpha) {
      return alpha << 24 | rgb & 16777215;
   }

   public static float clamp01(float v) {
      return Math.max(0.0F, Math.min(1.0F, v));
   }

   public static int blend(int argb, int target, float f) {
      int r = argb >> 16 & 0xFF;
      int g = argb >> 8 & 0xFF;
      int b = argb & 0xFF;
      int tr = target >> 16 & 0xFF;
      int tg = target >> 8 & 0xFF;
      int tb = target & 0xFF;
      r = Math.round((float)r + (float)(tr - r) * f);
      g = Math.round((float)g + (float)(tg - g) * f);
      b = Math.round((float)b + (float)(tb - b) * f);
      return 0xFF000000 | r << 16 | g << 8 | b;
   }

   public static int lighten(int argb, float f) {
      return blend(argb, 16777215, f);
   }

   public static int darken(int argb, float f) {
      return blend(argb, 0, f);
   }

   public static String questCount(int progress, int required) {
      return shortCount(progress) + " / " + shortCount(required);
   }

   public static String shortCount(int n) {
      if (n < 10000) {
         return String.valueOf(n);
      } else {
         int hundreds = n / 100;
         return hundreds % 10 == 0 ? hundreds / 10 + "k" : hundreds / 10 + "." + hundreds % 10 + "k";
      }
   }

   public static List<FormattedCharSequence> clipLines(Font font, Component text, int width, int maxLines) {
      List<FormattedCharSequence> lines = font.split(text, width);
      if (lines.size() <= maxLines) {
         return lines;
      } else {
         lines = font.split(text, Math.max(1, width - font.width("…")));
         List<FormattedCharSequence> kept = new ArrayList<>(lines.subList(0, Math.min(maxLines, lines.size())));
         int last = kept.size() - 1;
         if (last >= 0) {
            kept.set(last, FormattedCharSequence.composite(kept.get(last), FormattedCharSequence.forward("…", Style.EMPTY)));
         }

         return kept;
      }
   }

   public static String clip(Font font, String text, int width) {
      return font.width(text) <= width ? text : font.plainSubstrByWidth(text, Math.max(1, width - font.width("…"))) + "…";
   }

   public static void queueTooltip(Font font, Component text, int wrapWidth) {
      pendingTooltip = font.split(text, Math.max(40, wrapWidth));
   }

   public static void flushTooltip(GuiGraphics g, Font font, int mouseX, int mouseY) {
      List<FormattedCharSequence> lines = pendingTooltip;
      pendingTooltip = null;
      if (lines != null && !lines.isEmpty()) {
         g.renderTooltip(font, lines, mouseX, mouseY);
      }
   }

   public static void hatch(GuiGraphics g, int x0, int y0, int x1, int y1, int color) {
      int dash = 2;
      int period = dash * 2;

      for (int y = y0; y < y1; y++) {
         int start = x0 + ((y - y0) % 2 == 0 ? 0 : dash);

         for (int px = start; px < x1; px += period) {
            g.fill(px, y, Math.min(px + dash, x1), y + 1, color);
         }
      }
   }

   public static void outline(GuiGraphics g, int x, int y, int w, int h, int color) {
      g.fill(x, y, x + w, y + 1, color);
      g.fill(x, y + h - 1, x + w, y + h, color);
      g.fill(x, y, x + 1, y + h, color);
      g.fill(x + w - 1, y, x + w, y + h, color);
   }

   public static int centredWithin(int centreX, int width, int left, int right) {
      return Math.max(left, Math.min(centreX - width / 2, right - width));
   }

   public static int roomForCentred(int inset, int width) {
      return Math.max(inset + width / 2, width);
   }

   public static void rightString(GuiGraphics g, Font font, String text, int rightX, int y, int color) {
      g.drawString(font, text, rightX - font.width(text), y, color);
   }

   public static void chip(GuiGraphics g, Font font, int x, int y, int w, int h, String label, int color, boolean strong) {
      g.fill(x, y, x + w, y + h, withAlpha(color, strong ? 51 : 24));
      outline(g, x, y, w, h, withAlpha(color, strong ? 187 : 102));
      g.drawCenteredString(font, label, x + w / 2, y + 2, color);
   }

   public static void glossyBar(GuiGraphics g, int x, int y, int w, int h, float frac, int fill) {
      g.fill(x - 1, y - 1, x + w + 1, y + h + 1, -16777216);
      g.fillGradient(x, y, x + w, y + h, -14277074, -15395558);
      int fw = Math.round((float)w * clamp01(frac));
      if (fw > 0) {
         g.fillGradient(x, y, x + fw, y + h, lighten(fill, 0.28F), darken(fill, 0.18F));
         g.fill(x, y, x + fw, y + 1, withAlpha(lighten(fill, 0.55F), 204));
         if (fw >= 2) {
            g.fill(x + fw - 1, y, x + fw, y + h, withAlpha(lighten(fill, 0.6F), 170));
         }
      }
   }

   public static void panel(GuiGraphics g, int x, int y, int w, int h, int theme) {
      g.fill(x + 4, y + 5, x + w + 4, y + h + 5, 1711276032);
      g.fill(x - 1, y - 1, x + w + 1, y + h + 1, withAlpha(theme, 255));
      g.fillGradient(x, y, x + w, y + h, -266988004, -267711729);
      g.fillGradient(x, y, x + w, y + 3, withAlpha(theme, 240), withAlpha(theme, 68));
      int tick = withAlpha(lighten(theme, 0.4F), 255);
      g.fill(x, y, x + 6, y + 1, tick);
      g.fill(x, y, x + 1, y + 6, tick);
      g.fill(x + w - 6, y, x + w, y + 1, tick);
      g.fill(x + w - 1, y, x + w, y + 6, tick);
   }
}
