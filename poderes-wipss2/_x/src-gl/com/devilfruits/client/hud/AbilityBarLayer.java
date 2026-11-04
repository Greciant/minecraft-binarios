package com.devilfruits.client.hud;

import com.devilfruits.client.ClientFruitState;
import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.config.CoreClientConfig;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.form.FormAbility;
import com.devilfruits.core.form.FruitForm;
import com.devilfruits.core.fruit.DevilFruit;
import com.devilfruits.core.fruit.FruitTheme;
import java.util.List;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

public final class AbilityBarLayer {
   private static int barX;
   private static int barY;
   private static int barLength = DFHud.barLength(7);
   private static AbilityBarLayer.Layout layout = AbilityBarLayer.Layout.HORIZONTAL;

   public static int barX() {
      return barX;
   }

   public static int barY() {
      return barY;
   }

   public static boolean vertical() {
      return layout == AbilityBarLayer.Layout.VERTICAL;
   }

   public static void render(GuiGraphics gg, DeltaTracker delta) {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      DevilFruit fruit = ClientFruitState.fruit();
      if (player != null && !mc.options.hideGui && !player.isSpectator() && fruit != null && !DFHud.hidden()) {
         List<FormAbility> abilities = ClientFruitState.abilities();
         float partial = delta.getGameTimeDeltaPartialTick(false);
         barLength = DFHud.barLength(abilities.size());
         computeLayout(gg, mc, player, fruit.hasForms());
         PlayerFruitData data = ClientFruitState.data();
         FruitTheme theme = fruit.theme();
         if ((Boolean)CoreClientConfig.SHOW_ABILITY_BAR.get()) {
            renderSlots(gg, mc, data, abilities, theme, partial);
            if ((Boolean)CoreClientConfig.FRUIT_LOCKED.get()) {
               renderLocked(gg);
            }

            if (fruit.hasForms()) {
               renderForm(gg, mc, data, partial);
            }
         }

         renderEnergy(gg, mc, data, theme, partial);
      }
   }

   private static void computeLayout(GuiGraphics gg, Minecraft mc, LocalPlayer player, boolean forms) {
      int w = gg.guiWidth();
      int h = gg.guiHeight();
      CoreClientConfig.HudAnchor anchor = (CoreClientConfig.HudAnchor)CoreClientConfig.HUD_ANCHOR.get();
      boolean offhand = !player.getOffhandItem().isEmpty();
      boolean rightHanded = player.getMainArm() == HumanoidArm.RIGHT;
      boolean attackHotbar = mc.options.attackIndicator().get() == AttackIndicatorStatus.HOTBAR;
      int reservedRight = (!rightHanded && offhand ? 29 : 0) + (attackHotbar && rightHanded ? 24 : 0);
      int reservedLeft = (rightHanded && offhand ? 29 : 0) + (attackHotbar && !rightHanded ? 24 : 0);
      int span = Math.max(barLength + (forms ? 20 : 0), 142);
      int rightX = w / 2 + 91 + 6 + reservedRight;
      int leftX = w / 2 - 91 - 6 - reservedLeft - span;
      int y = h - 22;
      layout = AbilityBarLayer.Layout.HORIZONTAL;
      switch (anchor) {
         case RIGHT_OF_HOTBAR:
            barX = rightX;
            break;
         case LEFT_OF_HOTBAR:
            barX = leftX;
            break;
         case RIGHT_EDGE:
         case LEFT_EDGE:
            layout = AbilityBarLayer.Layout.VERTICAL;
            break;
         case AUTO:
            if (rightX + span + 2 <= w) {
               barX = rightX;
            } else if (leftX >= 2) {
               barX = leftX;
            } else {
               layout = AbilityBarLayer.Layout.VERTICAL;
               anchor = CoreClientConfig.HudAnchor.RIGHT_EDGE;
            }
      }

      if (layout == AbilityBarLayer.Layout.VERTICAL) {
         barX = anchor == CoreClientConfig.HudAnchor.LEFT_EDGE ? 4 : w - 22 - 4;
         y = h / 2 - Math.max(barLength, 142) / 2;
      }

      barX = barX + (Integer)CoreClientConfig.HUD_OFFSET_X.get();
      barY = y + (Integer)CoreClientConfig.HUD_OFFSET_Y.get();
   }

