package com.awakenedmod.awakened.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw.Layer;
import net.minecraft.world.entity.Mob;

public final class PossessTetherFx {
   private static final double FRAY_MARGIN = 32.0;
   private static final int ALARM_RGB = 12588058;

   private PossessTetherFx() {
   }

   public static final class Overlay implements Layer {
      public void render(GuiGraphics g, DeltaTracker deltaTracker) {
         Minecraft mc = Minecraft.getInstance();
         Mob mob = PossessCamera.piloted();
         if (mc.player != null && mc.level != null && mob != null && !mc.options.hideGui) {
            double edge = Math.max(16.0, (double)mc.options.getEffectiveRenderDistance() * 16.0 - 8.0);
            double warnStart = edge - 32.0;
            double dx = mob.getX() - mc.player.getX();
            double dz = mob.getZ() - mc.player.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (!(dist <= warnStart)) {
               float frac = (float)Math.max(0.0, Math.min(1.0, (dist - warnStart) / 32.0));
               double t = (double)mc.level.getGameTime();
               float pulse = 0.7F + 0.3F * (float)Math.sin(t * (0.35 + 0.6 * (double)frac));
               int alpha = Math.max(0, Math.min(255, (int)(frac * pulse * 180.0F)));
               int band = alpha << 24 | 12588058;
               int clear = 12588058;
               int w = g.guiWidth();
               int h = g.guiHeight();
               int bandH = Math.max(56, h / 3);
               g.fillGradient(0, 0, w, bandH, band, clear);
               g.fillGradient(0, h - bandH, w, h, clear, band);
               int washA = Math.max(0, Math.min(255, (int)(frac * frac * 46.0F)));
               g.fill(0, 0, w, h, washA << 24 | 12588058);
            }
         }
      }
   }
}
