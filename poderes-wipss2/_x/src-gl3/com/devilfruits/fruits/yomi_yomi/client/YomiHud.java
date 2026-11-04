package com.devilfruits.fruits.yomi_yomi.client;

import com.devilfruits.fruits.yomi_yomi.SecondWind;
import com.devilfruits.fruits.yomi_yomi.Vessel;
import com.devilfruits.fruits.yomi_yomi.YomiYomi;
import com.devilfruits.fruits.yomi_yomi.client.vfx.SoulTetherCinematic;
import com.devilfruits.fruits.yomi_yomi.entity.SoulShell;
import com.devilfruits.fruits.yomi_yomi.soul.SoulProjection;
import com.devilfruits.fruits.yomi_yomi.soul.YomiSoul;
import com.devilfruits.registry.DFAttachments;
import java.util.Locale;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class YomiHud {
   private static float dimAlpha;
   private static long dimStart;
   private static int dimFade;
   private static final String[] ARROWS = new String[]{"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};

   static void dim(float alpha, int fade) {
      ClientLevel level = Minecraft.getInstance().level;
      dimAlpha = alpha;
      dimFade = fade;
      dimStart = level == null ? 0L : level.getGameTime();
   }

   private static float dimNow(float partial) {
      ClientLevel level = Minecraft.getInstance().level;
      if (dimAlpha <= 0.0F || level == null) {
         return 0.0F;
      } else if (dimFade == 0) {
         return dimAlpha;
      } else {
         float t = (float)level.getGameTime() + partial - (float)dimStart - 15.0F;
         float a = t <= 0.0F ? dimAlpha : dimAlpha * (1.0F - t / (float)dimFade);
         if (a <= 0.0F) {
            dimAlpha = 0.0F;
         }

         return Math.max(0.0F, a);
      }
   }

   static void render(GuiGraphics g, DeltaTracker delta) {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      float dim = dimNow(delta.getGameTimeDeltaPartialTick(false));
      if (dim > 0.001F) {
         g.fill(0, 0, g.guiWidth(), g.guiHeight(), Math.round(dim * 255.0F) << 24);
      }

      SoulKingClient.renderStun(g, delta);
      BladeClient.hint(g, delta);
      projection(g, delta);
      secondWind(g);
      if (player != null && !mc.options.hideGui && YomiYomi.isUser(player)) {
         YomiSoul soul = YomiSoul.of(player);
         if (soul.active() && !soul.pending()) {
            int seconds = Math.max(0, soul.ticksLeft() / 20);
            String time = String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
            int color = seconds <= 20 ? 16738906 : 13172586;
            int cx = g.guiWidth() / 2;
            g.drawCenteredString(mc.font, Component.translatable("hud.devilfruits.yomi_yomi.soul_time", new Object[]{time}), cx, 22, color);
            Vec3 to = soul.body().subtract(player.position());
            if (player.level().dimension() != soul.dimension()) {
               g.drawCenteredString(mc.font, Component.translatable("hud.devilfruits.yomi_yomi.body_elsewhere"), cx, 34, 14221248);
            } else {
               double bearing = Math.toDegrees(Math.atan2(-to.x, to.z)) - (double)player.getYRot();
               String arrow = ARROWS[Math.floorMod(Math.round((float)Mth.wrapDegrees(bearing) / 45.0F), 8)];
               g.drawCenteredString(
                  mc.font,
                  Component.translatable("hud.devilfruits.yomi_yomi.body", new Object[]{arrow, (int)to.length(), to.y < -2.0 ? "↓" : (to.y > 2.0 ? "↑" : "")}),
                  cx,
                  34,
                  14221248
               );
            }
         }
      }
   }

   private static void projection(GuiGraphics g, DeltaTracker delta) {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      if (player != null && mc.level != null && !mc.options.hideGui && SoulProjection.active(player)) {
         SoulProjection.Anchor anchor = (SoulProjection.Anchor)player.getData(SoulProjection.ANCHOR);
         Vec3 home = anchor.active() ? anchor.body() : null;

         for (Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof SoulShell s && player.getUUID().equals(s.ownerId())) {
               home = s.position();
               break;
            }
         }

         if (home != null) {
            Vec3 to = home.subtract(player.position());
            double d = to.length();
            double leash = SoulTetherCinematic.leash();
            double bearing = Math.toDegrees(Math.atan2(-to.x, to.z)) - (double)player.getYRot();
            String arrow = ARROWS[Math.floorMod(Math.round((float)Mth.wrapDegrees(bearing) / 45.0F), 8)];
            int color = d > leash * 0.85 ? 15007628 : 13172586;
            g.drawCenteredString(
               mc.font,
               Component.translatable(
                  "hud.devilfruits.yomi_yomi.projection", new Object[]{arrow, (int)d, (int)leash, to.y < -2.0 ? "↓" : (to.y > 2.0 ? "↑" : "")}
               ),
               g.guiWidth() / 2,
               22,
               color
            );
         }
      }
   }

   private static void secondWind(GuiGraphics g) {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      if (player != null && mc.level != null && !mc.options.hideGui && YomiYomi.vessel(player) == Vessel.SKELETON) {
         int ticks = DFAttachments.data(player).cooldownTicksLeft(SecondWind.COOLDOWN, mc.level.getGameTime());
         Component text = ticks <= 0
            ? Component.translatable("hud.devilfruits.yomi_yomi.second_wind_ready")
            : Component.translatable(
               "hud.devilfruits.yomi_yomi.second_wind", new Object[]{String.format(Locale.ROOT, "%d:%02d", ticks / 1200, ticks / 20 % 60)}
            );
         int x = g.guiWidth() / 2 - 91 - 8 - mc.font.width(text);
         g.drawString(mc.font, text, x, g.guiHeight() - 14, ticks <= 0 ? 8191802 : 10135200);
      }
   }

   private YomiHud() {
   }
}
