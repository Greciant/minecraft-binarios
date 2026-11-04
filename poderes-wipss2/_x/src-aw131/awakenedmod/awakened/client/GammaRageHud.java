package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.client.screen.GuiPanels;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw.Layer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public final class GammaRageHud implements Layer, FreeHud {
   private static final int WIDTH = 7;
   private static final int HEIGHT = 80;
   private static final int EDGE_INSET = 16;
   private static final int BAR_RIGHT_GAP = 14;
   private static final int BAR_CENTRE_INSET = 18;
   private static final int LABEL_GAP = 10;
   private static final int TIER_GAP = 4;
   private static final int LINE_H = 9;
   private static final int COLOR_BG = -1442840576;
   private static final int COLOR_BORDER = -14671840;
   private static final int COLOR_TICK = -2130706433;
   private static final int FILL_LOW = -5841292;
   private static final int FILL_FULL = -11225052;
   private static final float MIN_RAGE = 0.005F;
   private static final float[] TIER_FROM = new float[]{0.9F, 0.7F, 0.45F, 0.2F, 0.0F};
   private static final String[] TIER_KEY = new String[]{"unstoppable", "enraged", "furious", "simmering", "calm"};
   private static float lastRage = -1.0F;
   private static float pulse;
   public static final GammaRageHud METER = new GammaRageHud();

   private GammaRageHud() {
   }

   @Override
   public String hudId() {
      return "gamma_rage";
   }

   @Override
   public HudSettings hudSettings() {
      return ClientConfig.HUD_GAMMA_RAGE;
   }

   @Override
   public int hudWidth() {
      Font font = Minecraft.getInstance().font;
      int widest = GuiPanels.roomForCentred(18, font.width(Component.translatable("awakened.gamma.rage")));

      for (String key : TIER_KEY) {
         widest = Math.max(widest, GuiPanels.roomForCentred(18, font.width(Component.translatable("awakened.gamma.tier." + key))));
      }

      return Math.max(21, widest);
   }

   @Override
   public int hudHeight() {
      return 103;
   }

   @Override
   public HudPlacement.Box defaultBox(int screenW, int screenH, int w, int h) {
      return new HudPlacement.Box(screenW - 2 - w, screenH / 2 + 50 - 80 - 10, w, h);
   }

   @Override
   public boolean hudRelevant() {
      return "gamma".equals(ClientPowerState.get().powerId()) || GammaRageState.localRage() > 0.005F;
   }

   public void render(GuiGraphics g, DeltaTracker deltaTracker) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && !mc.options.hideGui && this.hudVisible()) {
         float rage = GammaRageState.localRage();
         if (!(rage <= 0.005F)) {
            rage = Mth.clamp(rage, 0.0F, 1.0F);
            if (lastRage >= 0.0F && rage > lastRage + 0.008F) {
               pulse = 1.0F;
            }

            lastRage = rage;
            pulse = Math.max(0.0F, pulse - 0.05F);
            draw(g, mc.font, this.layout(g.guiWidth(), g.guiHeight()), rage, pulse);
         }
      }
   }

   @Override
   public void drawPreview(GuiGraphics g, HudPlacement.Box box) {
      draw(g, Minecraft.getInstance().font, box, Mth.clamp(GammaRageState.localRage(), 0.0F, 1.0F), 0.0F);
   }

   private static void draw(GuiGraphics g, Font font, HudPlacement.Box box, float rage, float flash) {
      int x1 = box.right() - 14;
      int x0 = x1 - 7;
      int topY = box.y() + 10;
      int bottomY = topY + 80;
      g.fill(x0 - 1, topY - 1, x1 + 1, bottomY + 1, -14671840);
      g.fill(x0, topY, x1, bottomY, -1442840576);
      int fillH = Math.round(80.0F * rage);
      if (fillH > 0) {
         g.fill(x0, bottomY - fillH, x1, bottomY, fillColor(rage));
      }

      if (flash > 0.01F) {
         int a = (int)(Mth.clamp(flash, 0.0F, 1.0F) * 150.0F);
         if (fillH > 0) {
            g.fill(x0, bottomY - fillH, x1, bottomY, a << 24 | 16777215);
         }

         int frame = a << 24 | 5552164;
         g.fill(x0 - 2, topY - 2, x1 + 2, topY - 1, frame);
         g.fill(x0 - 2, bottomY + 1, x1 + 2, bottomY + 2, frame);
         g.fill(x0 - 2, topY - 1, x0 - 1, bottomY + 1, frame);
         g.fill(x1 + 1, topY - 1, x1 + 2, bottomY + 1, frame);
      }

      for (float bound : TIER_FROM) {
         if (!(bound <= 0.0F)) {
            int ty = bottomY - Math.round(80.0F * bound);
            g.fill(x0, ty, x1, ty + 1, -2130706433);
         }
      }

      int color = fillColor(rage);
      int centre = x0 + 3;
      Component label = Component.translatable("awakened.gamma.rage");
      g.drawString(font, label, GuiPanels.centredWithin(centre, font.width(label), box.x(), box.right()), topY - 10, color);
      Component tier = tierName(rage);
      g.drawString(font, tier, GuiPanels.centredWithin(centre, font.width(tier), box.x(), box.right()), bottomY + 4, color);
   }

   private static int fillColor(float rage) {
      int r = (int)Mth.lerp(rage, 166.0F, 84.0F);
      int gr = (int)Mth.lerp(rage, 222.0F, 184.0F);
      int b = (int)Mth.lerp(rage, 116.0F, 36.0F);
      return 0xFF000000 | r << 16 | gr << 8 | b;
   }

   private static Component tierName(float rage) {
      for (int i = 0; i < TIER_FROM.length; i++) {
         if (rage >= TIER_FROM[i]) {
            return Component.translatable("awakened.gamma.tier." + TIER_KEY[i]);
         }
      }

      return Component.translatable("awakened.gamma.tier.calm");
   }
}
