package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.client.screen.GuiPanels;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.power.Power;
import com.awakenedmod.awakened.core.power.PowerRegistry;
import com.awakenedmod.awakened.core.power.PowerResource;
import com.awakenedmod.awakened.core.stamina.StaminaResource;
import java.lang.ref.WeakReference;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw.Layer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class StaminaHud implements Layer {
   private static final int BAR_WIDTH = 182;
   private static final int BAR_HEIGHT = 6;
   private static final int BOTTOM_OFFSET = 64;
   private static final int COLOR_BG = -1442840576;
   private static final int COLOR_BORDER = -14671840;
   private static final int COLOR_FLASH = -43691;
   private static final int COLOR_REASON = -38037;
   private static final int COLOR_LOCKED = -3377289;
   private static final int COLOR_DIM = -4210753;
   private static final int COLOR_LIMIT = -1;
   private static final int LIMIT_PROUD = 2;
   private static final int COLOR_UNREACHABLE = -1441787888;
   private static final int COLOR_COOLDOWN = -8400897;
   private static final int ICON = 16;
   private static final int HOTBAR_H = 22;
   private static final int ICON_GAP = 5;
   private static final int GAUGE_W = 7;
   private static final int GAUGE_H = 60;
   private static final float GAUGE_MAX = 2.3F;
   private static final int GAUGE_INSET = 16;
   private static final int GAUGE_DROP = 50;
   private static final int GAUGE_RIGHT_GAP = 14;
   private static final int GAUGE_CENTRE_INSET = 18;
   private static final int GAUGE_LABEL_GAP = 4;
   private static final int GAUGE_BADGE_GAP = 3;
   private static final int BADGE_ICON_W = 11;
   private static final String POWER_GROUP = "power_hud";
   public static final StaminaHud.StaminaBar STAMINA_BAR = new StaminaHud.StaminaBar();
   public static final StaminaHud.AbilityBar ABILITY_BAR = new StaminaHud.AbilityBar();
   public static final StaminaHud.AbilityIcon ABILITY_ICON = new StaminaHud.AbilityIcon();
   public static final StaminaHud.ResourceGauge RESOURCE_GAUGE = new StaminaHud.ResourceGauge();
   private static final List<StaminaHud.Part> PARTS = List.of(STAMINA_BAR, ABILITY_BAR, ABILITY_ICON, RESOURCE_GAUGE);
   private static final int COLOR_COOLDOWN_SWEEP = -1342177280;

   private static int shippedBarX(int screenW) {
      return (screenW - 182) / 2;
   }

   private static int shippedBarY(int screenH) {
      return screenH - 64;
   }

   public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
      Minecraft mc = Minecraft.getInstance();
      ClientPowerState state = ClientPowerState.get();
      if (mc.player != null && !mc.options.hideGui && state.awakened()) {
         Power power = PowerRegistry.get(state.powerId());
         if (power != null) {
            for (StaminaHud.Part part : PARTS) {
               if (part.hudVisible() && part.shows(state, power)) {
                  part.draw(guiGraphics, part.layout(guiGraphics.guiWidth(), guiGraphics.guiHeight()), state, power);
               }
            }
         }
      }
   }

   @Nullable
   private static Power localPower() {
      ClientPowerState state = ClientPowerState.get();
      return state.awakened() ? PowerRegistry.get(state.powerId()) : null;
   }

   private static Font font() {
      return Minecraft.getInstance().font;
   }

   private static int barColor(Power power, float fraction) {
      return power.resourceBarColor(Minecraft.getInstance().player, fraction);
   }

   private static float barCeiling(Power power, ClientPowerState state) {
      float ceiling = power.resourceBarCeiling(Minecraft.getInstance().player, state.masteryLevel());
      return Math.max(0.0F, Math.min(1.0F, ceiling));
   }

   private static int barOverColor(Power power, float fraction, float ceiling) {
      return power.resourceBarOverColor(Minecraft.getInstance().player, fraction, ceiling);
   }

   private static boolean affordable(ClientPowerState state, Power power, Ability ability) {
      PowerResource resource = (PowerResource)(power != null ? power.resource() : StaminaResource.INSTANCE);
      return resource.affordable(
         new PowerResource.Fill(state.stamina(), state.staminaMax(), state.resourceFraction(), state.infiniteStamina()),
         ability.shownCost(Minecraft.getInstance().player)
      );
   }

   private static boolean drawCooldown(GuiGraphics g, Font font, ClientPowerState state, int slot, int iconX, int iconY) {
      int left = state.cooldownLeft(slot);
      if (left <= 0) {
         return false;
      } else {
         int covered = Math.max(1, Math.round(16.0F * state.cooldownFraction(slot)));
         g.fill(iconX, iconY + 16 - covered, iconX + 16, iconY + 16, -1342177280);
         String seconds = Integer.toString((left + 19) / 20);
         g.drawString(font, seconds, iconX + (16 - font.width(seconds)) / 2 + 1, iconY + 4, -8400897, true);
         return true;
      }
   }

   private static String iconId(Ability ability) {
      String id = ability.id();
      return "web_wings".equals(id) && SpiderSwingController.wingsEnabled() ? "web_wings_on" : id;
   }

   private static void track(GuiGraphics g, int x0, int y0, int x1, int y1) {
      g.fill(x0 - 1, y0 - 1, x1 + 1, y1 + 1, -14671840);
      g.fill(x0, y0, x1, y1, -1442840576);
   }

   public static final class AbilityBar implements StaminaHud.Part {
      private static final int HEADLINE_LIFT = 11;
      private static final int TRAY_GAP = 6;
      private static final int CELL_W = 22;
      private static final int CELL_H = 26;
      private static final int CELL_GAP = 3;
      private static final int LABEL_ROOM = 24;
      private static final int LIT_WASH_ALPHA = 1291845632;
      private static final int LIT_RING_ALPHA = -872415232;

      private AbilityBar() {
      }

      @Override
      public String hudId() {
         return "ability_hud";
      }

      @Override
      public HudSettings hudSettings() {
         return ClientConfig.HUD_ABILITY_BAR;
      }

      @Override
      public String hudGroup() {
         return "power_hud";
      }

      @Override
      public boolean hudRelevant() {
         return StaminaHud.localPower() != null;
      }

      @Override
      public boolean shows(ClientPowerState state, Power power) {
         return true;
      }

      public boolean showsFailReason() {
         return this.hudVisible() && StaminaHud.localPower() != null;
      }

      @Override
      public HudPlacement.Box defaultBox(int screenW, int screenH, int w, int h) {
         return new HudPlacement.Box(StaminaHud.shippedBarX(screenW) + 91 - this.leftPad(), StaminaHud.shippedBarY(screenH) - h, w, h);
      }

      @Override
      public int hudWidth() {
         return this.leftPad() + this.rightPad();
      }

      @Override
      public int hudHeight() {
         Power power = StaminaHud.localPower();
         return 11 + (trayShown(power) ? 32 : 0) + barLineRoom(power);
      }

      private static int barLineRoom(Power power) {
         int lines = power == null ? 0 : power.staminaBarLineRoom(ClientPowerState.get().masteryLevel());
         return lines > 0 ? StaminaHud.StaminaBar.barLinesHeight(lines) : 0;
      }

      @Override
      public void draw(GuiGraphics g, HudPlacement.Box box, ClientPowerState state, Power power) {
         Font font = StaminaHud.font();
         int centre = this.centreX(box);
         int theme = power.themeColor();
         drawHeadline(g, font, state, power, centre, box.y(), theme);
         if (trayShown(power)) {
            drawTray(g, font, state, power, centre, box.y() + 11, theme);
         }
      }

      @Override
      public void drawStandIn(GuiGraphics g, HudPlacement.Box box) {
         g.drawCenteredString(StaminaHud.font(), headline(ClientPowerState.get(), null), this.centreX(box), box.y(), -4210753);
      }

      private int centreX(HudPlacement.Box box) {
         return box.x() + this.leftPad();
      }

      private int leftPad() {
         Font font = StaminaHud.font();
         ClientPowerState state = ClientPowerState.get();
         Power power = StaminaHud.localPower();
         int pad = 91;
         if (trayShown(power)) {
            pad = Math.max(pad, trayLeft(trayWidth(power)));
         }

         return Math.max(pad, font.width(headline(state, power)) / 2);
      }

      private int rightPad() {
         Font font = StaminaHud.font();
         ClientPowerState state = ClientPowerState.get();
         Power power = StaminaHud.localPower();
         int pad = 91;
         if (trayShown(power)) {
            pad = Math.max(pad, trayWidth(power) / 2);
         }

         int headW = font.width(headline(state, power));
         return Math.max(pad, headW - headW / 2);
      }

      private static boolean trayShown(@Nullable Power power) {
         return power != null && !(Boolean)ClientConfig.HUD_ICONS_OFF.get();
      }

      private static int trayWidth(@Nullable Power power) {
         int slots = power == null ? 0 : power.abilityCount();
         return slots <= 0 ? 0 : slots * 22 + (slots - 1) * 3;
      }

      private static int trayLeft(int trayW) {
         return trayW - trayW / 2;
      }

      private static void drawTray(GuiGraphics g, Font font, ClientPowerState state, Power power, int centre, int trayY, int theme) {
         int slots = power.abilityCount();
         int trayX = centre - trayLeft(trayWidth(power));

         for (int slot = 0; slot < slots; slot++) {
            Ability ability = power.ability(slot);
            if (ability != null) {
               boolean active = slot == state.activeSlot();
               boolean locked = state.masteryLevel() < ability.unlockLevel();
               boolean affordable = StaminaHud.affordable(state, power, ability);
               boolean lit = !locked && power.abilityLitOnClient(Minecraft.getInstance().player, slot);
               int x0 = trayX + slot * 25;
               int border = active ? theme : (locked ? -10864070 : -14671840);
               int bg = active ? -871494130 : -1728053248;
               g.fill(x0 - 1, trayY - 1, x0 + 22 + 1, trayY + 26 + 1, border);
               g.fill(x0, trayY, x0 + 22, trayY + 26, bg);
               int iconX = x0 + 3;
               int iconY = trayY + 2;
               g.blit(AbilityIcons.of(StaminaHud.iconId(ability)), iconX, iconY, 0.0F, 0.0F, 16, 16, 16, 16);
               if (locked) {
                  g.fill(iconX, iconY, iconX + 16, iconY + 16, -1342177280);
               } else if (!StaminaHud.drawCooldown(g, font, state, slot, iconX, iconY) && active && !affordable) {
                  g.fill(iconX, iconY, iconX + 16, iconY + 16, 1624256560);
               }

               if (lit) {
                  drawLit(g, iconX, iconY, theme);
               }

               int cx = x0 + 11;
               String bottom;
               int bottomColor;
               if (locked) {
                  bottom = "Lv" + ability.unlockLevel();
                  bottomColor = -3377289;
               } else {
                  String level = ability.trayLabel(state.resourceTier());
                  bottom = level != null ? level : ability.costLabel(Minecraft.getInstance().player);
                  bottomColor = !affordable ? -43691 : (active ? theme : -4210753);
               }

               HudText.fitCentered(g, font, bottom, cx, trayY + 26 - 8, 24, bottomColor);
            }
         }
      }

      private static void drawLit(GuiGraphics g, int iconX, int iconY, int theme) {
         g.fill(iconX, iconY, iconX + 16, iconY + 16, theme & 16777215 | 1291845632);
         int ring = theme & 16777215 | -872415232;
         g.fill(iconX - 1, iconY - 1, iconX + 16 + 1, iconY, ring);
         g.fill(iconX - 1, iconY + 16, iconX + 16 + 1, iconY + 16 + 1, ring);
         g.fill(iconX - 1, iconY, iconX, iconY + 16, ring);
         g.fill(iconX + 16, iconY, iconX + 16 + 1, iconY + 16, ring);
      }

      private static void drawHeadline(GuiGraphics g, Font font, ClientPowerState state, Power power, int centre, int y, int theme) {
         g.drawCenteredString(font, headline(state, power), centre, y, headlineColor(state, power, theme));
      }

      private static Component headline(ClientPowerState state, @Nullable Power power) {
         if (power == null) {
            return Component.translatable("awakened.hud.ability_hud_empty");
         } else if (state.flashTicks() > 0) {
            return state.failMessage();
         } else {
            Ability ability = power.ability(state.activeSlot());
            if (ability == null) {
               return power.displayName();
            } else if ((Boolean)ClientConfig.HUD_ABILITY_NUMBER_ONLY.get()) {
               return Component.translatable("awakened.hud.slot_short", new Object[]{state.activeSlot() + 1, power.abilityCount()});
            } else {
               Component own = ability.displayName(Minecraft.getInstance().player);
               Component name = (Component)(state.masteryLevel() < ability.unlockLevel()
                  ? Component.translatable("awakened.hud.locked", new Object[]{own})
                  : own);
               return Component.translatable("awakened.hud.slot", new Object[]{state.activeSlot() + 1, power.abilityCount(), name});
            }
         }
      }

      private static int headlineColor(ClientPowerState state, Power power, int theme) {
         if (state.flashTicks() > 0) {
            return -38037;
         } else {
            Ability ability = power.ability(state.activeSlot());
            if (ability == null) {
               return theme;
            } else if (state.masteryLevel() < ability.unlockLevel()) {
               return -3377289;
            } else {
               boolean affordable = StaminaHud.affordable(state, power, ability);
               return affordable ? theme : -43691;
            }
         }
      }
   }

   public static final class AbilityIcon implements StaminaHud.Part {
      private AbilityIcon() {
      }

      @Override
      public String hudId() {
         return "ability_icon";
      }

      @Override
      public HudSettings hudSettings() {
         return ClientConfig.HUD_ABILITY_ICON;
      }

      @Override
      public boolean hudRelevant() {
         return (Boolean)ClientConfig.HUD_ICONS_OFF.get();
      }

      @Override
      public boolean shows(ClientPowerState state, Power power) {
         return (Boolean)ClientConfig.HUD_ICONS_OFF.get() && power.ability(state.activeSlot()) != null;
      }

      @Override
      public HudPlacement.Box defaultBox(int screenW, int screenH, int w, int h) {
         return new HudPlacement.Box(screenW / 2 + 91 + 5, screenH - 22 + 3, w, h);
      }

      @Override
      public int hudWidth() {
         return 16;
      }

      @Override
      public int hudHeight() {
         return 16;
      }

      @Override
      public void draw(GuiGraphics g, HudPlacement.Box box, ClientPowerState state, Power power) {
         Ability ability = power.ability(state.activeSlot());
         if (ability != null) {
            boolean locked = state.masteryLevel() < ability.unlockLevel();
            boolean affordable = StaminaHud.affordable(state, power, ability);
            boolean lit = !locked && power.abilityLitOnClient(Minecraft.getInstance().player, state.activeSlot());
            int iconX = box.x();
            int iconY = box.y();
            drawTile(g, iconX, iconY);
            g.blit(AbilityIcons.of(StaminaHud.iconId(ability)), iconX, iconY, 0.0F, 0.0F, 16, 16, 16, 16);
            if (locked) {
               g.fill(iconX, iconY, iconX + 16, iconY + 16, -1342177280);
            } else if (!StaminaHud.drawCooldown(g, StaminaHud.font(), state, state.activeSlot(), iconX, iconY) && !affordable) {
               g.fill(iconX, iconY, iconX + 16, iconY + 16, 1624256560);
            }

            if (lit) {
               StaminaHud.AbilityBar.drawLit(g, iconX, iconY, power.themeColor());
            }
         }
      }

      @Override
      public void drawStandIn(GuiGraphics g, HudPlacement.Box box) {
         drawTile(g, box.x(), box.y());
      }

      private static void drawTile(GuiGraphics g, int iconX, int iconY) {
         g.fill(iconX - 2, iconY - 2, iconX + 16 + 2, iconY + 16 + 2, -14671840);
         g.fill(iconX - 1, iconY - 1, iconX + 16 + 1, iconY + 16 + 1, -871494130);
      }
   }

   private interface Part extends FreeHud {
      boolean shows(ClientPowerState var1, Power var2);

      void draw(GuiGraphics var1, HudPlacement.Box var2, ClientPowerState var3, Power var4);

      void drawStandIn(GuiGraphics var1, HudPlacement.Box var2);

      @Override
      default void drawPreview(GuiGraphics g, HudPlacement.Box box) {
         ClientPowerState state = ClientPowerState.get();
         Power power = StaminaHud.localPower();
         if (power != null && this.shows(state, power)) {
            this.draw(g, box, state, power);
         } else {
            this.drawStandIn(g, box);
         }
      }
   }

   public static final class ResourceGauge implements StaminaHud.Part {
      private static final int OVERSHOOT_ROOM = fillHeight(2.3F) - 60;
      private static final int COLOR_RESERVED = 872415231;
      private static final int HATCH_PITCH = 3;
      private static final int HATCH_DASH = 2;

      private ResourceGauge() {
      }

      @Override
      public String hudId() {
         return "resource_gauge";
      }

      @Override
      public HudSettings hudSettings() {
         return ClientConfig.HUD_RESOURCE_GAUGE;
      }

      @Override
      public boolean hudRelevant() {
         return gauge(StaminaHud.localPower());
      }

      @Override
      public boolean shows(ClientPowerState state, Power power) {
         return gauge(power);
      }

      @Override
      public HudPlacement.Box defaultBox(int screenW, int screenH, int w, int h) {
         return new HudPlacement.Box(screenW - 2 - w, screenH / 2 + 50 - 60 - OVERSHOOT_ROOM, w, h);
      }

      @Override
      public int hudWidth() {
         return Math.max(21, GuiPanels.roomForCentred(18, StaminaHud.font().width(caption(StaminaHud.localPower()))));
      }

      @Override
      public int hudHeight() {
         return OVERSHOOT_ROOM + 60 + below();
      }

      @Override
      public void draw(GuiGraphics g, HudPlacement.Box box, ClientPowerState state, Power power) {
         Font font = StaminaHud.font();
         int x0 = barLeft(box);
         int x1 = x0 + 7;
         int centre = x0 + 3;
         int bottomY = footY(box);
         int trackTop = bottomY - 60;
         float raw = Math.max(0.0F, state.resourceFraction());
         int color = StaminaHud.barColor(power, raw);
         drawTrack(g, box, bottomY);
         int fillH = fillHeight(raw);
         int top = bottomY - fillH;
         if (fillH > 0) {
            g.fill(x0, top, x1, bottomY, color);
         }

         g.fill(x0 - 2, trackTop - 1, x1 + 2, trackTop, -1);
         float mark = Math.max(0.0F, state.resourceMark());
         if (power.resourceBarMarkTrails()) {
            float[] trail = StaminaHud.StaminaBar.trackSpend(state, Minecraft.getInstance().player, mark);
            int trailH = Math.min(top - trackTop, Math.round(60.0F * trail[0]));
            if (trail[1] > 0.0F && trailH > 0) {
               int alpha = Math.round(130.0F * trail[1]);
               g.fill(x0, top - trailH, x1, top, alpha << 24 | color & 16777215);
            }
         } else {
            int markH = Math.round(60.0F * Math.min(mark, Math.min(2.3F, raw)));
            if (markH > 0 && markH < fillH) {
               int divY = top + markH;
               g.fill(x0, divY, x1, divY + 1, -2880);
            }
         }

         float ceiling = StaminaHud.barCeiling(power, state);
         if (ceiling < 1.0F) {
            int cy0 = bottomY - Math.round(60.0F * ceiling);
            g.fill(x0, trackTop, x1, cy0, -1441787888);
            int over = raw > ceiling ? StaminaHud.barOverColor(power, raw, ceiling) : 0;
            if (over != 0 && top < cy0) {
               g.fill(x0, top, x1, cy0, over);
            }

            int ny = Math.max(trackTop + 1, Math.min(cy0, bottomY - 1));
            g.fill(x0 - 2, ny - 1, x1 + 2, ny + 1, -1);
         }

         drawCaption(g, box, bottomY, caption(power), color);
         Component badge = badge(power, raw);
         if (badge != null) {
            ItemStack icon = badgeIcon(power);
            int gap = icon.isEmpty() ? 0 : 11;
            int total = gap + font.width(badge);
            int sx = Math.min(centre - total / 2, box.right() - total);
            int by = bottomY + 4 + 9 + 3;
            if (!icon.isEmpty()) {
               g.pose().pushPose();
               g.pose().translate((float)sx, (float)by - 1.0F, 0.0F);
               g.pose().scale(0.625F, 0.625F, 1.0F);
               g.renderItem(icon, 0, 0);
               g.pose().popPose();
            }

            g.drawString(font, badge, sx + gap, by, color);
         }
      }

      @Override
      public void drawStandIn(GuiGraphics g, HudPlacement.Box box) {
         int bottomY = footY(box);
         drawTrack(g, box, bottomY);
         drawCaption(g, box, bottomY, caption(StaminaHud.localPower()), -4210753);
      }

      @Override
      public void drawPreview(GuiGraphics g, HudPlacement.Box box) {
         drawReservedRoom(g, box);
         StaminaHud.Part.super.drawPreview(g, box);
      }

      private static int barLeft(HudPlacement.Box box) {
         return box.right() - 14 - 7;
      }

      private static int footY(HudPlacement.Box box) {
         return box.y() + OVERSHOOT_ROOM + 60;
      }

      private static void drawTrack(GuiGraphics g, HudPlacement.Box box, int foot) {
         int x0 = barLeft(box);
         StaminaHud.track(g, x0, foot - 60, x0 + 7, foot);
      }

      private static void drawReservedRoom(GuiGraphics g, HudPlacement.Box box) {
         int x0 = barLeft(box);
         int x1 = x0 + 7;
         int top = box.y();
         int bottom = footY(box) - 60;
         int y = top;

         for (int row = 0; y < bottom; row++) {
            for (int x = x0 + (row % 2 == 0 ? 0 : 2); x < x1; x += 4) {
               g.fill(x, y, Math.min(x + 2, x1), y + 1, 872415231);
            }

            y += 3;
         }
      }

      private static void drawCaption(GuiGraphics g, HudPlacement.Box box, int foot, Component label, int color) {
         Font font = StaminaHud.font();
         g.drawString(font, label, GuiPanels.centredWithin(barLeft(box) + 3, font.width(label), box.x(), box.right()), foot + 4, color);
      }

      private static int below() {
         Font font = StaminaHud.font();
         return 4 + 9 + 3 + 9;
      }

      private static int fillHeight(float raw) {
         return Math.round(60.0F * Math.min(2.3F, raw));
      }

      private static boolean gauge(@Nullable Power power) {
         return power != null && power.hasResourceBar() && power.resourceBarVertical();
      }

      private static Component caption(@Nullable Power power) {
         return (Component)(power != null && power.hasResourceBar() ? power.resourceBarLabel() : Component.translatable("awakened.hud.resource_gauge_empty"));
      }

      @Nullable
      private static Component badge(Power power, float raw) {
         Player viewer = Minecraft.getInstance().player;
         return viewer == null ? null : power.resourceBarBadge(viewer, raw);
      }

      private static ItemStack badgeIcon(Power power) {
         Player viewer = Minecraft.getInstance().player;
         return viewer == null ? ItemStack.EMPTY : power.resourceBarBadgeIcon(viewer);
      }
   }

   public static final class StaminaBar implements StaminaHud.Part {
      private static final int RESOURCE_GAP = 3;
      private static final int RESOURCE_H = 4;
      private static final int RESOURCE_LABEL_LIFT = 2;
      private static final int RESOURCE_LABEL_GAP = 4;
      private static final int RESOURCE_TIER_GAP = 5;
      private static final int LOCK_HATCH_ALPHA = 1711276032;
      private static final int COLOR_LOCK_EDGE = -6645094;
      private static final int LOCK_HATCH_DASH = 2;
      private static final int LOCK_HATCH_PERIOD = 4;
      private static final int LOCK_HATCH_TICKS = 5;
      private static final int LOCK_BRACKET_MIN = 8;
      private static final float LOCK_LABEL_SCALE = 0.5F;
      private static int hatchPhase;
      private static int hatchLastTick = -1;
      private static final float BAR_LINE_SCALE = 0.75F;
      private static final int BAR_LINE_H = 8;
      private static final int BAR_LINE_GAP = 3;
      private static final int DRAIN_GAP = 4;
      private static final int DRAIN_LIFT = 1;
      private static final float DRAIN_LIGHTEN = 0.45F;
      private static final float DRAIN_DIM = 0.55F;
      private static final int DEPOSIT_FLASH_TICKS = 8;
      private static final int DEPOSIT_FLASH_ALPHA = 210;
      private static final float DEPOSIT_FLASH_LIGHTEN = 0.75F;
      private static final float DEPOSIT_EPS = 1.0E-4F;
      private static String depositPowerId = "";
      private static float depositLast = -1.0F;
      private static float depositFrom;
      private static float depositTo;
      private static int depositAt = Integer.MIN_VALUE;
      private static WeakReference<Player> depositViewer = new WeakReference<>(null);
      private static final int HATCH_STEP = 3;
      private static final int SPEND_TRAIL_FADE_TICKS = 10;
      private static final int SPEND_TRAIL_ALPHA = 130;
      private static final float SPEND_EPS = 1.0E-4F;
      private static float spendLive;
      private static float spendFading;
      private static int spendEndedAt = Integer.MIN_VALUE;
      private static WeakReference<Player> spendViewer = new WeakReference<>(null);
      private static String spendPowerId = "";
      private static final float TICK_LIGHTEN = 0.45F;
      private static final int COLOR_TICK_EMPTY = -12829636;
      private static final int COLOR_LOCKED_WASH = -1341124592;
      private static final int COLOR_LOCKED_EDGE = -6645094;
      private static final int COLOR_ORIGIN = -1513240;
      private static final int COLOR_LOCKED_HATCH = 1721802912;
      private static final int COLOR_HOLD_RESET = -419430401;

      private StaminaBar() {
      }

      @Override
      public String hudId() {
         return "stamina_hud";
      }

      @Override
      public HudSettings hudSettings() {
         return ClientConfig.HUD_STAMINA;
      }

      @Override
      public String hudGroup() {
         return "power_hud";
      }

      @Override
      public boolean hudRelevant() {
         return StaminaHud.localPower() != null;
      }

      @Override
      public boolean shows(ClientPowerState state, Power power) {
         return true;
      }

      @Override
      public HudPlacement.Box defaultBox(int screenW, int screenH, int w, int h) {
         return new HudPlacement.Box(StaminaHud.shippedBarX(screenW) + 91 - this.leftPad(), StaminaHud.shippedBarY(screenH), w, h);
      }

      @Override
      public int hudWidth() {
         return this.leftPad() + this.rightPad();
      }

      @Override
      public int hudHeight() {
         Power power = StaminaHud.localPower();
         if (primaryResource(power)) {
            return Math.max(6, 9 - 2);
         } else {
            return flatBar(power) ? 9 + Math.max(4, 9 - 2) : 6;
         }
      }

      @Override
      public void draw(GuiGraphics g, HudPlacement.Box box, ClientPowerState state, Power power) {
         this.draw(g, box, state, power, barShown(state, power));
      }

      private void draw(GuiGraphics g, HudPlacement.Box box, ClientPowerState state, Power power, boolean showBar) {
         int centre = this.centreX(box);
         int barY = box.y();
         int barX = centre - 91;
         if (showBar) {
            Player viewer = Minecraft.getInstance().player;
            String drain = power.staminaDrainLabel(viewer);
            boolean armed = drain != null && power.staminaDrainArmed(viewer);
            drawBar(g, state, barX, barY, power.themeColor(), armed);
            if (drain != null) {
               drawDrain(g, StaminaHud.font(), drain, barX, barY, power.staminaDrainColor(viewer), armed);
            }

            drawBarLines(g, StaminaHud.font(), power.staminaBarLines(viewer), barX, barY, power.themeColor());
         }

         if (flatBar(power)) {
            boolean primary = primaryResource(power);
            drawResourceBar(g, StaminaHud.font(), state, power, barX, primary ? barY : barY + 6 + 3, primary);
         }
      }

      @Override
      public void drawPreview(GuiGraphics g, HudPlacement.Box box) {
         ClientPowerState state = ClientPowerState.get();
         Power power = StaminaHud.localPower();
         if (power != null) {
            this.draw(g, box, state, power, power.resource().showsStaminaBar());
         } else {
            this.drawStandIn(g, box);
         }
      }

      @Override
      public void drawStandIn(GuiGraphics g, HudPlacement.Box box) {
         drawBar(g, ClientPowerState.get(), this.centreX(box) - 91, box.y(), -4210753, false);
      }

      private int centreX(HudPlacement.Box box) {
         return box.x() + this.leftPad();
      }

      private int leftPad() {
         Power power = StaminaHud.localPower();
         int pad = 91;
         if (flatBar(power)) {
            pad = Math.max(pad, 95 + StaminaHud.font().width(power.resourceBarLabel()));
         }

         return pad;
      }

      private int rightPad() {
         Power power = StaminaHud.localPower();
         int pad = 91;
         if (flatBar(power)) {
            pad = Math.max(pad, 96 + tierRoom(power));
         }

         return pad;
      }

      private static int tierRoom(Power power) {
         return Math.round((float)StaminaHud.font().width(power.resourceBarTierRoom()) * power.resourceBarTierScale());
      }

      private static boolean barShown(ClientPowerState state, @Nullable Power power) {
         if (power != null && !power.resource().showsStaminaBar()) {
            return false;
         } else {
            boolean full = state.staminaMax() > 0.0F && state.stamina() >= state.staminaMax();
            return !(Boolean)ClientConfig.HUD_HIDE_STAMINA_WHEN_FULL.get() || !full || state.flashTicks() > 0;
         }
      }

      private static boolean flatBar(@Nullable Power power) {
         return power != null && power.hasResourceBar() && !power.resourceBarVertical();
      }

      private static boolean primaryResource(@Nullable Power power) {
         return flatBar(power) && !power.resource().showsStaminaBar();
      }

      @Nullable
      private static String tierLabel(ClientPowerState state, Power power) {
         return power.resourceBarTierLabel(state.resourceTier());
      }

      private static void drawBar(GuiGraphics g, ClientPowerState state, int x, int y, int theme, boolean draining) {
         StaminaHud.track(g, x, y, x + 182, y + 6);
         float max = state.staminaMax();
         float fraction = max > 0.0F ? state.stamina() / max : 0.0F;
         fraction = Math.max(0.0F, Math.min(1.0F, fraction));
         int fillWidth = Math.round(182.0F * fraction);
         int fillColor = state.flashTicks() > 0 ? -43691 : theme;
         if (draining && state.flashTicks() <= 0 && Power.hudBlinkOn()) {
            fillColor = GuiPanels.lighten(theme, 0.45F);
         }

         if (fillWidth > 0) {
            g.fill(x, y, x + fillWidth, y + 6, fillColor);
         }

         float bonus = state.staminaBonus();
         if (bonus > 0.5F && max > 0.0F && state.flashTicks() <= 0) {
            int baseWidth = Math.round(182.0F * Math.max(0.0F, (max - bonus) / max));
            if (fillWidth > baseWidth) {
               g.fill(x + baseWidth, y, x + fillWidth, y + 6, -6528);
            }

            g.fill(x + baseWidth, y - 1, x + baseWidth + 1, y + 6 + 1, -2880);
         }

         drawLockedSpan(g, state, x, y, theme, max);
      }

      private static int hatchOffset(@Nullable Player viewer, boolean live) {
         int tick = viewer != null ? viewer.tickCount : 0;
         if (tick != hatchLastTick) {
            if (live) {
               hatchPhase++;
            }

            hatchLastTick = tick;
         }

         return hatchPhase / 5 % 4;
      }

      private static void drawLockedSpan(GuiGraphics g, ClientPowerState state, int x, int y, int theme, float max) {
         float locked = Math.min(max, Math.max(0.0F, state.staminaLocked()));
         if (!(locked <= 0.5F) && !(max <= 0.0F)) {
            Player viewer = Minecraft.getInstance().player;
            Power power = StaminaHud.localPower();
            boolean live = power != null && viewer != null && power.lockedStaminaLive(viewer);
            int x0 = x + Math.round(182.0F * ((max - locked) / max));
            int x1 = x + 182;
            if (x1 - x0 >= 1) {
               int hatch = theme & 16777215 | 1711276032;
               int offset = hatchOffset(viewer, live);

               for (int row = 0; row < 6; row++) {
                  int start = x0 - 4 + ((row % 2 == 0 ? 0 : 2) + offset) % 4;

                  for (int px = start; px < x1; px += 4) {
                     int from = Math.max(x0, px);
                     int to = Math.min(px + 2, x1);
                     if (to > from) {
                        g.fill(from, y + row, to, y + row + 1, hatch);
                     }
                  }
               }

               g.fill(x0, y - 1, x0 + 1, y + 6 + 1, -6645094);
               if (x1 - x0 >= 8) {
                  Font font = StaminaHud.font();
                  g.drawString(font, "[", x0 + 1, y - 1, -6645094, false);
                  g.drawString(font, "]", x1 - font.width("]"), y - 1, -6645094, false);
                  float returnLeft = (float)x1;
                  float inside = (float)x0;
                  float returnRight = (float)x1;

                  for (String candidate : power != null && viewer != null ? power.lockedStaminaReturnLabels(viewer) : List.of()) {
                     float width = (float)font.width(candidate);
                     if (!(returnRight - width < inside)) {
                        returnLeft = returnRight - width;
                        g.drawString(font, candidate, Math.round(returnLeft), y + 6 + 2, theme & 16777215 | 0xFF000000, false);
                        returnLeft = (float)x1;
                        break;
                     }
                  }

                  String label = power != null && viewer != null ? power.lockedStaminaLabel(viewer) : "";
                  if (!label.isEmpty()) {
                     float width = (float)font.width(label) * 0.5F;
                     float left = (float)(x0 + x1) / 2.0F - width / 2.0F;
                     if (left + width + 2.0F <= returnLeft) {
                        HudText.scaled(g, font, label, left, (float)y + 1.0F, 0.5F, 0, 0, -6645094, false);
                     }
                  }
               }
            }
         }
      }

      static int barLinesHeight(int lines) {
         return 3 + lines * 8;
      }

      private static void drawBarLines(GuiGraphics g, Font font, List<Component> lines, int x, int y, int theme) {
         if (!lines.isEmpty()) {
            float top = (float)(y - 3 - lines.size() * 8);

            for (int i = 0; i < lines.size(); i++) {
               HudText.scaled(g, font, lines.get(i).getString(), (float)x, top + (float)(i * 8), 0.75F, 0, 0, theme, true);
            }
         }
      }

      private static void drawDrain(GuiGraphics g, Font font, String drain, int x, int y, int theme, boolean armed) {
         int color = armed ? (Power.hudBlinkOn() ? GuiPanels.lighten(theme, 0.45F) : theme) : GuiPanels.darken(theme, 0.55F);
         g.drawString(font, drain, x + 182 + 4, y - 1, color);
      }

      private static void drawResourceBar(GuiGraphics g, Font font, ClientPowerState state, Power power, int x, int y, boolean primary) {
         int h = primary ? 6 : 4;
         StaminaHud.track(g, x, y, x + 182, y + h);
         float raw = Math.max(0.0F, state.resourceFraction());
         Player viewer = Minecraft.getInstance().player;
         float origin = Math.min(1.0F, Math.max(0.0F, power.resourceBarOrigin()));
         boolean twoSided = origin > 0.0F;
         trackDeposit(state, viewer, raw, twoSided);
         int color = StaminaHud.barColor(power, raw);
         if (viewer != null && power.resourceBarFlashing(viewer) && Power.hudBlinkOn()) {
            color = GuiPanels.lighten(color, 0.5F);
         }

         if (primary && state.flashTicks() > 0) {
            color = -43691;
         }

         int fillWidth = Math.round(182.0F * Math.min(1.0F, raw));
         int originPx = Math.round(182.0F * origin);
         int fill0 = twoSided ? Math.min(originPx, fillWidth) : 0;
         int fill1 = twoSided ? Math.max(originPx, fillWidth) : fillWidth;
         if (fill1 > fill0) {
            g.fill(x + fill0, y, x + fill1, y + h, color);
         }

         int mastery = state.masteryLevel();
         float locked = Math.min(1.0F, Math.max(0.0F, power.resourceBarLockedFrom(viewer, mastery)));
         int lockedX = x + Math.round(182.0F * locked);
         if (lockedX < x + 182) {
            g.fill(lockedX, y, x + 182, y + h, -1341124592);
         }

         float lockedLow = Math.min(1.0F, Math.max(0.0F, power.resourceBarLockedBelow(viewer, mastery)));
         int lockedLowX = x + Math.round(182.0F * lockedLow);
         if (lockedLowX > x) {
            g.fill(x, y, lockedLowX, y + h, -1341124592);
         }

         if (twoSided) {
            hatch(g, x, lockedLowX, y, h);
            hatch(g, lockedX, x + 182, y, h);
         }

         for (float t : power.resourceBarTicks()) {
            if (!(t <= 0.0F) && !(t >= 1.0F)) {
               int tx = x + Math.round(182.0F * t);
               boolean reached = tx >= x + fill0 && tx <= x + fill1;
               g.fill(tx, y, tx + 1, y + h, reached ? GuiPanels.lighten(color, 0.45F) : -12829636);
            }
         }

         if (twoSided) {
            g.fill(x + originPx, y - 1, x + originPx + 1, y + h + 1, -1513240);
         }

         if (lockedX < x + 182) {
            g.fill(lockedX, y, lockedX + 1, y + h, -6645094);
         }

         if (lockedLowX > x) {
            g.fill(lockedLowX - 1, y, lockedLowX, y + h, -6645094);
         }

         if (raw > 1.0F) {
            float over = Math.min(1.0F, raw - 1.0F);
            int overWidth = Math.round(182.0F * over);
            if (overWidth > 0) {
               g.fill(x, y, x + overWidth, y + h, -3392);
            }
         }

         float mark = Math.max(0.0F, state.resourceMark());
         if (power.resourceBarMarkTrails()) {
            drawSpendTrail(g, x, y, h, fillWidth, color, trackSpend(state, viewer, mark));
         } else {
            int markWidth = Math.round(182.0F * Math.min(mark, Math.min(1.0F, raw)));
            if (markWidth > 0) {
               int mx0 = Math.max(x, x + fillWidth - markWidth);
               g.fill(mx0, y, x + fillWidth, y + h, -6528);
            }
         }

         drawDeposit(g, x, y, h, color);
         if (twoSided && power.resourceBarHoldResets()) {
            drawHoldReset(g, x, y, h, fillWidth, originPx);
         }

         float ceiling = StaminaHud.barCeiling(power, state);
         if (ceiling < 1.0F) {
            int cx0 = x + Math.round(182.0F * ceiling);
            g.fill(cx0, y, x + 182, y + h, -1441787888);
            int over = raw > ceiling ? StaminaHud.barOverColor(power, raw, ceiling) : 0;
            if (over != 0 && x + fillWidth > cx0) {
               g.fill(cx0, y, x + fillWidth, y + h, over);
            }

            int nx = Math.max(x + 1, Math.min(cx0, x + 182 - 1));
            g.fill(nx - 1, y - 2, nx + 1, y + h + 2, -1);
         }

         int cooldownSeconds = state.resourceCooldownSeconds();
         if (cooldownSeconds > 0) {
            float phase = (float)(Minecraft.getInstance().player.tickCount % 60) / 60.0F;
            int alpha = 40 + Math.round(35.0F * (0.5F + 0.5F * Mth.sin(phase * (float) (Math.PI * 2))));
            g.fill(x, y, x + 182, y + h, alpha << 24 | 8376319);
         }

         int textY = y - 2;
         Component label = power.resourceBarLabel();
         g.drawString(font, label, x - 4 - font.width(label), textY, color);
         String tier = tierLabel(state, power);
         int rightX = x + 182 + 5;
         if (tier != null) {
            float scale = power.resourceBarTierScale();
            int tierColor = viewer != null ? power.resourceBarTierColor(viewer, state.resourceTier()) : color;
            if (scale == 1.0F) {
               g.drawString(font, tier, rightX, textY, tierColor);
            } else {
               int lift = Math.round(9.0F * (scale - 1.0F) / 2.0F);
               HudText.scaled(g, font, tier, (float)rightX, (float)(textY - lift), scale, 0, 0, tierColor, true);
            }

            rightX += Math.round((float)font.width(tier) * power.resourceBarTierScale()) + 5;
         }

         Component badge = viewer == null ? null : power.resourceBarBadge(viewer, raw);
         if (badge != null) {
            g.drawString(font, badge, rightX, y - 1, color);
         }

         if (cooldownSeconds > 0) {
            g.drawString(font, Component.translatable("awakened.hud.cooldown_short", new Object[]{cooldownSeconds}), rightX, textY, -8400897);
         }
      }

      private static void trackDeposit(ClientPowerState state, @Nullable Player viewer, float raw, boolean twoSided) {
         if (viewer != null) {
            if (depositViewer.get() != viewer) {
               depositViewer = new WeakReference<>(viewer);
               depositPowerId = state.powerId();
               depositLast = raw;
               depositAt = Integer.MIN_VALUE;
            } else if (!depositPowerId.equals(state.powerId())) {
               depositPowerId = state.powerId();
               depositLast = raw;
               depositAt = Integer.MIN_VALUE;
            } else if (depositLast < 0.0F) {
               depositLast = raw;
            } else {
               if (raw > depositLast + 1.0E-4F) {
                  depositFrom = !twoSided && depositFade(viewer) > 0.0F ? Math.min(depositFrom, depositLast) : depositLast;
                  depositTo = raw;
                  depositAt = viewer.tickCount;
               } else if (twoSided && raw < depositLast - 1.0E-4F) {
                  depositFrom = depositLast;
                  depositTo = raw;
                  depositAt = viewer.tickCount;
               }

               depositLast = raw;
            }
         }
      }

      private static float depositFade(@Nullable Player viewer) {
         if (viewer != null && depositAt != Integer.MIN_VALUE) {
            int age = viewer.tickCount - depositAt;
            return age >= 0 && age < 8 ? 1.0F - (float)age / 8.0F : 0.0F;
         } else {
            return 0.0F;
         }
      }

      private static void drawDeposit(GuiGraphics g, int x, int y, int h, int color) {
         float fade = depositFade(Minecraft.getInstance().player);
         if (!(fade <= 0.0F)) {
            float lo = Math.min(depositFrom, depositTo);
            float hi = Math.max(depositFrom, depositTo);
            int x0 = x + Math.round(182.0F * Math.max(0.0F, Math.min(1.0F, lo)));
            int x1 = x + Math.round(182.0F * Math.max(0.0F, Math.min(1.0F, hi)));
            if (x1 > x0) {
               int alpha = Math.round(210.0F * fade);
               g.fill(x0, y, x1, y + h, alpha << 24 | GuiPanels.lighten(color, 0.75F) & 16777215);
            }
         }
      }

      private static void drawHoldReset(GuiGraphics g, int x, int y, int h, int fillWidth, int originPx) {
         float p = ClientInputHandler.secondaryHoldProgress();
         if (!(p <= 0.0F) && fillWidth != originPx) {
            int head = fillWidth + Math.round((float)(originPx - fillWidth) * p);
            int a = Math.min(fillWidth, head);
            int b = Math.max(fillWidth, head);
            g.fill(x + a, y - 1, x + b + 1, y, -419430401);
            g.fill(x + a, y + h, x + b + 1, y + h + 1, -419430401);
         }
      }

      private static void hatch(GuiGraphics g, int x0, int x1, int y, int h) {
         for (int r = 0; r < h; r++) {
            for (int px = x0 + (2 - r % 3); px < x1; px += 3) {
               g.fill(px, y + r, px + 1, y + r + 1, 1721802912);
            }
         }
      }

      private static float[] trackSpend(ClientPowerState state, @Nullable Player viewer, float mark) {
         if (viewer == null) {
            return new float[]{0.0F, 0.0F};
         } else {
            if (spendViewer.get() != viewer || !spendPowerId.equals(state.powerId())) {
               spendViewer = new WeakReference<>(viewer);
               spendPowerId = state.powerId();
               spendLive = 0.0F;
               spendEndedAt = Integer.MIN_VALUE;
            }

            if (mark > 1.0E-4F) {
               spendLive = mark;
               spendEndedAt = Integer.MIN_VALUE;
               return new float[]{mark, 1.0F};
            } else {
               if (spendLive > 1.0E-4F) {
                  spendFading = spendLive;
                  spendEndedAt = viewer.tickCount;
                  spendLive = 0.0F;
               }

               if (spendEndedAt == Integer.MIN_VALUE) {
                  return new float[]{0.0F, 0.0F};
               } else {
                  int age = viewer.tickCount - spendEndedAt;
                  float fade = age >= 0 && age < 10 ? 1.0F - (float)age / 10.0F : 0.0F;
                  return new float[]{spendFading, fade};
               }
            }
         }
      }

      private static void drawSpendTrail(GuiGraphics g, int x, int y, int h, int fillWidth, int color, float[] trail) {
         if (!(trail[1] <= 0.0F)) {
            int x0 = x + fillWidth;
            int x1 = x + Math.min(182, fillWidth + Math.round(182.0F * trail[0]));
            if (x1 > x0) {
               int alpha = Math.round(130.0F * trail[1]);
               g.fill(x0, y, x1, y + h, alpha << 24 | color & 16777215);
            }
         }
      }
   }
}
