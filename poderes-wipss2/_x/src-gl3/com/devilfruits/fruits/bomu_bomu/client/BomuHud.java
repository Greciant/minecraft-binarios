package com.devilfruits.fruits.bomu_bomu.client;

import com.devilfruits.DevilFruits;
import com.devilfruits.core.form.FormFlags;
import com.devilfruits.fruits.bomu_bomu.BomuAbilities;
import com.devilfruits.fruits.bomu_bomu.BomuBomu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

final class BomuHud {
   private static final ResourceLocation ARMED = DevilFruits.id("hud/bomu_bomu/armed_fist");
   private static final ResourceLocation ABSORBED = DevilFruits.id("hud/bomu_bomu/absorbed");
   private static final ResourceLocation RING = DevilFruits.id("hud/bomu_bomu/sd_ring_background");
   private static final ResourceLocation[] RING_TIERS = new ResourceLocation[]{
      DevilFruits.id("hud/bomu_bomu/sd_ring_1"), DevilFruits.id("hud/bomu_bomu/sd_ring_2"), DevilFruits.id("hud/bomu_bomu/sd_ring_3")
   };
   private static long absorbedAt = -1000L;
   private static long armedSince = -1L;

   static void absorbed() {
      Minecraft mc = Minecraft.getInstance();
      absorbedAt = mc.level != null ? mc.level.getGameTime() : 0L;
   }

   static void render(GuiGraphics g, DeltaTracker delta) {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      if (player != null && !mc.options.hideGui && mc.level != null && BomuBomu.isUser(player)) {
         long now = mc.level.getGameTime();
         float partial = delta.getGameTimeDeltaPartialTick(false);
         int cx = g.guiWidth() / 2;
         int cy = g.guiHeight() / 2;
         RenderSystem.enableBlend();
         if (FormFlags.has(player, BomuBomu.ID, BomuBomu.ARMED)) {
            if (armedSince < 0L) {
               armedSince = now;
            }

            double total = BomuAbilities.PUNCH.config().extra("armSeconds") * 20.0;
            float left = (float)Math.max(0.0, total - (double)(now - armedSince));
            float pulse = 0.75F + 0.25F * Mth.sin(((float)now + partial) * 0.63F);
            g.setColor(1.0F, 1.0F, 1.0F, pulse);
            g.blitSprite(ARMED, cx - 34, cy - 8, 16, 16);
            g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            String s = String.format("%.0f", Math.ceil((double)(left / 20.0F)));
            g.drawString(mc.font, s, cx - 34 + 8 - mc.font.width(s) / 2, cy + 9, 16763024, true);
         } else {
            armedSince = -1L;
         }

         float progress = BomuClientEvents.selfDestructProgress(partial);
         if (progress >= 0.0F) {
            g.blitSprite(RING, cx - 16, cy - 16, 32, 32);
            double t1 = BomuAbilities.SELF_DESTRUCT.config().extra("tier1");
            double t2 = BomuAbilities.SELF_DESTRUCT.config().extra("tier2");
            float[] lit = new float[]{
               (float)Mth.clamp((double)progress / Math.max(0.001, t1), 0.0, 1.0),
               (float)Mth.clamp(((double)progress - t1) / Math.max(0.001, t2 - t1), 0.0, 1.0),
               (float)Mth.clamp(((double)progress - t2) / Math.max(0.001, 1.0 - t2), 0.0, 1.0)
            };

            for (int i = 0; i < 3; i++) {
               if (lit[i] > 0.0F) {
                  float a = lit[i] >= 1.0F ? 1.0F : 0.25F + 0.5F * lit[i];
                  g.setColor(1.0F, 1.0F, 1.0F, a);
                  g.blitSprite(RING_TIERS[i], cx - 16, cy - 16, 32, 32);
               }
            }

            g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
         }

         float since = (float)(now - absorbedAt) + partial;
         if (since < 24.0F) {
            float a = since < 4.0F ? since / 4.0F : 1.0F - (since - 4.0F) / 20.0F;
            g.setColor(1.0F, 1.0F, 1.0F, Mth.clamp(a, 0.0F, 1.0F) * 0.9F);
            g.blitSprite(ABSORBED, cx + 12, cy - 18 - (int)(since * 0.25F), 11, 11);
            g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
         }

         RenderSystem.disableBlend();
      } else {
         armedSince = -1L;
      }
   }

   private BomuHud() {
   }
}
