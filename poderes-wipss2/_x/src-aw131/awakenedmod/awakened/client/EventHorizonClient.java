package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.client.render.EventHorizonRenderer;
import com.awakenedmod.awakened.content.graviton.EventHorizonEntity;
import com.awakenedmod.awakened.content.graviton.EventHorizonVisuals;
import com.awakenedmod.awakened.core.Lab;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw.Layer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderLivingEvent.Post;
import net.neoforged.neoforge.client.event.RenderLivingEvent.Pre;
import net.neoforged.neoforge.client.event.ViewportEvent.ComputeFogColor;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class EventHorizonClient {
   private static final Lab.Value DIM_RANGE = Lab.CLIENT.def("graviton.dim.range", 40.0, 0.0, 128.0, 1.0);
   private static final Lab.Value DIM_MAX = Lab.CLIENT.def("graviton.dim.max", 0.7, 0.0, 1.0, 0.05);
   private static final Lab.Value FOG_TEAL = Lab.CLIENT.def("graviton.dim.teal", 0.45, 0.0, 1.0, 0.05);
   private static final Lab.Value STRETCH = Lab.CLIENT.def("graviton.stretch.max", 2.4, 1.0, 6.0, 0.1);
   private static final Lab.Value STRETCH_FULL = Lab.CLIENT.def("graviton.stretch.full", 2.0, 1.0, 6.0, 0.1);
   private static final Lab.Value WASH = Lab.CLIENT.def("graviton.dim.wash", 0.08, 0.0, 1.0, 0.02);
   private static final Lab.Value VIGNETTE = Lab.CLIENT.def("graviton.dim.vignette", 0.55, 0.0, 1.0, 0.02);
   private static final float TEAL_R = 0.02F;
   private static final float TEAL_G = 0.1F;
   private static final float TEAL_B = 0.13F;
   private static final int WASH_RGB = 198924;
   private static final List<EventHorizonEntity> HOLES = new ArrayList<>();
   private static Object holesLevel;
   private static long holesTime = Long.MIN_VALUE;
   private static float holesPt = Float.NaN;
   private static float darkenCached;
   private static Object darkenLevel;
   private static long darkenTime = Long.MIN_VALUE;
   private static float darkenPt = Float.NaN;
   private static Matrix4f pending;

   private EventHorizonClient() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(EventPriority.LOWEST, EventHorizonClient::onRenderLivingPre);
      gameBus.addListener(EventPriority.HIGHEST, EventHorizonClient::onRenderLivingPost);
      gameBus.addListener(EventHorizonClient::onFogColor);
   }

   public static List<EventHorizonEntity> holes(float partialTick) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null) {
         HOLES.clear();
         holesLevel = null;
         return HOLES;
      } else {
         long now = mc.level.getGameTime();
         if (holesLevel == mc.level && holesTime == now && holesPt == partialTick) {
            return HOLES;
         } else {
            holesLevel = mc.level;
            holesTime = now;
            holesPt = partialTick;
            HOLES.clear();

            for (Entity e : mc.level.entitiesForRendering()) {
               if (e instanceof EventHorizonEntity hole) {
                  HOLES.add(hole);
               }
            }

            return HOLES;
         }
      }
   }

   public static float darken(float partialTick) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null) {
         return 0.0F;
      } else {
         long now = mc.level.getGameTime();
         if (darkenLevel == mc.level && darkenTime == now && darkenPt == partialTick) {
            return darkenCached;
         } else {
            darkenLevel = mc.level;
            darkenTime = now;
            darkenPt = partialTick;
            darkenCached = computeDarken(mc, partialTick);
            return darkenCached;
         }
      }
   }

   private static float computeDarken(Minecraft mc, float partialTick) {
      List<EventHorizonEntity> holes = holes(partialTick);
      if (holes.isEmpty()) {
         return 0.0F;
      } else {
         float range = DIM_RANGE.getF();
         float max = DIM_MAX.getF();
         if (!(range <= 0.0F) && !(max <= 0.0F)) {
            Vec3 eye = mc.gameRenderer.getMainCamera().getPosition();
            float best = 0.0F;

            for (EventHorizonEntity hole : holes) {
               EventHorizonVisuals.Phase p = hole.phase(partialTick);
               float env = p.open() * (1.0F - p.flash() * p.flash());
               if (!(env <= 0.0F)) {
                  double d = eye.distanceTo(hole.getPosition(partialTick));
                  float near = (float)Mth.clamp(1.0 - (d - (double)range * 0.3) / ((double)range * 0.7), 0.0, 1.0);
                  best = Math.max(best, env * near * near * (3.0F - 2.0F * near));
               }
            }

            return best * max;
         } else {
            return 0.0F;
         }
      }
   }

   private static void onFogColor(ComputeFogColor event) {
      float d = darken((float)event.getPartialTick());
      if (!(d <= 0.001F)) {
         float t = d * FOG_TEAL.getF();
         event.setRed(Mth.lerp(t, event.getRed(), 0.02F));
         event.setGreen(Mth.lerp(t, event.getGreen(), 0.1F));
         event.setBlue(Mth.lerp(t, event.getBlue(), 0.13F));
      }
   }

   private static Matrix4f stretchFor(Vec3 feet, float h, float partialTick) {
      List<EventHorizonEntity> holes = holes(partialTick);
      if (holes.isEmpty()) {
         return null;
      } else {
         Vec3 body = feet.add(0.0, (double)h * 0.5, 0.0);
         EventHorizonEntity nearest = null;
         double nearestDist = Double.MAX_VALUE;
         float nearestPresence = 0.0F;

         for (EventHorizonEntity hole : holes) {
            EventHorizonVisuals.Phase p = hole.phase(partialTick);
            float presence = p.open() * (1.0F - p.collapse());
            if (!(presence <= 0.01F)) {
               double d = body.distanceTo(hole.getPosition(partialTick));
               if (d < (double)hole.pullRadius() && d < nearestDist) {
                  nearest = hole;
                  nearestDist = d;
                  nearestPresence = presence;
               }
            }
         }

         if (nearest == null) {
            return null;
         } else {
            float full = STRETCH_FULL.getF() * EventHorizonRenderer.coreRadius();
            float f = (float)Mth.clamp(((double)nearest.pullRadius() - nearestDist) / Math.max(0.5, (double)(nearest.pullRadius() - full)), 0.0, 1.0);
            float k = 1.0F + (STRETCH.getF() - 1.0F) * f * f * nearestPresence;
            if (k <= 1.001F) {
               return null;
            } else {
               Vec3 dir = nearest.getPosition(partialTick).subtract(body);
               if (dir.lengthSqr() < 1.0E-6) {
                  return null;
               } else {
                  Vector3f axis = new Vector3f((float)dir.x, (float)dir.y, (float)dir.z).normalize();
                  Quaternionf toAxis = new Quaternionf().rotationTo(0.0F, 1.0F, 0.0F, axis.x, axis.y, axis.z);
                  float across = (float)(1.0 / Math.sqrt((double)k));
                  return new Matrix4f()
                     .translate(0.0F, h * 0.5F, 0.0F)
                     .rotate(toAxis)
                     .scale(across, k, across)
                     .rotate(toAxis.conjugate(new Quaternionf()))
                     .translate(0.0F, -h * 0.5F, 0.0F);
               }
            }
         }
      }
   }

   private static void onRenderLivingPre(Pre<?, ?> event) {
      pending = null;
      if (!holes(event.getPartialTick()).isEmpty()) {
         LivingEntity entity = event.getEntity();
         if (!(entity instanceof Player)) {
            Vec3 feet = entity.getPosition(event.getPartialTick());
            float h = entity.getBbHeight();
            Matrix4f m = stretchFor(feet, h, event.getPartialTick());
            if (m != null) {
               PoseStack ps = event.getPoseStack();
               ps.mulPose(m);
               pending = m.invert();
            }
         }
      }
   }

   private static void onRenderLivingPost(Post<?, ?> event) {
      if (pending != null) {
         event.getPoseStack().mulPose(pending);
         pending = null;
      }
   }

   public static final class Overlay implements Layer {
      public void render(GuiGraphics g, DeltaTracker deltaTracker) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null && !mc.options.hideGui) {
            float d = EventHorizonClient.darken(deltaTracker.getGameTimeDeltaPartialTick(false)) / Math.max(0.01F, EventHorizonClient.DIM_MAX.getF());
            if (!(d <= 0.005F)) {
               int w = g.guiWidth();
               int h = g.guiHeight();
               int washA = Mth.clamp(Math.round(d * EventHorizonClient.WASH.getF() * 255.0F), 0, 255);
               if (washA > 0) {
                  g.fill(0, 0, w, h, washA << 24 | 198924);
               }

               int edgeA = Mth.clamp(Math.round(d * EventHorizonClient.VIGNETTE.getF() * 255.0F), 0, 255);
               if (edgeA > 0) {
                  int edge = edgeA << 24 | 198924;
                  int clear = 198924;
                  int bandV = Math.max(24, h * 9 / 20);
                  g.fillGradient(0, 0, w, bandV, edge, clear);
                  g.fillGradient(0, h - bandV, w, h, clear, edge);
                  int bandH = Math.max(24, w * 3 / 10);
                  int steps = 24;

                  for (int i = 0; i < steps; i++) {
                     float q = 1.0F - (float)i / (float)steps;
                     int alpha = Math.round((float)edgeA * q * q);
                     if (alpha <= 0) {
                        break;
                     }

                     int x0 = bandH * i / steps;
                     int x1 = bandH * (i + 1) / steps;
                     int c = alpha << 24 | 198924;
                     g.fill(x0, 0, x1, h, c);
                     g.fill(w - x1, 0, w - x0, h, c);
                  }
               }
            }
         }
      }
   }
}
