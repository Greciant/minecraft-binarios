package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.client.render.BeamDraw;
import com.awakenedmod.awakened.client.render.FxLayer;
import com.awakenedmod.awakened.client.render.FxLayers;
import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.data.SpiderVisual;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage;
import org.joml.Vector3f;

public final class SpiderWebRenderer {
   private static final int SEGMENTS = 10;
   private static final float HALF_WIDTH = 0.018F;
   private static final double WRIST_SIDE = 0.25;
   private static final double WRIST_RISE = 0.1;
   private static final double WRIST_FORWARD = 0.45;
   private static final double SHOULDER_DROP = 0.3;
   private static final double ZIP_1X_RISE = -0.15;
   private static final double ZIP_1X_FWD = -0.25;
   private static final double ZIP_1X_SIDE = 0.0;
   private static final double MAX_SAG = 0.7;
   private static final double SAG_ZERO_SPEED = 1.2;
   private static final float ZIP_HALF_WIDTH = 0.012F;
   private static final double ZIP_MIN_SAG = 0.18;
   private static final double ZIP_WAVER = 0.06;
   private static final double YANK_SHOULDER_DROP = 0.15;
   private static final double YANK_SHOULDER_SEP = 0.32;
   private static final double YANK_ARM_LEN = 0.75;
   private static final double ZIP_PIVOT_Y = 0.9;
   private static final double ZIP_ARM_LEN = 0.68;
   private static final double WING_SHOULDER_H = 1.4;
   private static final double WING_SHOULDER_SIDE = 0.32;
   private static final double WING_SHOULDER_FWD = 0.0;
   private static final double WING_HIP_H = 0.82;
   private static final double WING_HIP_SIDE = 0.18;
   private static final double WING_HIP_FWD = 0.0;
   private static final double WING_TAIL_H = 0.52;
   private static final double WING_TAIL_SIDE = 0.1;
   private static final double WING_TAIL_FWD = -0.08;
   private static final double WING_ARM_LEN = 0.6;
   private static final float WING_RIB = 0.01F;
   private static final float WING_CROSS = 0.007F;
   private static final int WING_RIBS = 12;
   private static final double[] WING_RINGS = new double[]{0.3, 0.52, 0.74, 0.92};
   private static final int WING_FILL_COLOR = 820571381;
   private static final int WING_THREAD_COLOR = 1726541045;

