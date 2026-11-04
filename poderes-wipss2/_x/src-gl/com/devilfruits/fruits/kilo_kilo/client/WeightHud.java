package com.devilfruits.fruits.kilo_kilo.client;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.hud.AbilityBarLayer;
import com.devilfruits.client.hud.DFHud;
import com.devilfruits.fruits.kilo_kilo.KiloKilo;
import com.devilfruits.fruits.kilo_kilo.weight.BodyWeight;
import com.devilfruits.fruits.kilo_kilo.weight.WeightCurves;
import java.util.Locale;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

final class WeightHud {
   static final int WIDTH = 92;
   private static final ResourceLocation BACKGROUND = sprite("gauge_background");
   private static final ResourceLocation FILL_LIGHT = sprite("gauge_light");
   private static final ResourceLocation FILL_HEAVY = sprite("gauge_heavy");
   private static final ResourceLocation MARKER = sprite("gauge_marker");
   private static final ResourceLocation STATE_LIGHT = sprite("state_light");
   private static final ResourceLocation STATE_NORMAL = sprite("state_normal");
   private static final ResourceLocation STATE_HEAVY = sprite("state_heavy");
   private static final ResourceLocation ARROW_UP = sprite("arrow_up");
   private static final ResourceLocation ARROW_DOWN = sprite("arrow_down");

   private static ResourceLocation sprite(String name) {
      return DevilFruits.id("hud/kilo_kilo/" + name);
   }

   static int position(double kg, int inner) {
      double f = Math.log(WeightCurves.clampKg(kg) / 1.0) / WeightCurves.FULL_SPAN;
      return Mth.clamp((int)Math.round(f * (double)(inner - 1)), 0, inner - 1);
   }

   static String label(double kg) {
      if (kg < 9.95) {
         return String.format(Locale.ROOT, "%.1f kg", kg);
      } else if (kg < 999.5) {
         return Math.round(kg) + " kg";
      } else {
         double t = kg / 1000.0;
         return t < 9.95 ? String.format(Locale.ROOT, "%.1f t", t) : Math.round(t) + " t";
      }
   }

   static void render(GuiGraphics gg, DeltaTracker delta) {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      if (player != null && !mc.options.hideGui && !player.isSpectator() && !DFHud.hidden() && KiloKilo.isUser(player)) {
         BodyWeight w = KiloClientState.weight(player);
         if (w != null) {
            float partial = delta.getGameTimeDeltaPartialTick(false);
            double kg = KiloClientState.kg(player, partial);
            int x;
            int y;
            if (AbilityBarLayer.vertical()) {
               boolean left = AbilityBarLayer.barX() < gg.guiWidth() / 2;
               x = left ? AbilityBarLayer.barX() + 34 : AbilityBarLayer.barX() - 92 - 22;
               y = AbilityBarLayer.barY() - 22;
            } else {
               x = AbilityBarLayer.barX() + 11;
               y = AbilityBarLayer.barY() - 30;
            }

            double s = WeightCurves.scale(kg);
            ResourceLocation state = s < -0.08 ? STATE_LIGHT : (s > 0.08 ? STATE_HEAVY : STATE_NORMAL);
            gg.blitSprite(state, x - 11, y - 1, 9, 9);
            gg.blitSprite(BACKGROUND, x, y, 92, 7);
            int inner = 90;
            int zero = position(70.0, inner);
            int at = position(kg, inner);
            if (at < zero) {
               gg.blitSprite(FILL_LIGHT, 92, 7, 1 + at, 0, x + 1 + at, y, zero - at, 7);
            } else if (at > zero) {
               gg.blitSprite(FILL_HEAVY, 92, 7, 1 + zero, 0, x + 1 + zero, y, at - zero + 1, 7);
            }

            gg.blitSprite(MARKER, x + 1 + at - 2, y - 1, 5, 9);
            String text = label(kg);
            int color = s < -0.08 ? 14216447 : (s > 0.08 ? 12765394 : 15130872);
            int tx = x + 92 - mc.font.width(text);
            gg.drawString(mc.font, text, tx, y - 10, color, true);
            if (w.ramping()) {
               boolean up = w.target() > w.kg();
               float blink = 0.6F + 0.4F * Mth.sin(((float)player.tickCount + partial) * 0.5F);
               gg.setColor(1.0F, 1.0F, 1.0F, blink);
               gg.blitSprite(up ? ARROW_UP : ARROW_DOWN, tx - 8, y - 10, 5, 7);
               gg.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
         }
      }
   }

   private WeightHud() {
   }
}
