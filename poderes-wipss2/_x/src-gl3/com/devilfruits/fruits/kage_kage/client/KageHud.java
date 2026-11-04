package com.devilfruits.fruits.kage_kage.client;

import com.devilfruits.DevilFruits;
import com.devilfruits.core.form.FormFlags;
import com.devilfruits.fruits.kage_kage.KageKage;
import com.devilfruits.fruits.kage_kage.shadow.Shadows;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class KageHud {
   private static final ResourceLocation SHADOWS = DevilFruits.id("hud/kage_kage/shadows");
   private static final ResourceLocation AEGIS = DevilFruits.id("hud/kage_kage/aegis");

   static void render(GuiGraphics g, DeltaTracker delta) {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      if (player != null && !mc.options.hideGui && mc.level != null && KageKage.isUser(player)) {
         int cx = g.guiWidth() / 2;
         int cy = g.guiHeight() / 2;
         RenderSystem.enableBlend();
         int n = Shadows.count(player);
         if (n > 0) {
            g.blitSprite(SHADOWS, cx + 12, cy + 6, 11, 11);
            g.drawString(mc.font, Integer.toString(n), cx + 25, cy + 8, 15189234, true);
         }

         if (FormFlags.has(player, KageKage.ID, KageKage.AEGIS)) {
            float t = (float)mc.level.getGameTime() + delta.getGameTimeDeltaPartialTick(false);
            g.setColor(1.0F, 1.0F, 1.0F, 0.7F + 0.3F * Mth.sin(t * 0.4F));
            g.blitSprite(AEGIS, cx - 34, cy - 8, 16, 16);
            g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
         }
      }
   }

   private KageHud() {
   }
}
