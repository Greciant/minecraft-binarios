package com.awakenedmod.awakened.client.chrono;

import com.awakenedmod.awakened.core.Lab;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw.Layer;
import net.minecraft.util.Mth;

public final class ChronoRewindScreen {
   static final Lab.Value NEGATIVE = Lab.CLIENT.def("chrono.rewind.screen_negative", 1.0, 0.0, 10.0, 1.0);
   static final Lab.Value DEVELOP = Lab.CLIENT.def("chrono.rewind.screen_develop", 8.0, 1.0, 40.0, 1.0);
   static final Lab.Value WASH = Lab.CLIENT.def("chrono.rewind.screen_wash", 0.5, 0.0, 1.0, 0.01);
   private static final int SEPIA = 12096090;
   private static final int EDGE = 3811860;
   private static int start = -1;

   private ChronoRewindScreen() {
   }

   static void rewound(int delay) {
      start = ChronoFxClient.now() + Math.max(0, delay);
   }

   static void clear() {
      start = -1;
   }

   private static float age(float partialTick) {
      return start < 0 ? -1.0F : (float)(ChronoFxClient.now() - start) + partialTick;
   }

   private static float negativeTicks() {
      return ChronoView.noNegative() ? 0.0F : NEGATIVE.getF();
   }

   static boolean negativeFrame(float partialTick) {
      float age = age(partialTick);
      return age >= 0.0F && age < negativeTicks();
   }

   private static float develop(float partialTick) {
      float age = age(partialTick);
      float from = negativeTicks();
      if (age < from) {
         return 0.0F;
      } else {
         float t = (age - from) / Math.max(1.0F, DEVELOP.getF());
         return t >= 1.0F ? 0.0F : (1.0F - t) * (1.0F - t);
      }
   }

   public static final class Overlay implements Layer {
      public void render(GuiGraphics g, DeltaTracker deltaTracker) {
         Minecraft mc = Minecraft.getInstance();
         if (ChronoRewindScreen.start >= 0 && mc.player != null && !mc.options.hideGui) {
            float amount = ChronoRewindScreen.develop(ChronoFxClient.partialTick());
            if (!(amount <= 0.0F)) {
               int w = g.guiWidth();
               int h = g.guiHeight();
               g.fill(0, 0, w, h, argb(amount * ChronoRewindScreen.WASH.getF(), 12096090));
               int edge = argb(amount * 0.6F, 3811860);
               int clear = argb(0.0F, 3811860);
               int band = Math.max(24, h / 4);
               g.fillGradient(0, 0, w, band, edge, clear);
               g.fillGradient(0, h - band, w, h, clear, edge);
            }
         }
      }

      private static int argb(float a, int rgb) {
         return Mth.clamp(Math.round(a * 255.0F), 0, 255) << 24 | rgb;
      }
   }
}