   private static int slotX(int i) {
      return layout == AbilityBarLayer.Layout.HORIZONTAL ? barX + 1 + i * 20 : barX + 1;
   }

   private static int slotY(int i) {
      return layout == AbilityBarLayer.Layout.HORIZONTAL ? barY + 1 : barY + 1 + i * 20;
   }

   private static void drawBar(GuiGraphics gg, int slots) {
      int tex = 202;
      boolean h = layout == AbilityBarLayer.Layout.HORIZONTAL;
      ResourceLocation sprite = h ? DFHud.BAR : DFHud.BAR_VERTICAL;
      segment(gg, sprite, tex, h, 0, 0, 3);

      for (int i = 0; i < slots; i++) {
         int from = 3 + 20 * (i % 10);
         segment(gg, sprite, tex, h, from, 3 + 20 * i, i == slots - 1 ? 18 : 20);
      }

      segment(gg, sprite, tex, h, tex - 1, 20 * slots + 1, 1);
   }

   private static void renderLocked(GuiGraphics gg) {
      boolean horizontal = layout == AbilityBarLayer.Layout.HORIZONTAL;
      int w = horizontal ? barLength : 22;
      int h = horizontal ? 22 : barLength;
      gg.fill(barX + 1, barY + 1, barX + w - 1, barY + h - 1, -1609560044);
      gg.blitSprite(DFHud.LOCK, barX + w / 2 - 4, barY + h / 2 - 5, 8, 10);
   }

   private static void segment(GuiGraphics gg, ResourceLocation sprite, int tex, boolean horizontal, int from, int at, int size) {
      if (horizontal) {
         gg.blitSprite(sprite, tex, 22, from, 0, barX + at, barY, size, 22);
      } else {
         gg.blitSprite(sprite, 22, tex, 0, from, barX, barY + at, 22, size);
      }
   }

