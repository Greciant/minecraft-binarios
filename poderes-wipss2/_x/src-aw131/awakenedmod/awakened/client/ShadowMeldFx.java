package com.awakenedmod.awakened.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw.Layer;

public final class ShadowMeldFx {
   private ShadowMeldFx() {
   }

   public static final class Overlay implements Layer {
      public void render(GuiGraphics g, DeltaTracker deltaTracker) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null && MeldHidden.isHidden(mc.player.getId())) {
            int w = g.guiWidth();
            int h = g.guiHeight();
            g.fill(0, 0, w, h, 856819742);
            int dark = -1073414128;
            int clear = 327696;
            int band = Math.max(48, h / 3);
            g.fillGradient(0, 0, w, band, dark, clear);
            g.fillGradient(0, h - band, w, h, clear, dark);
         }
      }
   }
}
