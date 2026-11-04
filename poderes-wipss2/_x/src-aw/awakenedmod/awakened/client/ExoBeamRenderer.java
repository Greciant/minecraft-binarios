package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.client.render.BeamDraw;
import com.awakenedmod.awakened.client.render.FxLayer;
import com.awakenedmod.awakened.client.render.FxLayers;
import com.awakenedmod.awakened.content.exosavant.ExoTags;
import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.data.ExoVisual;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage;
import org.joml.Vector3f;

public final class ExoBeamRenderer {
   private static final ResourceLocation CHEST_BEAM = AwId.of("chest_beam");
   private static final double RANGE = 24.0;
   private static final float BEAM_WIDTH = 0.22F;
   private static final double TILT_FOLLOW = 0.65;
   private static final double TILT_MAX_DEG = 55.0;
   private static final double REACTOR_UP = 0.4;
   private static final double REACTOR_OUT = 0.2;
   private static final double HIP_BELOW_EYE = 0.87;
   private static final double FP_FORWARD = 0.45;
   private static final double FP_DROP = 0.32;
   private static final int GLOW = 1352653055;
   private static final int BODY = -1866471681;
   private static final int CORE = -386860289;
   private static final float PULSE = 0.18F;
   private static final float FLARE_HALF = 0.17F;

   private ExoBeamRenderer() {
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
         VertexConsumer vc = null;

         for (Player p : mc.level.players()) {
            if (AwakenedAnimations.isPlaying(p.getId(), CHEST_BEAM)) {
               ExoVisual vis = (ExoVisual)p.getData(AwakenedAttachments.EXO_VISUAL);
               if (vis.suited()) {
                  if (vc == null) {
                     vc = FxLayers.begin(buffers, FxLayer.LIGHT);
                  }

                  drawBeam(mc, poseStack, vc, cam, p, vis, partialTick);
               }
            }
         }

         if (vc != null) {
            buffers.endBatch();
         }
      }
   }

   private static void drawBeam(Minecraft mc, PoseStack poseStack, VertexConsumer vc, Vec3 cam, Player p, ExoVisual vis, float partialTick) {
      Vec3 eye = p.getEyePosition(partialTick);
      Vec3 aimDir = p.getViewVector(partialTick);
      if (!vis.taggedIds().isEmpty()) {
         List<LivingEntity> foes = new ArrayList<>();

         for (int id : vis.taggedIds()) {
            Entity pulse = mc.level.getEntity(id);
            if (pulse instanceof LivingEntity) {
               LivingEntity foe = (LivingEntity)pulse;
               if (foe.isAlive() && foe != p) {
                  foes.add(foe);
               }
            }
         }

         LivingEntity locked = ExoTags.selectPrimary(eye, aimDir, foes);
         if (locked != null) {
            Vec3 centre = locked.getPosition(partialTick).add(0.0, (double)locked.getBbHeight() * 0.5, 0.0);
            Vec3 to = centre.subtract(eye);
            if (to.lengthSqr() > 1.0E-6) {
               aimDir = to.normalize();
            }
         }
      }

      Vec3 reach = eye.add(aimDir.scale(24.0));
      BlockHitResult blocked = mc.level.clip(new ClipContext(eye, reach, Block.COLLIDER, Fluid.NONE, p));
      Vec3 end = blocked.getType() == Type.BLOCK ? blocked.getLocation() : reach;
      Vec3 chest;
      if (p == mc.player && mc.options.getCameraType().isFirstPerson()) {
         double yaw = Math.toRadians((double)p.getViewYRot(partialTick));
         Vec3 fwdH = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
         Vec3 camRight = fwdH.cross(new Vec3(0.0, 1.0, 0.0));
         Vec3 camUp = camRight.cross(aimDir).normalize();
         chest = eye.add(aimDir.scale(0.45)).subtract(camUp.scale(0.32));
      } else {
         double tilt = Math.toRadians(Mth.clamp((double)p.getViewXRot(partialTick) * 0.65, -55.0, 55.0));
         double yaw = Math.toRadians((double)p.getViewYRot(partialTick));
         Vec3 fwdH = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
         chest = eye.add(0.0, -0.87, 0.0).add(fwdH.scale(0.4 * Math.sin(tilt))).add(0.0, 0.4 * Math.cos(tilt), 0.0).add(aimDir.scale(0.2));
      }

      float pulse = 1.0F + 0.18F * (float)Math.sin((double)(((float)p.tickCount + partialTick) * 0.9F));
      poseStack.pushPose();
      poseStack.translate(-cam.x, -cam.y, -cam.z);
      Pose pose = poseStack.last();
      Vector3f a = BeamDraw.vec(chest);
      Vector3f b = BeamDraw.vec(end);
      BeamDraw.beamSegment(pose, vc, a, b, 0.22F * pulse, 1352653055);
      BeamDraw.beamSegment(pose, vc, a, b, 0.121F * pulse, -1866471681);
      BeamDraw.beamSegment(pose, vc, a, b, 0.0484F * pulse, -386860289);
      Vec3 mouth = chest.add(aimDir.scale(0.5));
      BeamDraw.beamSegment(pose, vc, a, BeamDraw.vec(mouth), 0.32999998F * pulse, -1866471681);
      float f = 0.17F * pulse;
      BeamDraw.beamSegment(pose, vc, new Vector3f(b.x - f, b.y, b.z), new Vector3f(b.x + f, b.y, b.z), 0.05F, -386860289);
      BeamDraw.beamSegment(pose, vc, new Vector3f(b.x, b.y - f, b.z), new Vector3f(b.x, b.y + f, b.z), 0.05F, -386860289);
      BeamDraw.beamSegment(pose, vc, new Vector3f(b.x, b.y, b.z - f), new Vector3f(b.x, b.y, b.z + f), 0.05F, -386860289);
      poseStack.popPose();
   }
}
