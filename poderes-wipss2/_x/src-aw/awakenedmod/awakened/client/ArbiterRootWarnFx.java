package com.awakenedmod.awakened.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw.Layer;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.IEventBus;

public final class ArbiterRootWarnFx {
   private static final int RGB = 11544319;
   private static final float PEAK_ALPHA = 0.35F;
   private static final float FADE_TICKS = 6.0F;
   private static final float MIN_PULSE_TICKS = 4.0F;
   private static final float BAND_V = 0.16F;
   private static final float BAND_H = 0.1F;
   private static final int SIDE_STEPS = 6;
   private static boolean inside;
   private static int ticksToBurst;
   private static double statedAt = Double.NEGATIVE_INFINITY;
   private static double fadeFrom = Double.NEGATIVE_INFINITY;
   private static float phase;
   private static double lastFrame = Double.NaN;

   private ArbiterRootWarnFx() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(event -> clear());
   }

   public static void set(boolean nowInside, int ticks) {
      double now = gameTime();
      if (nowInside) {
         inside = true;
         ticksToBurst = Math.max(0, ticks);
         statedAt = now;
         fadeFrom = Double.NEGATIVE_INFINITY;
      } else if (inside) {
         float showing = strength(now);
         inside = false;
         fadeFrom = now - (1.0 - (double)showing) * 6.0;
      }
   }

   public static void clear() {
      inside = false;
      statedAt = Double.NEGATIVE_INFINITY;
      fadeFrom = Double.NEGATIVE_INFINITY;
      phase = 0.0F;
      lastFrame = Double.NaN;
   }

   private static double gameTime() {
      Minecraft mc = Minecraft.getInstance();
      return mc.level == null ? 0.0 : (double)mc.level.getGameTime();
   }

   private static float remaining(double now) {
      return (float)((double)ticksToBurst - (now - statedAt));
   }

   private static float strength(double now) {
      if (inside) {
         float left = remaining(now);
         return left > 0.0F ? 1.0F : Math.max(0.0F, 1.0F + left / 6.0F);
      } else {
         return fadeFrom == Double.NEGATIVE_INFINITY ? 0.0F : Math.max(0.0F, 1.0F - (float)(now - fadeFrom) / 6.0F);
      }
   }

   public static final class Overlay implements Layer {
      public void render(GuiGraphics g, DeltaTracker deltaTracker) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null && mc.level != null && !mc.options.hideGui) {
            double now = (double)((float)mc.level.getGameTime() + deltaTracker.getGameTimeDeltaPartialTick(false));
            float strength = ArbiterRootWarnFx.strength(now);
            if (strength <= 0.0F) {
               ArbiterRootWarnFx.lastFrame = now;
            } else {
               float period = Math.max(4.0F, ArbiterRootWarnFx.remaining(now) * 0.5F);
               double step = Double.isNaN(ArbiterRootWarnFx.lastFrame) ? 0.0 : Math.max(0.0, now - ArbiterRootWarnFx.lastFrame);
               ArbiterRootWarnFx.lastFrame = now;
               ArbiterRootWarnFx.phase = (float)(((double)ArbiterRootWarnFx.phase + step * (Math.PI * 2) / (double)period) % (Math.PI * 2));
               float pulse = 0.6F + 0.4F * Mth.sin(ArbiterRootWarnFx.phase);
               int peak = Math.round(0.35F * strength * pulse * 255.0F);
               if (peak > 0) {
                  draw(g, Mth.clamp(peak, 0, 255));
               }
            }
         }
      }

      private static void draw(GuiGraphics g, int peakAlpha) {
         int w = g.guiWidth();
         int h = g.guiHeight();
         int edge = peakAlpha << 24 | 11544319;
         int clear = 11544319;
         int bandV = Math.max(1, Math.round((float)h * 0.16F));
         int bandH = Math.max(1, Math.round((float)w * 0.1F));
         g.fillGradient(0, 0, w, bandV, edge, clear);
         g.fillGradient(0, h - bandV, w, h, clear, edge);

         for (int i = 0; i < 6; i++) {
            int a = peakAlpha * (6 - i) / 6;
            if (a > 0) {
               int col = a << 24 | 11544319;
               int x0 = bandH * i / 6;
               int x1 = bandH * (i + 1) / 6;
               g.fill(x0, bandV, x1, h - bandV, col);
               g.fill(w - x1, bandV, w - x0, h - bandV, col);
            }
         }
      }
   }
}
