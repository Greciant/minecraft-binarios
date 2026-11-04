package com.awakenedmod.awakened.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw.Layer;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;

public final class ReaperHauntFx {
   private static final int SOUL_BLUE = 6277344;
   private static final int VOID_BLACK = 526864;
   private static final float VIGNETTE_PEAK = 0.72F;
   private static final float TINT_PEAK = 0.1F;
   private static final float RISE = 0.1F;
   private static final float FALL = 0.06F;
   private static final int WISPS = 5;
   private static final int WISP_PERIOD = 70;
   private static int remainingTicks;
   private static float intensity;
   private static int animTick;
   private static final float[] WISP_SEED = new float[5];

   private ReaperHauntFx() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(ReaperHauntFx::onClientTick);
   }

   public static void setRemaining(int ticks) {
      remainingTicks = Math.max(0, ticks);
   }

   public static void clear() {
      remainingTicks = 0;
   }

   private static void onClientTick(Post event) {
      if (!Minecraft.getInstance().isPaused()) {
         animTick++;
         if (remainingTicks > 0) {
            remainingTicks--;
         }

         float target = remainingTicks > 0 ? 1.0F : 0.0F;
         if (intensity < target) {
            intensity = Math.min(target, intensity + 0.1F);
         } else if (intensity > target) {
            intensity = Math.max(target, intensity - 0.06F);
         }
      }
   }

   private static int alpha(float a) {
      return Mth.clamp(Math.round(a * 255.0F), 0, 255);
   }

   static {
      for (int i = 0; i < 5; i++) {
         WISP_SEED[i] = (float)i * 14.0F;
      }
   }

   public static final class Overlay implements Layer {
      public void render(GuiGraphics g, DeltaTracker deltaTracker) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null && !mc.options.hideGui && !(ReaperHauntFx.intensity <= 0.0F)) {
            int w = g.guiWidth();
            int h = g.guiHeight();
            float pulse = 0.82F + 0.18F * (float)Math.sin((double)ReaperHauntFx.animTick * 0.18);
            float i = ReaperHauntFx.intensity * pulse;
            int tintA = ReaperHauntFx.alpha(i * 0.1F);
            if (tintA > 0) {
               g.fill(0, 0, w, h, tintA << 24 | 6277344);
            }

            int band = Math.max(40, h / 3);
            int sideBand = Math.max(40, w / 4);
            int darkEdge = ReaperHauntFx.alpha(i * 0.72F) << 24 | 526864;
            int blueLip = ReaperHauntFx.alpha(i * 0.72F * 0.45F) << 24 | 6277344;
            int clearVoid = 526864;
            g.fillGradient(0, 0, w, band, darkEdge, clearVoid);
            g.fillGradient(0, h - band, w, h, clearVoid, darkEdge);
            g.fillGradient(0, 0, w, band / 2, blueLip, clearVoid);
            g.fillGradient(0, h - band / 2, w, h, clearVoid, blueLip);
            g.fillGradient(0, 0, sideBand, h, darkEdge, clearVoid);
            g.fillGradient(w - sideBand, 0, w, h, clearVoid, darkEdge);
            this.drawWisps(g, w, h, i);
         }
      }

      private void drawWisps(GuiGraphics g, int w, int h, float i) {
         int cx = w / 2;
         int cy = h / 2;

         for (int n = 0; n < 5; n++) {
            float phase = ((float)ReaperHauntFx.animTick + ReaperHauntFx.WISP_SEED[n]) % 70.0F / 70.0F;
            long cycle = ((long)ReaperHauntFx.animTick + (long)ReaperHauntFx.WISP_SEED[n]) / 70L;
            double ang = (double)cycle * 2.39996 + (double)n * 1.3;
            float reach = 1.0F - phase * phase;
            double maxR = (double)Math.min(w, h) * 0.42;
            double r = maxR * (double)reach;
            double drift = Math.sin((double)((float)ReaperHauntFx.animTick + ReaperHauntFx.WISP_SEED[n]) * 0.07) * 12.0;
            int x = cx + (int)(Math.cos(ang) * r + Math.cos(ang + 1.57) * drift);
            int y = cy + (int)(Math.sin(ang) * r + Math.sin(ang + 1.57) * drift);
            float vis = Mth.clamp(phase < 0.15F ? phase / 0.15F : (phase > 0.85F ? (1.0F - phase) / 0.15F : 1.0F), 0.0F, 1.0F);
            float a = i * vis * 0.5F;
            if (!(a <= 0.01F)) {
               int size = (int)(10.0F + (1.0F - reach) * 26.0F);
               this.blob(g, x, y, size, a);
            }
         }
      }

      private void blob(GuiGraphics g, int x, int y, int size, float a) {
         for (int ring = 3; ring >= 1; ring--) {
            int s = size * ring / 3;
            int ra = ReaperHauntFx.alpha(a * 0.18F * (float)(4 - ring));
            if (ra > 0) {
               g.fill(x - s, y - s, x + s, y + s, ra << 24 | 6277344);
            }
         }
      }
   }
}
