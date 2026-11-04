package com.awakenedmod.awakened.client;

import com.mojang.math.Axis;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw.Layer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;

public final class SpiderSenseFx {
   private static final int DURATION = 12;
   private static final float EDGE_PEAK = 0.42F;
   private static final long MIN_TING_GAP_MS = 180L;
   private static final int MOB_RGB = 13775674;
   private static final int PROJ_RGB = 15245344;
   private static final int CREP_RGB = 5950005;
   private static final int COLOR_LEVEL = 5;
   private static final int STRAND_LEVEL = 8;
   private static final int ARROW_LEVEL = 10;
   private static int totalTicks;
   private static int ticksLeft;
   private static int bearingDeg;
   private static float intensity;
   private static boolean directional;
   private static long lastTingMs;
   private static int kind;
   private static int masteryLevel;
   private static int edgeRgb = 13775674;

   private SpiderSenseFx() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(SpiderSenseFx::onClientTick);
   }

   public static void trigger(int bearing, float threatIntensity, boolean isDirectional, int threatKind, int mastery) {
      if ((Boolean)ClientConfig.SPIDER_SENSE.get()) {
         bearingDeg = bearing;
         intensity = Mth.clamp(threatIntensity, 0.0F, 1.0F);
         directional = isDirectional;
         kind = threatKind;
         masteryLevel = mastery;
         int var10000;
         if (mastery >= 5) {
            switch (threatKind) {
               case 2:
                  var10000 = 15245344;
                  break;
               case 3:
                  var10000 = 5950005;
                  break;
               default:
                  var10000 = 13775674;
            }
         } else {
            var10000 = 13775674;
         }

         edgeRgb = var10000;
         totalTicks = 12;
         ticksLeft = 12;
         Minecraft mc = Minecraft.getInstance();
         long now = System.currentTimeMillis();
         if (mc.player != null && now - lastTingMs >= 180L) {
            lastTingMs = now;
            float pitch = 1.4F + intensity * 0.5F;
            float vol = 0.35F + intensity * 0.25F;
            mc.getSoundManager().play(SimpleSoundInstance.forUI((SoundEvent)SoundEvents.NOTE_BLOCK_CHIME.value(), pitch, vol));
         }
      }
   }

   private static void onClientTick(Post event) {
      if (ticksLeft > 0 && !Minecraft.getInstance().isPaused()) {
         ticksLeft--;
      }
   }

   private static float fraction(float partialTick) {
      return ticksLeft > 0 && totalTicks > 0 ? Mth.clamp(((float)ticksLeft - partialTick) / (float)totalTicks, 0.0F, 1.0F) : 0.0F;
   }

   public static final class Overlay implements Layer {
      public void render(GuiGraphics g, DeltaTracker deltaTracker) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null && SpiderSenseFx.ticksLeft > 0) {
            float frac = SpiderSenseFx.fraction(deltaTracker.getGameTimeDeltaPartialTick(false));
            if (!(frac <= 0.0F)) {
               int w = g.guiWidth();
               int h = g.guiHeight();
               float pulse = frac > 0.5F ? 1.0F : frac * 2.0F;
               float strength = SpiderSenseFx.intensity * pulse;
               float peak = strength * 0.42F;
               int band = Mth.clamp(Math.min(w, h) / 5, 28, 56);
               if (!SpiderSenseFx.directional) {
                  hBand(g, 0, 0, band, h, false, peak);
                  hBand(g, w - band, 0, band, h, true, peak);
                  g.fillGradient(0, 0, w, band, color(peak), color(0.0F));
                  g.fillGradient(0, h - band, w, h, color(0.0F), color(peak));
               } else {
                  float rad = (float)Math.toRadians((double)SpiderSenseFx.bearingDeg);
                  float wL = Math.max(0.0F, -Mth.sin(rad));
                  float wR = Math.max(0.0F, Mth.sin(rad));
                  float wT = Math.max(0.0F, Mth.cos(rad));
                  float wB = Math.max(0.0F, -Mth.cos(rad));
                  float bl = 0.18F;
                  hBand(g, 0, 0, band, h, false, (bl + wL) * peak);
                  hBand(g, w - band, 0, band, h, true, (bl + wR) * peak);
                  g.fillGradient(0, 0, w, band, color((bl + wT) * peak), color(0.0F));
                  g.fillGradient(0, h - band, w, h, color(0.0F), color((bl + wB) * peak));
                  float maxW = Math.max(Math.max(wL, wR), Math.max(wT, wB));
                  if (maxW > 0.35F) {
                     float flareA = Math.min(peak * 1.5F, 0.75F);
                     int fb = band * 2 / 3;
                     if (wL >= maxW) {
                        hBand(g, 0, h / 4, fb, h / 2, false, flareA);
                     } else if (wR >= maxW) {
                        hBand(g, w - fb, h / 4, fb, h / 2, true, flareA);
                     } else if (wT >= maxW) {
                        g.fillGradient(w / 2 - w / 6, 0, w / 2 + w / 6, fb, color(flareA), color(0.0F));
                     } else {
                        g.fillGradient(w / 2 - w / 6, h - fb, w / 2 + w / 6, h, color(0.0F), color(flareA));
                     }
                  }

                  if (SpiderSenseFx.masteryLevel >= 8) {
                     drawStrands(g, w, h, rad, strength);
                  }

                  if (SpiderSenseFx.masteryLevel >= 10) {
                     drawArrow(g, w, h, rad, strength);
                  }
               }
            }
         }
      }

      private static void hBand(GuiGraphics g, int x, int y, int width, int height, boolean fromRight, float a) {
         if (!(a <= 0.0F)) {
            int slices = Math.min(8, width);

            for (int i = 0; i < slices; i++) {
               float t = fromRight ? (float)(i + 1) / (float)slices : 1.0F - (float)i / (float)slices;
               float sa = a * t * t;
               int ia = Mth.clamp(Math.round(sa * 255.0F), 0, 255);
               if (ia > 0) {
                  int x1 = x + width * i / slices;
                  int x2 = x + width * (i + 1) / slices;
                  g.fill(x1, y, x2, y + height, ia << 24 | SpiderSenseFx.edgeRgb);
               }
            }
         }
      }

      private static int color(float a) {
         return Mth.clamp(Math.round(a * 255.0F), 0, 255) << 24 | SpiderSenseFx.edgeRgb;
      }

      private static void drawStrands(GuiGraphics g, int w, int h, float rad, float strength) {
         float cx = (float)w / 2.0F;
         float cy = (float)h / 2.0F;
         float dx = Mth.sin(rad);
         float dy = -Mth.cos(rad);
         float tx = Math.abs(dx) > 1.0E-4F ? (dx > 0.0F ? ((float)w - cx) / dx : -cx / dx) : Float.MAX_VALUE;
         float ty = Math.abs(dy) > 1.0E-4F ? (dy > 0.0F ? ((float)h - cy) / dy : -cy / dy) : Float.MAX_VALUE;
         float t = Math.min(tx, ty);
         float ex = Mth.clamp(cx + t * dx, 0.0F, (float)w);
         float ey = Mth.clamp(cy + t * dy, 0.0F, (float)h);
         float idx = -dx;
         float idy = -dy;
         float px = idy;
         float py = -idx;
         float reach = (float)Math.min(w, h) * 0.25F;
         float vx = ex + idx * reach;
         float vy = ey + idy * reach;
         int strandColor = color(Math.min(strength * 0.5F, 0.55F));
         float spread = 24.0F;

         for (int s = -1; s <= 1; s++) {
            drawLine(g, ex + px * spread * (float)s, ey + py * spread * (float)s, vx, vy, strandColor);
         }
      }

      private static void drawArrow(GuiGraphics g, int w, int h, float rad, float strength) {
         float cx = (float)w / 2.0F;
         float cy = (float)h / 2.0F;
         float dx = Mth.sin(rad);
         float dy = -Mth.cos(rad);
         float tx = Math.abs(dx) > 1.0E-4F ? (dx > 0.0F ? ((float)w - cx) / dx : -cx / dx) : Float.MAX_VALUE;
         float ty = Math.abs(dy) > 1.0E-4F ? (dy > 0.0F ? ((float)h - cy) / dy : -cy / dy) : Float.MAX_VALUE;
         float t = Math.min(tx, ty);
         float ex = Mth.clamp(cx + t * dx, 0.0F, (float)w);
         float ey = Mth.clamp(cy + t * dy, 0.0F, (float)h);
         int arrowColor = color(Math.min(strength * 0.9F, 0.82F));
         int arrowH = 14;
         int arrowW = 10;
         g.pose().pushPose();
         g.pose().translate(ex, ey, 0.0F);
         g.pose().mulPose(Axis.ZP.rotationDegrees((float)SpiderSenseFx.bearingDeg));

         for (int i = 0; i < arrowH; i++) {
            int half = arrowW - arrowW * i / arrowH;
            if (half > 0) {
               g.fill(-half, i, half, i + 1, arrowColor);
            }
         }

         g.pose().popPose();
      }

      private static void drawLine(GuiGraphics g, float x1, float y1, float x2, float y2, int color) {
         float lx = x2 - x1;
         float ly = y2 - y1;
         float len = (float)Math.sqrt((double)(lx * lx + ly * ly));
         if (!(len < 1.0F)) {
            float angle = (float)Math.atan2((double)ly, (double)lx);
            g.pose().pushPose();
            g.pose().translate(x1, y1, 0.0F);
            g.pose().mulPose(Axis.ZP.rotationDegrees((float)Math.toDegrees((double)angle)));
            g.fill(0, -1, Math.round(len), 1, color);
            g.pose().popPose();
         }
      }
   }
}
