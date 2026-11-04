package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.client.screen.SoulMenuScreen;
import com.awakenedmod.awakened.content.summoning.SoulData;
import com.awakenedmod.awakened.content.summoning.SummonAttachments;
import com.awakenedmod.awakened.content.summoning.SummonSouls;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw.Layer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;

public final class SoulHud implements Layer {
   private static final int THEME = -13049400;
   private static final int PANEL = -938470376;
   private static final int FRAME = 1426063360;
   private static final int TEXT = -1;
   private static final int DIM = -5785420;
   private static final int OVER = -34182;
   private static final int TRACK = -872085494;
   public static final SoulHud.Chip CHIP = new SoulHud.Chip();
   public static final SoulHud.Roster ROSTER = new SoulHud.Roster();

   public void render(GuiGraphics g, DeltaTracker deltaTracker) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.level != null && !mc.options.hideGui && summoning()) {
         CHIP.render(g, mc);
         ROSTER.render(g, mc);
      }
   }

   private static boolean summoning() {
      return "summoning".equals(ClientPowerState.get().powerId());
   }

   private static void panel(GuiGraphics g, int x, int y, int w, int h) {
      g.fill(x - 2, y - 2, x + w + 2, y + h + 2, 1426063360);
      g.fill(x - 1, y - 1, x + w + 1, y + h + 1, -13049400);
      g.fill(x, y, x + w, y + h, -938470376);
   }

   private static void drawScaledItem(GuiGraphics g, ItemStack stack, int x, int y, float size) {
      float s = size / 16.0F;
      g.pose().pushPose();
      g.pose().translate((float)x, (float)y, 0.0F);
      g.pose().scale(s, s, 1.0F);
      g.renderItem(stack, 0, 0);
      g.pose().popPose();
   }

   public static final class Chip implements FreeHud {
      private static final int PANEL_W = 90;
      private static final int PANEL_H = 24;
      private static final int HINT_H = 10;

      private Chip() {
      }

      @Override
      public String hudId() {
         return "soul_chip";
      }

      @Override
      public HudSettings hudSettings() {
         return ClientConfig.HUD_SOUL_CHIP;
      }

      @Override
      public boolean hudRelevant() {
         return SoulHud.summoning();
      }

      @Override
      public int hudWidth() {
         Component hint = hint();
         return hint == null ? 90 : Math.max(90, Minecraft.getInstance().font.width(hint));
      }

      @Override
      public int hudHeight() {
         return hint() == null ? 24 : 34;
      }

      @Override
      public HudPlacement.Box defaultBox(int screenW, int screenH, int w, int h) {
         return new HudPlacement.Box(Math.max(4, screenW / 2 - 91 - 90 - 4), screenH - h - 3, w, h);
      }

      private void render(GuiGraphics g, Minecraft mc) {
         if (this.hudVisible()) {
            this.draw(g, this.layout(g.guiWidth(), g.guiHeight()), mc.font, (SoulData)mc.player.getData(SummonAttachments.SOULS));
         }
      }

      @Override
      public void drawPreview(GuiGraphics g, HudPlacement.Box box) {
         Minecraft mc = Minecraft.getInstance();
         this.draw(g, box, mc.font, mc.player == null ? new SoulData() : (SoulData)mc.player.getData(SummonAttachments.SOULS));
      }

      private void draw(GuiGraphics g, HudPlacement.Box box, Font font, SoulData souls) {
         int x = box.x();
         int y = box.bottom() - 24;
         Component hint = hint();
         if (hint != null) {
            g.drawString(font, hint, x, box.y(), -13049400);
         }

         SoulHud.panel(g, x, y, 90, 24);
         String type = souls.active();
         if (type.isEmpty()) {
            g.drawString(font, Component.translatable("awakened.souls.no_active"), x + 6, y + 8, -5785420);
         } else {
            g.renderItem(SoulIcons.of(type), x + 4, y + 4);
            String name = font.plainSubstrByWidth(SummonSouls.displayName(type).getString(), 64);
            g.drawString(font, name, x + 23, y + 4, -13049400);
            g.drawString(font, "x" + souls.count(type), x + 23, y + 14, -5785420);
         }

         int cap = SummonSouls.capForLevel(ClientPowerState.get().masteryLevel());
         String total = souls.total() + "/" + cap;
         g.drawString(font, total, x + 90 - font.width(total) - 4, y + 14, souls.total() >= cap ? -34182 : -5785420);
      }

      @Nullable
      private static Component hint() {
         return SoulMenuScreen.openCount >= 5
            ? null
            : Component.translatable("awakened.souls.hud_hint", new Object[]{AwakenedKeybinds.SECONDARY_ABILITY.getTranslatedKeyMessage()});
      }
   }

   public static final class Roster implements FreeHud {
      private static final int W = 80;
      private static final int HEADER_H = 12;
      private static final int ROW_H = 11;
      private static final int MORE_H = 10;
      private static final int BOTTOM_PAD = 2;
      private static final int MAX_ROWS = 8;
      private static final int TRACKER_CLEARANCE = 34;

      private Roster() {
      }

      @Override
      public String hudId() {
         return "soul_roster";
      }

      @Override
      public HudSettings hudSettings() {
         return ClientConfig.HUD_SOUL_ROSTER;
      }

      @Override
      public boolean hudRelevant() {
         return SoulHud.summoning();
      }

      @Override
      public int hudWidth() {
         return 80;
      }

      @Override
      public int hudHeight() {
         return height(MinionView.owned(Minecraft.getInstance()).size());
      }

      @Override
      public HudPlacement.Box defaultBox(int screenW, int screenH, int w, int h) {
         return new HudPlacement.Box(4, 34, w, h);
      }

      private static int height(int count) {
         int rows = Math.max(1, Math.min(8, count));
         int more = count > 8 ? 10 : 0;
         return 12 + rows * 11 + more + 2;
      }

      private void render(GuiGraphics g, Minecraft mc) {
         List<Mob> minions = MinionView.owned(mc);
         if (!minions.isEmpty() && this.hudVisible()) {
            this.draw(g, this.layout(g.guiWidth(), g.guiHeight()), mc.font, minions);
         }
      }

      @Override
      public void drawPreview(GuiGraphics g, HudPlacement.Box box) {
         Minecraft mc = Minecraft.getInstance();
         List<Mob> minions = MinionView.owned(mc);
         if (!minions.isEmpty()) {
            this.draw(g, box, mc.font, minions);
         } else {
            SoulHud.panel(g, box.x(), box.y(), box.width(), box.height());
            header(g, mc.font, box, 0);
            g.drawString(mc.font, Component.translatable("awakened.hud.soul_roster.empty"), box.x() + 5, box.y() + 12 + 1, -5785420);
         }
      }

      private void draw(GuiGraphics g, HudPlacement.Box box, Font font, List<Mob> minions) {
         minions.sort(Comparator.comparingDouble(mx -> (double)(mx.getHealth() / Math.max(1.0F, mx.getMaxHealth()))));
         int x = box.x();
         int w = box.width();
         SoulHud.panel(g, x, box.y(), w, box.height());
         header(g, font, box, MinionView.fielded(minions));
         int shown = Math.min(8, minions.size());
         int ry = box.y() + 12;

         for (int i = 0; i < shown; i++) {
            Mob m = minions.get(i);
            float frac = Math.max(0.0F, Math.min(1.0F, m.getHealth() / Math.max(1.0F, m.getMaxHealth())));
            SoulHud.drawScaledItem(g, SoulIcons.of(m.getType()), x + 4, ry, 9.0F);
            int barX = x + 15;
            int barW = w - 15 - 4;
            int barY = ry + 1;
            g.fill(barX, barY, barX + barW, barY + 7, -872085494);
            g.fill(barX, barY, barX + (int)((float)barW * frac), barY + 7, healthColor(frac));
            ry += 11;
         }

         if (minions.size() > 8) {
            g.drawString(font, Component.translatable("awakened.souls.army_more", new Object[]{minions.size() - 8}), x + 5, ry + 1, -5785420);
         }
      }

      private static void header(GuiGraphics g, Font font, HudPlacement.Box box, int fielded) {
         int cap = SummonSouls.maxMinionsForLevel(ClientPowerState.get().masteryLevel());
         g.drawString(
            font,
            Component.translatable("awakened.souls.army_count", new Object[]{fielded, cap}),
            box.x() + 5,
            box.y() + 3,
            fielded >= cap ? -34182 : -13049400
         );
      }

      private static int healthColor(float frac) {
         if (frac > 0.5F) {
            return -12594609;
         } else {
            return frac > 0.25F ? -2047936 : -2609104;
         }
      }
   }
}
