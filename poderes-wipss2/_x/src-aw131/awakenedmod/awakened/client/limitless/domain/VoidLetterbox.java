package com.awakenedmod.awakened.client.limitless.domain;

import com.awakenedmod.awakened.client.HudText;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw.Layer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public final class VoidLetterbox implements Layer {
   private static final float SLIDE = 8.0F;
   private static final float BAR = 0.12F;
   private static final float TITLE_FADE = 8.0F;
   private static final float TITLE_SCALE = 2.5F;

   public void render(GuiGraphics g, DeltaTracker deltaTracker) {
      Minecraft mc = Minecraft.getInstance();
      if (!mc.options.hideGui) {
         VoidIntro.State s = VoidIntro.current(deltaTracker.getGameTimeDeltaPartialTick(false));
         if (s != null) {
            float t = s.t();
            float end = (float)s.length();
            float bars = Math.min(ease(t / 8.0F), ease((end - t) / 8.0F));
            int w = g.guiWidth();
            int h = g.guiHeight();
            int bar = Math.round((float)h * 0.12F * bars);
            if (bar > 0) {
               g.fill(0, 0, w, bar, -16777216);
               g.fill(0, h - bar, w, h, -16777216);
            }

            float title = Math.min(ease((t - 12.0F) / 8.0F), ease((end - t) / 8.0F));
            int alpha = Math.round(255.0F * title);
            if (alpha >= 8) {
               Font font = mc.font;
               String text = Component.translatable("awakened.limitless.void_title").getString();
               float scale = 2.5F * (0.96F + 0.04F * title);
               HudText.scaled(
                  g, font, text, (float)w / 2.0F, (float)h / 2.0F - 9.0F * scale / 2.0F, scale, -font.width(text) / 2, 0, alpha << 24 | 15397631, true
               );
            }
         }
      }
   }

   private static float ease(float x) {
      float u = Mth.clamp(x, 0.0F, 1.0F);
      return u * u * (3.0F - 2.0F * u);
   }
}
