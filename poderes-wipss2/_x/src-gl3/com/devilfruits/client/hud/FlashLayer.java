package com.devilfruits.client.hud;

import com.devilfruits.client.render.CameraEffects;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;

public final class FlashLayer {
   public static void render(GuiGraphics gg, DeltaTracker delta) {
      float alpha = CameraEffects.flashAlpha(delta.getGameTimeDeltaPartialTick(false));
      if (!(alpha <= 0.01F)) {
         int w = gg.guiWidth();
         int h = gg.guiHeight();
         RenderSystem.enableBlend();
         int rgb = CameraEffects.flashColor();
         gg.setColor((float)(rgb >> 16 & 0xFF) / 255.0F, (float)(rgb >> 8 & 0xFF) / 255.0F, (float)(rgb & 0xFF) / 255.0F, Math.min(1.0F, alpha));
         gg.blit(DFHud.FLASH_TEXTURE, 0, 0, 0.0F, 0.0F, w, h, w, h);
         gg.setColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.disableBlend();
      }
   }

   private FlashLayer() {
   }
}