   private SpiderWebRenderer() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(event -> {
         if (event.getStage() == Stage.AFTER_TRANSLUCENT_BLOCKS) {
            draw(event);
         }
      });
   }

   private static void draw(RenderLevelStageEvent event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null) {
         float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
         Vec3 cam = event.getCamera().getPosition();
         PoseStack poseStack = event.getPoseStack();
         BufferSource buffers = mc.renderBuffers().bufferSource();
         VertexConsumer vc = FxLayers.begin(buffers, FxLayer.LIGHT);
         drawLocal(mc, poseStack, vc, cam, partialTick);
         drawRemote(mc, poseStack, vc, cam, partialTick);
         buffers.endBatch();
      }
   }

   private static void drawLocal(Minecraft mc, PoseStack poseStack, VertexConsumer vc, Vec3 cam, float partialTick) {
      LocalPlayer self = mc.player;
      int yankId = SpiderSwingController.yankRenderId();
      Entity yankEntity = self != null && yankId >= 0 ? mc.level.getEntity(yankId) : null;
      boolean yank = yankEntity != null && yankEntity.isAlive();
      boolean glide = SpiderSwingController.gliding();
      if (self != null && (SpiderClientState.swinging() || SpiderClientState.zipping() || yank || glide)) {
         Vec3 anchor = yank ? yankEntity.getPosition(partialTick).add(0.0, (double)yankEntity.getBbHeight() * 0.5, 0.0) : SpiderClientState.anchor();
         boolean zip = yank || SpiderClientState.zipping();
         boolean firstPerson = mc.options.getCameraType().isFirstPerson();
         double yaw = Math.toRadians((double)Mth.rotLerp(partialTick, self.yBodyRotO, self.yBodyRot));
         Vec3 flat = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
         Vec3 right = flat.cross(new Vec3(0.0, 1.0, 0.0));
         Vec3 up = new Vec3(0.0, 1.0, 0.0);
         double rise = 0.1;
         double reach = 0.45;
         double sideMag = 0.25;
         Vec3 eyeBase = self.getPosition(partialTick).add(0.0, (double)self.getEyeHeight(), 0.0);
         double speed = self.getDeltaMovement().length();
         double sag = 0.7 * Mth.clamp(1.0 - speed / 1.2, 0.0, 1.0);
         float halfWidth = zip ? 0.012F : 0.018F;
         double waver = 0.0;
         if (zip) {
            sag = Math.max(sag, 0.18);
            waver = 0.06;
         }

         float phase = ((float)self.tickCount + partialTick) * 0.35F;
         if (!glide) {
            Vec3 base = firstPerson ? eyeBase.subtract(0.0, 0.3, 0.0) : eyeBase;
            double effRise = firstPerson ? rise + 0.3 : rise;
            Vec3 armBase = base.add(up.scale(effRise)).add(flat.scale(reach));
            if (zip && !yank) {
               boolean pulling = SpiderSwingController.zipPulling();
               double bodyRad = Math.toRadians(pulling ? 43.0 : 11.0);
               double totalRad = Math.toRadians(pulling ? -55.0 : -81.0);
               Vec3 feet = self.getPosition(partialTick);
               double shoulderAbovePivot = 0.4999999999999999;
               Vec3 pivot = feet.add(0.0, 0.9, 0.0);
               Vec3 shoulder = pivot.add(right.scale(0.32))
                  .add(new Vec3(0.0, 1.0, 0.0).scale(shoulderAbovePivot * Math.cos(bodyRad)))
                  .add(flat.scale(shoulderAbovePivot * Math.sin(bodyRad)));
               Vec3 armDir = flat.scale(-Math.sin(totalRad)).add(up.scale(-Math.cos(totalRad)));
               armBase = shoulder.add(armDir.scale(0.68));
               armBase = armBase.add(up.scale(-0.15)).add(flat.scale(-0.25)).add(right.scale(0.0));
               sideMag = 0.0;
            }

            if (yank) {
               double t = (double)((float)SpiderSwingController.yankRenderAge() + partialTick) / 20.0;
               double a = Math.toRadians(sampleYankArmPitch(t));
               Vec3 armDir = flat.scale(-Math.sin(a)).add(up.scale(-Math.cos(a)));
               Vec3 shoulder = eyeBase.subtract(up.scale(0.15));
               Vec3 reachV = armDir.scale(0.75);
               drawFrom(poseStack, vc, shoulder.subtract(right.scale(0.32)).add(reachV), anchor, cam, sag, halfWidth, waver, phase, 1.0F);
               drawFrom(poseStack, vc, shoulder.add(right.scale(0.32)).add(reachV), anchor, cam, sag, halfWidth, waver, phase, 1.0F);
            } else {
               double sideSign = zip ? 1.0 : (SpiderClientState.leftArm() ? -1.0 : 1.0);
               float attach = zip && SpiderSwingController.zipShooting() ? SpiderSwingController.zipAttachProgress() : 1.0F;
               drawFrom(poseStack, vc, armBase.add(right.scale(sideMag * sideSign)), anchor, cam, sag, halfWidth, waver, phase, attach);
            }
         } else if (!firstPerson) {
            float lookPitch = self.getViewXRot(partialTick);
            double lean = SpiderSwingController.glidePitchRadians(lookPitch);
            float tuck = SpiderSwingController.glideTuck(lookPitch, self.getDeltaMovement().length());
            double bodyYaw = Math.toRadians((double)Mth.rotLerp(partialTick, self.yBodyRotO, self.yBodyRot));
            Vec3 bFwd = new Vec3(-Math.sin(bodyYaw), 0.0, Math.cos(bodyYaw));
            Vec3 bUp = new Vec3(0.0, 1.0, 0.0);
            Vec3 bRight = bFwd.cross(bUp);
            Vec3 feet = self.getPosition(partialTick);
            Vec3 pivot = feet.add(0.0, 0.9, 0.0);
            double bankRad = SpiderSwingController.glideBodyRollRadians(partialTick);
            drawWings(poseStack, vc, feet, bFwd, bUp, bRight, cam, pivot, lean, tuck, bankRad);
         }
      }
   }

   private static void drawRemote(Minecraft mc, PoseStack poseStack, VertexConsumer vc, Vec3 cam, float partialTick) {
      for (Player p : mc.level.players()) {
         if (p != mc.player) {
            SpiderVisual v = (SpiderVisual)p.getData(AwakenedAttachments.SPIDER_VISUAL);
            if (v.gliding()) {
               drawRemoteWings(poseStack, vc, p, cam, partialTick);
            } else if (v.tether() == 1) {
               drawRemoteSwing(poseStack, vc, p, cam, partialTick, new Vec3(v.ax(), v.ay(), v.az()), v.leftArm());
            } else if (v.tether() == 2) {
               drawRemoteZip(poseStack, vc, p, cam, partialTick, new Vec3(v.ax(), v.ay(), v.az()));
            }
         }
      }
   }

   private static void drawRemoteSwing(PoseStack poseStack, VertexConsumer vc, Player p, Vec3 cam, float partialTick, Vec3 anchor, boolean leftArm) {
      double yaw = Math.toRadians((double)Mth.rotLerp(partialTick, p.yBodyRotO, p.yBodyRot));
      Vec3 flat = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
      Vec3 right = flat.cross(new Vec3(0.0, 1.0, 0.0));
      Vec3 up = new Vec3(0.0, 1.0, 0.0);
      Vec3 eyeBase = p.getPosition(partialTick).add(0.0, (double)p.getEyeHeight(), 0.0);
      Vec3 armBase = eyeBase.add(up.scale(0.1)).add(flat.scale(0.45));
      double speed = p.getDeltaMovement().length();
      double sag = 0.7 * Mth.clamp(1.0 - speed / 1.2, 0.0, 1.0);
      double sideSign = leftArm ? -1.0 : 1.0;
      float phase = ((float)p.tickCount + partialTick) * 0.35F;
      drawFrom(poseStack, vc, armBase.add(right.scale(0.25 * sideSign)), anchor, cam, sag, 0.018F, 0.0, phase, 1.0F);
   }

   private static void drawRemoteZip(PoseStack poseStack, VertexConsumer vc, Player p, Vec3 cam, float partialTick, Vec3 anchor) {
      double yaw = Math.toRadians((double)Mth.rotLerp(partialTick, p.yBodyRotO, p.yBodyRot));
      Vec3 flat = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
      Vec3 up = new Vec3(0.0, 1.0, 0.0);
      Vec3 armBase = p.getPosition(partialTick).add(0.0, (double)p.getEyeHeight(), 0.0).add(up.scale(0.1)).add(flat.scale(0.45));
      double speed = p.getDeltaMovement().length();
      double sag = Math.max(0.7 * Mth.clamp(1.0 - speed / 1.2, 0.0, 1.0), 0.18);
      float phase = ((float)p.tickCount + partialTick) * 0.35F;
      drawFrom(poseStack, vc, armBase, anchor, cam, sag, 0.012F, 0.06, phase, 1.0F);
   }

   private static void drawRemoteWings(PoseStack poseStack, VertexConsumer vc, Player p, Vec3 cam, float partialTick) {
      float lookPitch = p.getViewXRot(partialTick);
      double lean = SpiderSwingController.glidePitchRadians(lookPitch);
      float tuck = SpiderSwingController.glideTuck(lookPitch, p.getDeltaMovement().length());
      double bodyYaw = Math.toRadians((double)Mth.rotLerp(partialTick, p.yBodyRotO, p.yBodyRot));
      Vec3 bFwd = new Vec3(-Math.sin(bodyYaw), 0.0, Math.cos(bodyYaw));
      Vec3 bUp = new Vec3(0.0, 1.0, 0.0);
      Vec3 bRight = bFwd.cross(bUp);
      Vec3 feet = p.getPosition(partialTick);
      Vec3 pivot = feet.add(0.0, 0.9, 0.0);
      drawWings(poseStack, vc, feet, bFwd, bUp, bRight, cam, pivot, lean, tuck, 0.0);
   }

   private static void drawWings(
      PoseStack poseStack, VertexConsumer vc, Vec3 feet, Vec3 fwd, Vec3 up, Vec3 right, Vec3 cam, Vec3 pivot, double lean, float tuck, double bankRad
   ) {
      poseStack.pushPose();
      poseStack.translate(-cam.x, -cam.y, -cam.z);
      poseStack.translate(0.0F, -0.6F, 0.0F);
      Pose pose = poseStack.last();
      double armAngle = Math.toRadians(72.0 - (double)tuck * 48.0);
      double wristH = 1.4 - Math.cos(armAngle) * 0.6;
      double wristSide = 0.32 + Math.sin(armAngle) * 0.6;

      for (int side = -1; side <= 1; side += 2) {
         Vec3 sh = jointAnchor(feet, up, right, fwd, 1.4, 0.32 * (double)side, 0.0, pivot, right, -lean, bankRad);
         Vec3 hp = jointAnchor(feet, up, right, fwd, 0.82, 0.18 * (double)side, 0.0, pivot, right, -lean, bankRad);
         Vec3 tail = jointAnchor(feet, up, right, fwd, 0.52, 0.1 * (double)side, -0.08, pivot, right, -lean, bankRad);
         Vec3 wrist = jointAnchor(feet, up, right, fwd, wristH, wristSide * (double)side, 0.0, pivot, right, -lean, bankRad);
         Vec3[] edge = sampleEdge(new Vec3[]{sh, hp, tail}, 12);
         Vector3f w = vec(wrist);

         for (int i = 0; i < 12; i++) {
            Vector3f a = vec(edge[i]);
            Vector3f b = vec(edge[i + 1]);
            tri(pose, vc, w, a, b, 820571381);
            tri(pose, vc, w, b, a, 820571381);
         }

         for (int i = 0; i <= 12; i++) {
            beamSegment(pose, vc, w, vec(edge[i]), 0.01F, 1726541045);
         }

         for (double r : WING_RINGS) {
            for (int i = 0; i < 12; i++) {
               Vector3f a = vec(wrist.add(edge[i].subtract(wrist).scale(r)));
               Vector3f b = vec(wrist.add(edge[i + 1].subtract(wrist).scale(r)));
               beamSegment(pose, vc, a, b, 0.007F, 1726541045);
            }
         }
      }

      poseStack.popPose();
   }

   private static Vec3 jointAnchor(
      Vec3 feet, Vec3 up, Vec3 right, Vec3 fwd, double h, double side, double forward, Vec3 pivot, Vec3 axis, double angle, double bankRad
   ) {
      Vec3 p = feet.add(up.scale(h)).add(right.scale(side)).add(fwd.scale(forward));
      p = rotateAround(p, pivot, axis, angle);
      return rotateAround(p, pivot, fwd, bankRad);
   }

   private static Vec3 rotateAround(Vec3 p, Vec3 pivot, Vec3 axis, double angle) {
      Vec3 d = p.subtract(pivot);
      double c = Math.cos(angle);
      double s = Math.sin(angle);
      Vec3 rotated = d.scale(c).add(axis.cross(d).scale(s)).add(axis.scale(axis.dot(d) * (1.0 - c)));
      return pivot.add(rotated);
   }

   private static Vec3[] sampleEdge(Vec3[] corners, int count) {
      double total = 0.0;
      double[] segLen = new double[corners.length - 1];

      for (int i = 0; i < segLen.length; i++) {
         segLen[i] = corners[i + 1].distanceTo(corners[i]);
         total += segLen[i];
      }

      Vec3[] out = new Vec3[count + 1];

      for (int i = 0; i <= count; i++) {
         double target = total * (double)i / (double)count;

         int seg;
         for (seg = 0; seg < segLen.length - 1 && target > segLen[seg]; seg++) {
            target -= segLen[seg];
         }

         double f = segLen[seg] > 1.0E-6 ? target / segLen[seg] : 0.0;
         out[i] = corners[seg].add(corners[seg + 1].subtract(corners[seg]).scale(f));
      }

      return out;
   }

   private static Vector3f vec(Vec3 v) {
      return BeamDraw.vec(v);
   }

   private static double sampleYankArmPitch(double t) {
      double[] ts = new double[]{0.0, 0.06, 0.16, 0.26, 0.4};
      double[] ps = new double[]{0.0, -96.0, -70.0, 55.0, 0.0};
      if (t <= ts[0]) {
         return ps[0];
      } else {
         for (int i = 1; i < ts.length; i++) {
            if (t <= ts[i]) {
               double f = (t - ts[i - 1]) / (ts[i] - ts[i - 1]);
               return ps[i - 1] + (ps[i] - ps[i - 1]) * f;
            }
         }

         return ps[ps.length - 1];
      }
   }

   private static void drawFrom(
      PoseStack poseStack, VertexConsumer vc, Vec3 wrist, Vec3 anchor, Vec3 cam, double sag, float halfWidth, double waver, float phase, float attach
   ) {
      Vec3 end = wrist.add(anchor.subtract(wrist).scale((double)attach));
      poseStack.pushPose();
      poseStack.translate(wrist.x - cam.x, wrist.y - cam.y, wrist.z - cam.z);
      drawStrand(poseStack, vc, wrist, end, sag * (double)attach, halfWidth, waver, phase);
      poseStack.popPose();
   }

   private static void drawStrand(PoseStack poseStack, VertexConsumer vc, Vec3 wrist, Vec3 anchor, double sag, float halfWidth, double waver, float phase) {
      Pose pose = poseStack.last();
      int color = -1512203;
      Vec3 span = anchor.subtract(wrist);
      Vec3 sideDir = new Vec3(-span.z, 0.0, span.x);
      if (sideDir.lengthSqr() < 1.0E-6) {
         sideDir = new Vec3(1.0, 0.0, 0.0);
      }

      sideDir = sideDir.normalize();
      Vector3f prev = new Vector3f(0.0F, 0.0F, 0.0F);

      for (int i = 1; i <= 10; i++) {
         float t = (float)i / 10.0F;
         double x = (anchor.x - wrist.x) * (double)t;
         double y = (anchor.y - wrist.y) * (double)t;
         double z = (anchor.z - wrist.z) * (double)t;
         double droop = sag * 4.0 * (double)t * (1.0 - (double)t);
         if (waver > 0.0) {
            double w = waver * 4.0 * (double)t * (1.0 - (double)t) * Math.sin((double)phase + (double)t * 6.0);
            x += sideDir.x * w;
            z += sideDir.z * w;
         }

         Vector3f cur = new Vector3f((float)x, (float)(y - droop), (float)z);
         beamSegment(pose, vc, prev, cur, halfWidth, color);
         prev = cur;
      }
   }

   private static void beamSegment(Pose pose, VertexConsumer vc, Vector3f a, Vector3f b, float w, int color) {
      BeamDraw.beamSegment(pose, vc, a, b, w, color);
   }

   private static void tri(Pose pose, VertexConsumer vc, Vector3f a, Vector3f b, Vector3f c, int color) {
      BeamDraw.tri(pose, vc, a, b, c, color);
   }
}
