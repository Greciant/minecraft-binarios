package com.awakenedmod.awakened.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public final class HudText {
   private HudText() {
   }

   public static void scaled(GuiGraphics g, Font font, String text, float x, float y, float scale, int dx, int dy, int argb, boolean shadow) {
      g.pose().pushPose();
      g.pose().translate(x, y, 0.0F);
      g.pose().scale(scale, scale, 1.0F);
      g.drawString(font, text, dx, dy, argb, shadow);
      g.pose().popPose();
   }

   public static void fitCentered(GuiGraphics g, Font font, String text, int cx, int y, int maxWidth, int argb) {
      int width = font.width(text);
      if (maxWidth > 0 && width > maxWidth) {
         float scale = (float)maxWidth / (float)width;
         scaled(g, font, text, (float)cx, (float)y, scale, -width / 2, 0, argb, true);
      } else {
         g.drawCenteredString(font, text, cx, y, argb);
      }
   }

   public static void outlined(GuiGraphics g, Font font, String text, float x, float y, float scale, int dx, int dy, int argb, int outlineArgb) {
      g.pose().pushPose();
      g.pose().translate(x, y, 0.0F);
      g.pose().scale(scale, scale, 1.0F);
      g.drawString(font, text, dx - 1, dy, outlineArgb, false);
      g.drawString(font, text, dx + 1, dy, outlineArgb, false);
      g.drawString(font, text, dx, dy - 1, outlineArgb, false);
      g.drawString(font, text, dx, dy + 1, outlineArgb, false);
      g.drawString(font, text, dx, dy, argb, false);
      g.pose().popPose();
   }
}
