package com.devilfruits.fruits.horo_horo.client;

import com.devilfruits.client.vfx.sprite.VfxSprites;
import com.devilfruits.fruits.horo_horo.HoroHoro;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

public final class SwallowedView {
   private static long since = -1L;
   private static int swell = 50;

   static void init(IEventBus modBus) {
      modBus.addListener(e -> e.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, HoroHoro.id("swallowed"), SwallowedView::render));
   }

   public static void begin(int ticks) {
      ClientLevel level = Minecraft.getInstance().level;
      since = level == null ? -1L : level.getGameTime();
      swell = Math.max(1, ticks);
   }

   static void end() {
      since = -1L;
   }

   private static void render(GuiGraphics g, DeltaTracker delta) {
      Minecraft mc = Minecraft.getInstance();
      if (since >= 0L && mc.level != null && mc.player != null && mc.options.getCameraType().isFirstPerson()) {
         float t = (float)mc.level.getGameTime() + delta.getGameTimeDeltaPartialTick(false) - (float)since;
         if (t > (float)(swell + 10)) {
            since = -1L;
         } else {
            float in = Mth.clamp(t / 6.0F, 0.0F, 1.0F);
            float k = Mth.clamp(t / (float)swell, 0.0F, 1.0F);
            int w = g.guiWidth();
            int h = g.guiHeight();
            g.fill(0, 0, w, h, Math.round(in * (110.0F + 70.0F * k)) << 24 | 16770801);
            float size = (float)h * (0.55F + 0.35F * k);
            float shake = (1.0F + 5.0F * k * k) * Mth.sin(t * (1.3F + k));
            int x = Math.round((float)w / 2.0F - size / 2.0F + shake);
            int y = Math.round((float)h / 2.0F - size / 2.0F - shake * 0.6F);
            g.setColor(1.0F, 1.0F, 1.0F, in * (0.45F + 0.25F * k));
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            int frame = k > 0.7F ? 1 : 0;
            g.blit(VfxSprites.ATLAS, x, y, Math.round(size), Math.round(size), 1920.0F + (float)frame * 64.0F, 2560.0F, 64, 64, 2048, 4096);
            g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
         }
      }
   }

   private SwallowedView() {
   }
}