   private static void renderSlots(GuiGraphics gg, Minecraft mc, PlayerFruitData data, List<FormAbility> abilities, FruitTheme theme, float partial) {
      Font font = mc.font;
      long gameTime = mc.level.getGameTime();
      int selected = ClientFruitState.selected();
      ResourceLocation charging = ClientFruitState.chargingAbility();
      float time = (float)gameTime + partial;
      drawBar(gg, abilities.size());

      for (int i = 0; i < abilities.size(); i++) {
         FormAbility entry = abilities.get(i);
         Ability ability = entry.ability();
         int sx = slotX(i);
         int sy = slotY(i);
         boolean locked = ClientFruitState.isLocked(ability);
         boolean enabled = ClientFruitState.enabled(ability);
         if (locked || !enabled || data.suppressed()) {
            gg.setColor(0.32F, 0.32F, 0.36F, 1.0F);
         }

         gg.blitSprite(ability.iconSprite(), sx + 2, sy + 2, 16, 16);
         gg.setColor(1.0F, 1.0F, 1.0F, 1.0F);
         if (ability.id().equals(charging)) {
            float pulse = 0.35F + 0.25F * Mth.sin(time * 0.8F);
            gg.fill(sx + 2, sy + 2, sx + 18, sy + 18, (int)(pulse * 255.0F) << 24 | theme.chargeGlowColor());
         }

         float cd = data.cooldownFraction(ability.id(), gameTime, partial);
         if (cd > 0.0F && !locked) {
            int top = sy + 2 + Mth.floor(16.0F * (1.0F - cd));
            int bottom = top + Mth.ceil(16.0F * cd);
            gg.fill(sx + 2, top, sx + 18, bottom, Integer.MAX_VALUE);
         }

         gg.pose().pushPose();
         gg.pose().translate(0.0F, 0.0F, 200.0F);
         if (locked) {
            gg.blitSprite(DFHud.LOCK, sx + 6, sy + 4, 8, 10);
            String lvl = String.valueOf(ClientFruitState.requiredMastery(ability));
            gg.drawString(font, lvl, sx + 19 - font.width(lvl), sy + 11, 16744576, true);
         } else if (!enabled) {
            gg.drawString(font, "✕", sx + 6, sy + 6, 16733525, true);
         } else {
            int ticksLeft = data.cooldownTicksLeft(ability.id(), gameTime);
            if (ticksLeft > 0) {
               String secs = ticksLeft >= 20 ? String.valueOf(Mth.ceil((float)ticksLeft / 20.0F)) : "";
               if (!secs.isEmpty()) {
                  gg.drawString(font, secs, sx + 10 - font.width(secs) / 2, sy + 6, 16777215, true);
               }
            } else {
               int cost = ClientFruitState.energyCost(entry);
               if (cost > 0) {
                  String text = String.valueOf(cost);
                  boolean affordable = data.energy() >= (float)cost;
                  gg.drawString(font, text, sx + 19 - font.width(text), sy + 11, affordable ? theme.costColor() : 16736352, true);
               }
            }
         }

         gg.pose().popPose();
         if (data.isToggleActive(ability.id())) {
            float pulse = 0.6F + 0.4F * Mth.sin(time * 0.6F);
            gg.setColor(1.0F, 1.0F, 1.0F, pulse);
            gg.blitSprite(theme.activeFrame(), sx - 1, sy - 1, 22, 22);
            gg.setColor(1.0F, 1.0F, 1.0F, 1.0F);
         }
      }

      if (data.suppressed()) {
         int w = layout == AbilityBarLayer.Layout.HORIZONTAL ? barLength : 22;
         int h = layout == AbilityBarLayer.Layout.HORIZONTAL ? 22 : barLength;
         gg.fill(barX, barY, barX + w, barY + h, 1344819400);
      }

      if (!abilities.isEmpty()) {
         gg.blitSprite(theme.selection(), slotX(selected) - 2, slotY(selected) - 2, 24, 24);
      }
   }

   private static void renderForm(GuiGraphics gg, Minecraft mc, PlayerFruitData data, float partial) {
      FruitForm form = ClientFruitState.form();
      if (form != null) {
         int fx = layout == AbilityBarLayer.Layout.HORIZONTAL ? barX + barLength + 1 : barX + 1;
         int fy = layout == AbilityBarLayer.Layout.HORIZONTAL ? barY + 1 : barY + barLength + 1;
         gg.blitSprite(DFHud.FORM_FRAME, fx, fy, 20, 20);
         gg.blitSprite(form.iconSprite(), fx + 2, fy + 2, 16, 16);
         float cd = data.cooldownFraction(PlayerFruitData.FORM_COOLDOWN, mc.level.getGameTime(), partial);
         if (cd > 0.0F) {
            int top = fy + 2 + Mth.floor(16.0F * (1.0F - cd));
            gg.fill(fx + 2, top, fx + 18, top + Mth.ceil(16.0F * cd), Integer.MAX_VALUE);
         }

         int limit = form.isBase() ? 0 : form.config().timeLimitSeconds();
         if (limit > 0) {
            int left = Mth.ceil(Math.max(0.0F, data.energy()) * (float)limit / (float)Math.max(1, ClientFruitState.maxEnergy()));
            String text = String.valueOf(left);
            gg.pose().pushPose();
            gg.pose().translate(0.0F, 0.0F, 200.0F);
            gg.drawString(mc.font, text, fx + 19 - mc.font.width(text), fy + 12, left <= 10 ? 16733525 : 16777215, true);
            gg.pose().popPose();
         }
      }
   }

