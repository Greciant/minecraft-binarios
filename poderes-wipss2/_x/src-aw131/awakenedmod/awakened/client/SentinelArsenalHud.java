package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.content.entity.sentinel.AbsorbedPower;
import com.awakenedmod.awakened.content.entity.sentinel.SentinelEntity;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw.Layer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

public final class SentinelArsenalHud implements Layer, FreeHud {
   private static final int CELL = 20;
   private static final int GAP = 3;
   private static final int ROW_Y = 24;
   private static final int ICON = 16;
   private static final int FLASH_TICKS = 70;
   private static final int BANNER_GAP = 3;
   private static final int LINE_H = 9;
   private static final int CELL_BG = -871757302;
   private static final int EMPTY_BORDER = -2006949784;
   private static final int BANNER = -43691;
   private static final int PREVIEW_CELLS = 3;
   public static final SentinelArsenalHud ARSENAL = new SentinelArsenalHud();
   private int lastMask;
   private long flashUntil;
   private Component flashText = Component.empty();
   private static long lookedUpAt = Long.MIN_VALUE;
   private static boolean sentinelNear;
   private static int absorbed;

   private SentinelArsenalHud() {
   }

   @Override
   public String hudId() {
      return "sentinel_arsenal";
   }

   @Override
   public HudSettings hudSettings() {
      return ClientConfig.HUD_SENTINEL_ARSENAL;
   }

   @Override
   public int hudWidth() {
      int count = Integer.bitCount(absorbedMask());
      return rowWidth(count > 0 ? count : 3);
   }

   @Override
   public int hudHeight() {
      return 32;
   }

   @Override
   public HudPlacement.Box defaultBox(int screenW, int screenH, int w, int h) {
      return new HudPlacement.Box((screenW - w) / 2, 24, w, h);
   }

   public void render(GuiGraphics g, DeltaTracker deltaTracker) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.level != null && !mc.options.hideGui && this.hudVisible()) {
         if (!sentinelNear()) {
            this.lastMask = 0;
         } else {
            int mask = absorbedMask();
            long now = mc.level.getGameTime();
            int added = mask & ~this.lastMask;
            if (added != 0) {
               AbsorbedPower power = firstBit(added);
               if (power != null) {
                  this.flashText = Component.translatable(
                     "entity.awakened.sentinel.adapted", new Object[]{Component.translatable("power.awakened." + power.powerId())}
                  );
                  this.flashUntil = now + 70L;
               }
            }

            this.lastMask = mask;
            this.draw(g, mc.font, this.layout(g.guiWidth(), g.guiHeight()), mask, now < this.flashUntil);
         }
      }
   }

   @Override
   public void drawPreview(GuiGraphics g, HudPlacement.Box box) {
      int mask = absorbedMask();
      if (mask != 0) {
         this.draw(g, Minecraft.getInstance().font, box, mask, false);
      } else {
         for (int i = 0; i < 3; i++) {
            cell(g, box.x() + i * 23, box.y(), -2006949784);
         }
      }
   }

   private void draw(GuiGraphics g, Font font, HudPlacement.Box box, int mask, boolean flashing) {
      int x = box.x();

      for (AbsorbedPower power : AbsorbedPower.values()) {
         if ((mask & power.bit()) != 0) {
            drawCell(g, power, x, box.y());
            x += 23;
         }
      }

      if (flashing) {
         g.drawCenteredString(font, this.flashText, box.right() - box.width() / 2, box.y() + 20 + 3, -43691);
      }
   }

   private static void drawCell(GuiGraphics g, AbsorbedPower power, int x, int y) {
      cell(g, x, y, 0xFF000000 | power.glowColor() & 16777215);
      int ix = x + 2;
      int iy = y + 2;
      g.blit(AbilityIcons.of(power.iconAbilityId()), ix, iy, 0.0F, 0.0F, 16, 16, 16, 16);
   }

   private static void cell(GuiGraphics g, int x, int y, int border) {
      g.fill(x - 1, y - 1, x + 20 + 1, y + 20 + 1, border);
      g.fill(x, y, x + 20, y + 20, -871757302);
   }

   private static int rowWidth(int count) {
      return count * 20 + (count - 1) * 3;
   }

   private static void findSentinel() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.level != null) {
         long now = mc.level.getGameTime();
         if (now != lookedUpAt) {
            lookedUpAt = now;
            SentinelEntity boss = nearestSentinel(mc);
            sentinelNear = boss != null;
            absorbed = boss == null ? 0 : boss.getAbsorbedMask();
         }
      } else {
         lookedUpAt = Long.MIN_VALUE;
         sentinelNear = false;
         absorbed = 0;
      }
   }

   private static boolean sentinelNear() {
      findSentinel();
      return sentinelNear;
   }

   private static int absorbedMask() {
      findSentinel();
      return absorbed;
   }

   private static SentinelEntity nearestSentinel(Minecraft mc) {
      SentinelEntity best = null;
      double bestSq = 6400.0;

      for (Entity entity : mc.level.entitiesForRendering()) {
         if (entity instanceof SentinelEntity) {
            SentinelEntity sentinel = (SentinelEntity)entity;
            if (sentinel.isAlive()) {
               double d = sentinel.distanceToSqr(mc.player);
               if (d < bestSq) {
                  bestSq = d;
                  best = sentinel;
               }
            }
         }
      }

      return best;
   }

   private static AbsorbedPower firstBit(int bits) {
      for (AbsorbedPower power : AbsorbedPower.values()) {
         if ((bits & power.bit()) != 0) {
            return power;
         }
      }

      return null;
   }
}
