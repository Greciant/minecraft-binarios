package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.client.VelocityLaneTransitClient;
import com.awakenedmod.awakened.content.velocity.VelocityPalette;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw.Layer;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.client.event.ViewportEvent.ComputeCameraAngles;

public final class VelocityScreenFx {
   private static final float RISE = 0.15F;
   private static final float FALL = 0.1F;
   private static final int TIMELESS_TINT = 9872568;
   private static final float TIMELESS_TINT_ALPHA = 0.18F;
   private static final float TIMELESS_EDGE_ALPHA = 0.34F;
   private static final float TIMELESS_EDGE_DEPTH = 0.07F;
   private static final float HEARTBEAT_TICKS = 40.0F;
   private static final float HEARTBEAT_DUB_AT = 0.18F;
   private static final float HEARTBEAT_DUB = 0.6F;
   private static final float HEARTBEAT_DECAY = 9.0F;
   private static final float TIMELESS_KICK = 0.05F;
   private static final int TIMELESS_KICK_RISE_TICKS = 8;
   private static final int TIMELESS_KICK_SETTLE_TICKS = 20;
   private static final float TUNNEL_EDGE_ALPHA = 0.65F;
   private static final float TUNNEL_CENTRE_ALPHA = 0.25F;
   private static final float TUNNEL_VIGNETTE_DEPTH = 0.45F;
   private static final int TUNNEL_VIGNETTE = 265002;
   private static final float FOV_PUSH = 0.15F;
   private static final float ROLL_DEG = 1.0F;
   private static final float ROLL_PERIOD_TICKS = 20.0F;
   private static final int H_SLICES = 14;
   private static final float LANE_FOV_EXTRA = 0.13F;
   private static final float LANE_ROLL_DEG = 1.6F;
   private static final float LANE_VIGNETTE_ALPHA = 0.3F;
   private static final float LANE_VIGNETTE_DEPTH = 0.6F;
   private static final int LANE_FLASH_TINT = 15267071;
   private static final float LANE_FLASH_ALPHA = 0.85F;
   private static final float LANE_FLASH_DECAY = 0.45F;
   private static final int BOOM_TICKS = 6;
   private static final float BOOM_FOV_PUSH = 0.08F;
   private static final int BOOM_FLASH_TICKS = 1;
   private static final float BOOM_FLASH_ALPHA = 0.25F;
   private static final int BOOM_FLASH_TINT = 16777215;
   private static boolean timelessOn;
   private static boolean tunnelOn;
   private static float timeless;
   private static float tunnel;
   private static int boomTicks;
   private static int kickTicks = -1;
   private static float laneFlash;