   private static void renderEnergy(GuiGraphics gg, Minecraft mc, PlayerFruitData data, FruitTheme theme, float partial) {
      Font font = mc.font;
      int max = Math.max(1, ClientFruitState.maxEnergy());
      float fraction = Mth.clamp(data.energy() / (float)max, 0.0F, 1.0F);
      int length = 142;
      int filled = Mth.floor(fraction * (float)length);
      int ex;
      int ey;
      if (layout == AbilityBarLayer.Layout.HORIZONTAL) {
         ex = barX;
         ey = barY - 7;
         gg.blitSprite(theme.energyBackground(), ex, ey, length, 5);
         if (filled > 0) {
            gg.blitSprite(theme.energyProgress(), length, 5, 0, 0, ex, ey, filled, 5);
         }
      } else {
         boolean left = CoreClientConfig.HUD_ANCHOR.get() == CoreClientConfig.HudAnchor.LEFT_EDGE;
         ex = left ? barX + 24 : barX - 7;
         ey = barY;
         gg.blitSprite(theme.energyBackgroundVertical(), ex, ey, 5, length);
         if (filled > 0) {
            gg.blitSprite(theme.energyProgressVertical(), 5, length, 0, length - filled, ex, ey + length - filled, 5, filled);
         }
      }

      String level = String.valueOf(data.mastery());
      int lx;
      int ly;
      if (layout == AbilityBarLayer.Layout.HORIZONTAL) {
         lx = ex + 1;
         ly = ey - 9;
      } else {
         lx = barX + 11 - font.width(level) / 2;
         ly = barY - 11;
      }

      outlined(gg, font, level, lx, ly, data.mastery() >= 10 ? theme.maxMasteryColor() : theme.masteryColor());
      if (layout == AbilityBarLayer.Layout.HORIZONTAL && (Boolean)CoreClientConfig.SHOW_ENERGY_TEXT.get()) {
         String text = Mth.floor(data.energy()) + "/" + max;
         gg.drawString(font, text, ex + length - font.width(text), ey - 9, theme.energyTextColor(), true);
      }

      int sx = layout == AbilityBarLayer.Layout.HORIZONTAL ? lx + font.width(level) + 3 : barX + 6;
      int sy = layout == AbilityBarLayer.Layout.HORIZONTAL ? ly - 1 : barY - 22;
      DevilFruit fruit = ClientFruitState.fruit();
      if (fruit == null
         || !LogiaHud.render(
            gg,
            mc,
            fruit,
            data,
            layout == AbilityBarLayer.Layout.HORIZONTAL ? ex : sx,
            layout == AbilityBarLayer.Layout.HORIZONTAL ? ly - 11 : sy,
            layout == AbilityBarLayer.Layout.HORIZONTAL,
            partial
         )) {
         if (data.suppressed()) {
            float pulse = 0.6F + 0.4F * Mth.sin(((float)mc.level.getGameTime() + partial) * 0.4F);
            gg.setColor(1.0F, 1.0F, 1.0F, pulse);
            gg.blitSprite(DFHud.SUPPRESSED, sx, sy, 9, 9);
            gg.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            if (layout == AbilityBarLayer.Layout.HORIZONTAL) {
               gg.drawString(font, Component.translatable("hud.devilfruits.suppressed"), sx + 11, ly, 7317759, true);
            }
         }
      }
   }

   private static void outlined(GuiGraphics gg, Font font, String text, int x, int y, int color) {
      gg.drawString(font, text, x + 1, y, 0, false);
      gg.drawString(font, text, x - 1, y, 0, false);
      gg.drawString(font, text, x, y + 1, 0, false);
      gg.drawString(font, text, x, y - 1, 0, false);
      gg.drawString(font, text, x, y, color, false);
   }

   private AbilityBarLayer() {
   }

   private static enum Layout {
      HORIZONTAL,
      VERTICAL;
   }
}
