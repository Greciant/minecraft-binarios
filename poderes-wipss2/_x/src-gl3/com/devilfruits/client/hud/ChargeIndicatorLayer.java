package com.devilfruits.client.hud;

import com.devilfruits.client.ClientFruitState;
import com.devilfruits.core.fruit.FruitTheme;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

public final class ChargeIndicatorLayer {
   private static final int WIDTH = 32;
   private static final int HEIGHT = 5;

   public static void render(GuiGraphics gg, DeltaTracker delta) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && !mc.options.hideGui && mc.level != null && !DFHud.hidden()) {
         float partial = delta.getGameTimeDeltaPartialTick(false);
         float charge = ClientFruitState.chargeProgress(partial);
         if (!(charge < 0.0F)) {
            FruitTheme theme = DFHud.theme();
            int x = gg.guiWidth() / 2 - 16;
            int y = gg.guiHeight() / 2 + 10;
            gg.blitSprite(theme.chargeBackground(), x, y, 32, 5);
            int filled = Mth.floor(charge * 32.0F);
            if (filled > 0) {
               if (charge >= 1.0F) {
                  float pulse = 0.65F + 0.35F * Mth.sin(((float)mc.level.getGameTime() + partial) * 1.2F);
                  gg.setColor(1.0F, 1.0F, 1.0F, pulse);
               }

               gg.blitSprite(theme.chargeProgress(), 32, 5, 0, 0, x, y, filled, 5);
               gg.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
         }
      }
   }

   private ChargeIndicatorLayer() {
   }
}
