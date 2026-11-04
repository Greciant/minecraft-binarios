package com.awakenedmod.awakened.client.graviton.dense;

import com.awakenedmod.awakened.content.graviton.DenseVisual;
import com.awakenedmod.awakened.core.AwId;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(
   modid = "awakened",
   value = {Dist.CLIENT},
   bus = Bus.MOD
)
public final class DenseHud {
   private static final int BAR_W = 44;
   private static final int TEXT_Y = -30;
   private static final int PALE = -7484;
   private static final int WARM = -30147;
   private static final int DIM = -4609894;
   private static final int VIGNETTE_STEPS = 10;

   private DenseHud() {
   }

   @SubscribeEvent
   public static void onRegister(RegisterGuiLayersEvent event) {
      event.registerAbove(VanillaGuiLayers.CROSSHAIR, AwId.of("graviton_dense_hud"), new DenseHud.Layer());
   }

   private static void vignette(GuiGraphics g, float charge) {
      int w = g.guiWidth();
      int h = g.guiHeight();
      int depth = Math.round(10.0F + 34.0F * charge);
      float peak = 0.1F + 0.12F * charge;
      int rgb = 1971474;

      for (int i = 0; i < 10; i++) {
         float u = (float)i / 10.0F;
         int a = Math.round(255.0F * peak * (1.0F - u) * (1.0F - u));
         int color = Mth.clamp(a, 0, 255) << 24 | rgb;
         int inset = Math.round((float)depth * u);
         int band = Math.max(1, Math.round((float)depth / 10.0F));
         g.fill(0, inset, w, inset + band, color);
         g.fill(0, h - inset - band, w, h - inset, color);
         g.fill(inset, 0, inset + band, h, color);
         g.fill(w - inset - band, 0, w - inset, h, color);
      }
   }

   private static final class Layer implements net.minecraft.client.gui.LayeredDraw.Layer {
      public void render(GuiGraphics g, DeltaTracker deltaTracker) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null && !mc.options.hideGui) {
            DenseVisual vis = DenseVisual.of(mc.player);
            if (vis.active()) {
               float charge = vis.chargeFraction();
               if (mc.options.getCameraType().isFirstPerson()) {
                  DenseHud.vignette(g, charge);
               }

               Font font = mc.font;
               int cx = g.guiWidth() / 2;
               int y = g.guiHeight() / 2 + -30;
               boolean flash = vis.low() && mc.player.tickCount / 3 % 2 == 0;
               Component text = Component.translatableWithFallback("awakened.graviton.dense.hud.stored", "Stored %s", new Object[]{vis.stored()});
               g.drawCenteredString(font, text, cx, y, vis.stored() > 0 ? -7484 : -4609894);
               float fill = Mth.clamp((float)vis.stored() / 34.666668F, 0.0F, 1.0F);
               int bx = cx - 22;
               int by = y + 9 + 1;
               g.fill(bx - 1, by - 1, bx + 44 + 1, by + 2, -1879048192);
               int barColor = flash ? -30147 : (fill >= 1.0F ? -30147 : -7484);
               g.fill(bx, by, bx + Math.round(44.0F * fill), by + 1, barColor);
               if (vis.low()) {
                  g.drawCenteredString(
                     font, Component.translatableWithFallback("awakened.graviton.dense.hud.low", "Stamina low"), cx, by + 4, flash ? -30147 : -4609894
                  );
               }
            }
         }
      }
   }
}