   private VelocityScreenFx() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(VelocityScreenFx::onClientTick);
      gameBus.addListener(VelocityScreenFx::onComputeFov);
      gameBus.addListener(VelocityScreenFx::onCameraAngles);
   }

   public static void set(int kind, boolean on) {
      if (kind == 0) {
         if (on && !timelessOn) {
            kickTicks = 0;
         }

         timelessOn = on;
      } else if (kind == 1) {
         tunnelOn = on;
      } else if (kind == 2) {
         boomTicks = 6;
      }
   }

   public static void reset() {
      timelessOn = false;
      tunnelOn = false;
      timeless = 0.0F;
      tunnel = 0.0F;
      boomTicks = 0;
      kickTicks = -1;
      laneFlash = 0.0F;
      VelocityTimelessShader.clear();
   }

   public static float timeless() {
      return timeless;
   }

   public static float tunnel() {
      return tunnel;
   }

   private static void onClientTick(Post event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null) {
         reset();
      } else if (!mc.isPaused()) {
         timeless = ease(timeless, timelessOn);
         tunnel = ease(tunnel, tunnelOn || VelocityLaneTransitClient.active());
         laneFlash = VelocityLaneTransitClient.active() ? VelocityLaneTransitClient.flare(1.0F) : laneFlash * 0.45F;
         VelocityTimelessShader.update(timeless);
         if (boomTicks > 0) {
            boomTicks--;
         }

         if (kickTicks >= 0 && ++kickTicks > 28) {
            kickTicks = -1;
         }
      }
   }

   private static float boom() {
      float t = (float)boomTicks / 6.0F;
      return t * t;
   }

   private static float kick() {
      if (kickTicks < 0) {
         return 0.0F;
      } else {
         return kickTicks <= 8 ? VelocityStreaks.easeOut((float)kickTicks / 8.0F) : 1.0F - AnimEnvelopes.smooth((float)(kickTicks - 8) / 20.0F);
      }
   }

   private static float ease(float value, boolean on) {
      float target = on ? 1.0F : 0.0F;
      if (value < target) {
         return Math.min(target, value + 0.15F);
      } else {
         return value > target ? Math.max(target, value - 0.1F) : value;
      }
   }

   private static void onComputeFov(ComputeFovModifierEvent event) {
      float push = 0.15F * tunnel + 0.13F * tunnel * VelocityLaneTransitClient.ramp(0.0F) + 0.08F * boom() + 0.05F * kick();
      if (!(push <= 0.0F)) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null && event.getPlayer() == mc.player) {
            event.setNewFovModifier(event.getNewFovModifier() * (1.0F + push));
         }
      }
   }

   private static void onCameraAngles(ComputeCameraAngles event) {
      if (!(tunnel <= 0.0F)) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null) {
            float partial = (float)event.getPartialTick();
            float t = (float)mc.player.tickCount + partial;
            float ramp = VelocityLaneTransitClient.ramp(partial);
            float slow = 1.0F * Mth.sin(t * (float) (Math.PI * 2) / 20.0F) * (1.0F - ramp);
            float footfalls = 1.6F * ramp * Mth.sin(VelocityLaneTransitClient.cadence(partial));
            event.setRoll(event.getRoll() + tunnel * (slow + footfalls));
         }
      }
   }

   public static final class Overlay implements Layer {
      public void render(GuiGraphics g, DeltaTracker deltaTracker) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null
            && !mc.options.hideGui
            && (!(VelocityScreenFx.timeless <= 0.0F) || !(VelocityScreenFx.tunnel <= 0.0F) || VelocityScreenFx.boomTicks > 0)) {
            float partial = deltaTracker.getGameTimeDeltaPartialTick(false);
            float time = (float)mc.player.tickCount + partial;
            int w = g.guiWidth();
            int h = g.guiHeight();
            VelocityPalette palette = VelocityPalette.of(mc.player);
            if (VelocityScreenFx.timeless > 0.0F) {
               drawTimeless(g, w, h, time, palette);
            }

            if (VelocityScreenFx.tunnel > 0.0F) {
               drawTunnel(g, w, h);
               drawLane(g, w, h, partial);
            }

            if (VelocityScreenFx.boomTicks > 5) {
               g.fill(0, 0, w, h, alpha(0.25F) << 24 | 16777215);
            }
         }
      }

      private static void drawTimeless(GuiGraphics g, int w, int h, float time, VelocityPalette palette) {
         float beat = heartbeat(time);
         int tintA = VelocityTimelessShader.active() ? 0 : alpha(VelocityScreenFx.timeless * 0.18F * (1.0F + 0.15F * beat));
         if (tintA > 0) {
            g.fill(0, 0, w, h, tintA << 24 | 9872568);
         }

         int depth = Math.max(6, Math.round((float)Math.min(w, h) * 0.07F));
         int edge = alpha(VelocityScreenFx.timeless * 0.34F * (0.55F + 0.45F * beat)) << 24 | palette.body();
         vignette(g, w, h, depth, depth, edge);
      }

      private static float heartbeat(float time) {
         float p = Mth.frac(time / 40.0F);
         float lub = (float)Math.exp((double)(-p * 9.0F));
         float dub = p >= 0.18F ? 0.6F * (float)Math.exp((double)(-(p - 0.18F) * 9.0F)) : 0.0F;
         return Math.min(1.0F, lub + dub);
      }

      private static void drawTunnel(GuiGraphics g, int w, int h) {
         int centreA = alpha(VelocityScreenFx.tunnel * 0.25F);
         if (centreA > 0) {
            g.fill(0, 0, w, h, centreA << 24 | 265002);
         }

         float bandAlpha = 0.5333333F;
         int edge = alpha(VelocityScreenFx.tunnel * bandAlpha) << 24 | 265002;
         int depthY = Math.round((float)h * 0.5F * 0.45F);
         int depthX = Math.round((float)w * 0.5F * 0.45F);
         vignette(g, w, h, depthX, depthY, edge);
      }

      private static void drawLane(GuiGraphics g, int w, int h, float partial) {
         float ramp = VelocityLaneTransitClient.ramp(partial);
         if (ramp > 0.0F) {
            int edge = alpha(VelocityScreenFx.tunnel * 0.3F * ramp) << 24 | 265002;
            vignette(g, w, h, Math.round((float)w * 0.5F * 0.6F), Math.round((float)h * 0.5F * 0.6F), edge);
         }

         float flash = Math.max(VelocityLaneTransitClient.flare(partial), VelocityScreenFx.laneFlash);
         if (flash > 0.0F) {
            float s = flash * flash * flash;
            g.fill(0, 0, w, h, alpha(0.85F * s) << 24 | 15267071);
         }
      }

      private static void vignette(GuiGraphics g, int w, int h, int depthX, int depthY, int edge) {
         int clear = edge & 16777215;
         g.fillGradient(0, 0, w, depthY, edge, clear);
         g.fillGradient(0, h - depthY, w, h, clear, edge);
         hGradient(g, 0, 0, depthX, h, edge, clear);
         hGradient(g, w - depthX, 0, w, h, clear, edge);
      }

      private static void hGradient(GuiGraphics g, int x0, int y0, int x1, int y1, int from, int to) {
         int width = x1 - x0;
         if (width > 0) {
            for (int i = 0; i < 14; i++) {
               int sx0 = x0 + width * i / 14;
               int sx1 = x0 + width * (i + 1) / 14;
               if (sx1 > sx0) {
                  float t = ((float)i + 0.5F) / 14.0F;
                  int a = Math.round((float)(from >>> 24) * (1.0F - t) + (float)(to >>> 24) * t);
                  g.fill(sx0, y0, sx1, y1, a << 24 | from & 16777215);
               }
            }
         }
      }

      private static int alpha(float a) {
         return Mth.clamp(Math.round(a * 255.0F), 0, 255);
      }
   }
}
