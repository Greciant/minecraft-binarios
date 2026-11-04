package com.awakenedmod.awakened.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw.Layer;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.client.event.ViewportEvent.ComputeCameraAngles;

public final class AwakenScreenFx {
   private static final float FLASH_PEAK = 0.6F;
   private static final float FLASH_WINDOW = 0.45F;
   private static final float VIGNETTE_PEAK = 0.55F;
   private static final float TINT_PEAK = 0.14F;
   private static final float SHAKE_DEGREES = 2.4F;
   private static int totalTicks;
   private static int ticksLeft;
   private static int color;
   private static boolean shakeOnly;

   private AwakenScreenFx() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(AwakenScreenFx::onClientTick);
      gameBus.addListener(AwakenScreenFx::onCameraAngles);
   }

   public static void trigger(int rgb, int duration) {
      trigger(rgb, duration, false);
   }

   public static void trigger(int rgb, int duration, boolean onlyShake) {
      color = rgb & 16777215;
      totalTicks = Math.max(1, duration);
      ticksLeft = totalTicks;
      shakeOnly = onlyShake;
   }

   private static void onClientTick(Post event) {
      if (ticksLeft > 0 && !Minecraft.getInstance().isPaused()) {
         ticksLeft--;
      }
   }

   private static float fraction(float partialTick) {
      return ticksLeft <= 0 ? 0.0F : Mth.clamp(((float)ticksLeft - partialTick) / (float)totalTicks, 0.0F, 1.0F);
   }

   private static void onCameraAngles(ComputeCameraAngles event) {
      float frac = fraction((float)event.getPartialTick());
      if (!(frac <= 0.0F)) {
         float mag = frac * frac * 2.4F;
         float time = (float)totalTicks - frac * (float)totalTicks;
         event.setRoll(event.getRoll() + Mth.sin(time * 3.1F) * mag);
         event.setPitch(event.getPitch() + Mth.sin(time * 4.3F + 1.0F) * mag * 0.6F);
         event.setYaw(event.getYaw() + Mth.sin(time * 2.7F + 2.0F) * mag * 0.5F);
      }
   }

   public static final class Overlay implements Layer {
      public void render(GuiGraphics g, DeltaTracker deltaTracker) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null && AwakenScreenFx.ticksLeft > 0 && !AwakenScreenFx.shakeOnly) {
            float frac = AwakenScreenFx.fraction(deltaTracker.getGameTimeDeltaPartialTick(false));
            if (!(frac <= 0.0F)) {
               int w = g.guiWidth();
               int h = g.guiHeight();
               int tintA = alpha(frac * 0.14F);
               if (tintA > 0) {
                  g.fill(0, 0, w, h, tintA << 24 | AwakenScreenFx.color);
               }

               float vig = frac * 0.55F;
               int edge = alpha(vig) << 24 | AwakenScreenFx.color;
               int clear = AwakenScreenFx.color;
               int band = Math.max(24, h / 4);
               g.fillGradient(0, 0, w, band, edge, clear);
               g.fillGradient(0, h - band, w, h, clear, edge);
               float flashFrac = (frac - 0.55F) / 0.45F;
               if (flashFrac > 0.0F) {
                  int flashA = alpha(flashFrac * 0.6F);
                  if (flashA > 0) {
                     g.fill(0, 0, w, h, flashA << 24 | 16777215);
                  }
               }
            }
         }
      }

      private static int alpha(float a) {
         return Mth.clamp(Math.round(a * 255.0F), 0, 255);
      }
   }
}
