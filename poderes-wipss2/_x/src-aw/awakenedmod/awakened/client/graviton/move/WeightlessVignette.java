package com.awakenedmod.awakened.client.graviton.move;

import com.awakenedmod.awakened.content.graviton.WeightlessField;
import com.awakenedmod.awakened.core.Lab;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;

@EventBusSubscriber(
   modid = "awakened",
   value = {Dist.CLIENT}
)
public final class WeightlessVignette {
   private static final float DEPTH = 0.12F;
   private static final int STRIPS = 12;
   private static final float ALPHA = 0.3F;
   private static final int LINE_REPEAT = 40;
   private static final Lab.Value SHOW_WITH_HIDDEN_GUI = Lab.CLIENT.def("graviton.move.vignette_hidden_gui", 0.0, 0.0, 1.0, 1.0);
   private static int state = 0;
   private static int ticks;
   private static int lineCooldown;
   private static float shown;
   private static float shownPrev;

   private WeightlessVignette() {
   }

   @SubscribeEvent
   static void onClientTick(Post event) {
      LocalPlayer player = Minecraft.getInstance().player;
      state = player == null ? 0 : WeightlessField.zeroG(player);
      shownPrev = shown;
      boolean on = state != 0;
      shown = Mth.clamp(shown + (on ? 0.25F : -0.2F), 0.0F, 1.0F);
      if (!on) {
         ticks = 0;
         lineCooldown = 0;
      } else {
         ticks++;
         if (--lineCooldown <= 0) {
            player.displayClientMessage(Component.translatable("awakened.graviton.weightless_caught"), true);
            lineCooldown = 40;
         }
      }
   }

   @SubscribeEvent
   static void onRenderGui(net.neoforged.neoforge.client.event.RenderGuiEvent.Post event) {
      Minecraft mc = Minecraft.getInstance();
      if (!mc.options.hideGui || !(SHOW_WITH_HIDDEN_GUI.get() < 0.5)) {
         float pt = event.getPartialTick().getGameTimeDeltaPartialTick(false);
         float presence = Mth.lerp(pt, shownPrev, shown);
         if (!(presence <= 0.01F)) {
            float pulse = 1.0F;
            if (state == 2) {
               pulse = 1.6F + 0.9F * Mth.sin(((float)ticks + pt) * 1.2F);
            }

            draw(event.getGuiGraphics(), presence * pulse);
         }
      }
   }

   private static void draw(GuiGraphics g, float strength) {
      int w = g.guiWidth();
      int h = g.guiHeight();
      int depth = Math.max(4, Math.round((float)Math.min(w, h) * 0.12F));

      for (int i = 0; i < 12; i++) {
         int from = depth * i / 12;
         int to = depth * (i + 1) / 12;
         if (to > from) {
            float fall = 1.0F - (float)i / 12.0F;
            int color = argb(Mth.clamp(0.3F * strength * fall * fall, 0.0F, 0.6F), 16769732);
            g.fill(0, from, w, to, color);
            g.fill(0, h - to, w, h - from, color);
            g.fill(from, to, to, h - to, color);
            g.fill(w - to, to, w - from, h - to, color);
         }
      }
   }

   private static int argb(float alpha, int rgb) {
      return Mth.clamp(Math.round(alpha * 255.0F), 0, 255) << 24 | rgb & 16777215;
   }
}
