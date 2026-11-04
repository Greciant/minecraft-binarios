package com.devilfruits.fruits.suke_suke.client;

import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.rules.Mastery;
import com.devilfruits.fruits.suke_suke.SukeAbilities;
import com.devilfruits.fruits.suke_suke.SukeSuke;
import com.devilfruits.fruits.suke_suke.clear.Clear;
import com.devilfruits.fruits.suke_suke.clear.Cloak;
import com.devilfruits.fruits.suke_suke.clear.ShipView;
import com.devilfruits.registry.DFAttachments;
import java.util.Locale;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

public final class SukeHud {
   private static final int TEAL = 12578784;
   private static final int AQUA = 8378568;
   private static final int LIME = 14546376;

   static void init(IEventBus modBus) {
      modBus.addListener(e -> {
         e.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, SukeSuke.id("glass_vignette"), SukeHud::vignette);
         e.registerAbove(VanillaGuiLayers.CROSSHAIR, SukeSuke.id("clear_time"), SukeHud::time);
      });
   }

   private static void vignette(GuiGraphics g, DeltaTracker delta) {
      Minecraft mc = Minecraft.getInstance();
      Player p = mc.player;
      if (p != null && Clear.invisible(p) && mc.options.getCameraType().isFirstPerson()) {
         float k = Math.max(0.0F, ClearTransitions.phase(p, delta.getGameTimeDeltaPartialTick(false)));
         int w = g.guiWidth();
         int h = g.guiHeight();
         int edge = Math.max(24, Math.min(w, h) / 7);
         int a = Math.round(36.0F * k);
         int outer = a << 24 | 12578784;
         int inner = 12578784;
         g.fillGradient(0, 0, w, edge, outer, inner);
         g.fillGradient(0, h - edge, w, h, inner, outer);

         for (int i = 0; i < edge; i += 2) {
            int ai = Math.round((float)a * (1.0F - (float)i / (float)edge) * (1.0F - (float)i / (float)edge));
            if (ai <= 0) {
               break;
            }

            g.fill(i, 0, i + 2, h, ai << 24 | 12578784);
            g.fill(w - i - 2, 0, w - i, h, ai << 24 | 12578784);
         }

         float t = ((float)(p.level().getGameTime() % 120L) + delta.getGameTimeDeltaPartialTick(false)) / 120.0F;
         int gx = Math.round(Mth.lerp(t, (float)(-w) * 0.2F, (float)w * 1.2F));
         int ga = Math.round(46.0F * k * Mth.sin((float) Math.PI * t));
         if (ga > 0) {
            g.fillGradient(gx - w / 10, 0, gx + w / 10, 2, ga << 24 | 14546376, 14546376);
         }
      }
   }

   private static void time(GuiGraphics g, DeltaTracker delta) {
      Minecraft mc = Minecraft.getInstance();
      Player p = mc.player;
      if (p != null && !mc.options.hideGui) {
         int cx = g.guiWidth() / 2;
         int y = 22;
         if (Clear.invisible(p) && SukeSuke.isUser(p)) {
            PlayerFruitData data = DFAttachments.data(p);
            boolean on = data.isToggleActive(SukeAbilities.INVISIBILITY.id()) || data.isToggleActive(SukeAbilities.GROUP_INVISIBILITY.id());
            if (on) {
               int max = Mastery.maxEnergy(data);
               float drain = Cloak.drainPerTick(max, data.mastery(), 1 + members(p));
               float seconds = drain <= 0.0F ? 0.0F : data.energy() / drain / 20.0F;
               bar(
                  g,
                  cx,
                  y,
                  data.energy() / Math.max(1.0F, (float)max),
                  Component.translatable("hud.devilfruits.suke_suke.time_left", new Object[]{clock(seconds)}),
                  8378568
               );
               y += 16;
            }
         }

         for (Entry<UUID, ShipView.Entry> en : ShipView.all().entrySet()) {
            ShipView.Entry e = en.getValue();
            if (ShipLook.glassFor(e)) {
               float left = Math.max(0.0F, (float)(e.end() - p.level().getGameTime()) - delta.getGameTimeDeltaPartialTick(false)) / 20.0F;
               float share = left * 20.0F / Math.max(1.0F, (float)(e.end() - e.start()));
               bar(
                  g, cx, y, share, Component.translatable("hud.devilfruits.suke_suke.ship_left", new Object[]{clock(left)}), left <= 3.0F ? 14546376 : 12578784
               );
               y += 16;
            }
         }
      }
   }

   private static int members(Player owner) {
      int n = 0;
      double r = SukeSuke.setting("groupRadius") + 1.0;

      for (Player other : owner.level().players()) {
         if (other != owner && Clear.invisible(other) && Clear.of(other).group().equals(owner.getUUID()) && other.distanceToSqr(owner) <= r * r) {
            n++;
         }
      }

      return n;
   }

   private static String clock(float seconds) {
      int s = (int)Math.ceil((double)seconds);
      return s >= 60 ? String.format(Locale.ROOT, "%d:%02d", s / 60, s % 60) : s + " s";
   }

   private static void bar(GuiGraphics g, int cx, int y, float share, Component label, int colour) {
      Minecraft mc = Minecraft.getInstance();
      g.drawCenteredString(mc.font, label, cx, y, 0xFF000000 | colour);
      int w = 64;
      int x0 = cx - w / 2;
      int y0 = y + 10;
      g.fill(x0 - 1, y0 - 1, x0 + w + 1, y0 + 3, 1713257034);
      g.fill(x0, y0, x0 + Math.round((float)w * Mth.clamp(share, 0.0F, 1.0F)), y0 + 2, -872415232 | colour);
      g.fill(x0, y0, x0 + Math.round((float)w * Mth.clamp(share, 0.0F, 1.0F)), y0 + 1, -520093697);
   }

   private SukeHud() {
   }
}
