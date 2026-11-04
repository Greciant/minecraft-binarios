package com.awakenedmod.awakened.client.chrono;

import com.awakenedmod.awakened.client.chrono.ending.FilmBurnEnding;
import com.awakenedmod.awakened.client.chrono.ending.StopMoment;
import com.awakenedmod.awakened.client.render.PostEffectSlot;
import com.awakenedmod.awakened.core.Lab;
import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw.Layer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class ChronoScreen {
   public static final int WASH = 13214826;
   static final Lab.Value WASH_ALPHA = Lab.CLIENT.def("chrono.screen.wash", 0.3, 0.0, 1.0, 0.01);
   public static final Lab.Value VIGNETTE = Lab.CLIENT.def("chrono.screen.vignette", 0.75, 0.0, 1.0, 0.01);
   private static final ResourceLocation VIGNETTE_TEXTURE = ResourceLocation.withDefaultNamespace("textures/misc/vignette.png");
   private static final PostEffectSlot PHOTOGRAPH = new PostEffectSlot("chrono_photograph", "Chronokinetic photograph");
   private static float photo;
   private static float prevPhoto;

   private ChronoScreen() {
   }

   static void tick(Minecraft mc) {
      prevPhoto = photo;
      ChronoFxClient.Effect field = ChronoView.governing();
      ChronoFxClient.Effect ending = ChronoView.ending();
      if (field != null) {
         photo = ChronoTimeStop.photo(field.age(0.0F), ChronoView.noNegative());
      } else {
         photo = ending != null ? 1.0F : 0.0F;
      }

      if (ending != null && !FilmBurnEnding.photograph(StopMoment.of(ending, 0.0F))) {
         photo = 0.0F;
      }

      if (ChronoView.frozen()) {
         photo = 1.0F;
      }

      if (photo <= 0.0F) {
         prevPhoto = 0.0F;
      }

      PHOTOGRAPH.want(photo >= 1.0F);
   }

   static void clear() {
      photo = 0.0F;
      prevPhoto = 0.0F;
      PHOTOGRAPH.clear();
   }

   public static void wash(GuiGraphics g, float alpha) {
      int a = Mth.clamp(Math.round(alpha * 255.0F), 0, 255);
      if (a > 0) {
         g.fill(0, 0, g.guiWidth(), g.guiHeight(), a << 24 | 13214826);
      }
   }

   public static void vignette(GuiGraphics g, float strength) {
      int w = g.guiWidth();
      int h = g.guiHeight();
      float s = Mth.clamp(strength, 0.0F, 1.0F);
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);
      RenderSystem.enableBlend();
      RenderSystem.blendFuncSeparate(SourceFactor.ZERO, DestFactor.ONE_MINUS_SRC_COLOR, SourceFactor.ONE, DestFactor.ZERO);
      g.setColor(s * 0.8F, s * 0.92F, s, 1.0F);
      g.blit(VIGNETTE_TEXTURE, 0, 0, -90, 0.0F, 0.0F, w, h, w, h);
      RenderSystem.depthMask(true);
      RenderSystem.enableDepthTest();
      g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableBlend();
   }

   public static final class Overlay implements Layer {
      public void render(GuiGraphics g, DeltaTracker deltaTracker) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null && !mc.options.hideGui) {
            float partialTick = ChronoFxClient.partialTick();
            float amount = ChronoScreen.photo <= 0.0F ? 0.0F : Mth.lerp(partialTick, ChronoScreen.prevPhoto, ChronoScreen.photo);
            if (amount > 0.0F) {
               if (!ChronoScreen.PHOTOGRAPH.active()) {
                  ChronoScreen.wash(g, amount * ChronoScreen.WASH_ALPHA.getF());
               }

               ChronoScreen.vignette(g, amount * ChronoScreen.VIGNETTE.getF());
            }
         }
      }
   }
}
