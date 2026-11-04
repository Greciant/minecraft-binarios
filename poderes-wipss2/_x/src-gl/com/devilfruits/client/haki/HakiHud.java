package com.devilfruits.client.haki;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.hud.DFHud;
import com.devilfruits.core.haki.HakiManager;
import com.devilfruits.core.haki.PlayerHakiData;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class HakiHud {
   static final ResourceLocation BAR_BACKGROUND = DevilFruits.id("haki/bar_background");
   static final ResourceLocation BAR_PROGRESS = DevilFruits.id("haki/bar_progress");
   static final ResourceLocation STATE_IDLE = DevilFruits.id("haki/state_idle");
   static final ResourceLocation STATE_COATED = DevilFruits.id("haki/state_coated");
   static final ResourceLocation STATE_RYUO = DevilFruits.id("haki/state_ryuo");
   static final int BAR = 62;

   public static void render(GuiGraphics gg, DeltaTracker delta) {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      if (player != null && !mc.options.hideGui && !player.isSpectator() && HakiManager.unlocked(player) && !DFHud.hidden()) {
         PlayerHakiData data = HakiManager.data(player);
         int x = 4;
         int y = gg.guiHeight() - 14;
         ResourceLocation icon = data.ryuoCharge() > 0 ? STATE_RYUO : (data.active() ? STATE_COATED : STATE_IDLE);
         gg.blitSprite(icon, x, y, 9, 9);
         int bx = x + 12;
         int by = y + 2;
         gg.blitSprite(BAR_BACKGROUND, bx, by, 62, 5);
         float fraction = Mth.clamp(Math.max(0.0F, data.spirit()) / HakiManager.maxSpirit(data.level()), 0.0F, 1.0F);
         int filled = Mth.floor(fraction * 62.0F);
         if (filled > 0) {
            gg.blitSprite(BAR_PROGRESS, 62, 5, 0, 0, bx, by, filled, 5);
         }

         String level = String.valueOf(data.level());
         int lx = bx + 62 + 3;
         gg.drawString(mc.font, level, lx + 1, y + 1, 723728, false);
         gg.drawString(mc.font, level, lx, y, 15921906, false);
      }
   }

   private HakiHud() {
   }
}
