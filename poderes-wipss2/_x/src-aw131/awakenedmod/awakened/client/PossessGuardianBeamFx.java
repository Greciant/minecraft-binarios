package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.client.render.BeamDraw;
import com.awakenedmod.awakened.client.render.FxLayer;
import com.awakenedmod.awakened.client.render.FxLayers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage;
import org.joml.Vector3f;

public final class PossessGuardianBeamFx {
   private static final double FP_FORWARD = 0.5;
   private static final float WIDTH = 0.16F;
   private static final int GLOW_TEAL = 1347084760;
   private static final int BODY_TEAL = -1874140712;
   private static final int CORE_TEAL = -388562949;
   private static final int GLOW_ELDER = 1352368084;
   private static final int BODY_ELDER = -1868857388;
   private static final int CORE_ELDER = -387523585;

   private PossessGuardianBeamFx() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(event -> {
         if (event.getStage() == Stage.AFTER_TRANSLUCENT_BLOCKS) {
            draw(event);
         }
      });
   }

   private static void draw(RenderLevelStageEvent event) {
      if (PossessCamera.isActive()) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.level != null && mc.player != null && mc.options.getCameraType().isFirstPerson()) {
            if (PossessCamera.piloted() instanceof Guardian guardian && guardian.hasActiveAttackTarget()) {
               LivingEntity target = guardian.getActiveAttackTarget();
               if (target == null) {
                  return;
               }

               float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
               Vec3 cam = event.getCamera().getPosition();
               Vec3 eye = guardian.getEyePosition(partial);
               Vec3 origin = eye.add(guardian.getViewVector(partial).scale(0.5));
               Vec3 end = target.getPosition(partial).add(0.0, (double)target.getBbHeight() * 0.5, 0.0);
               boolean elder = guardian instanceof ElderGuardian;
               int glow = elder ? 1352368084 : 1347084760;
               int body = elder ? -1868857388 : -1874140712;
               int core = elder ? -387523585 : -388562949;
               float pulse = 1.0F + 0.18F * (float)Math.sin((double)(((float)guardian.tickCount + partial) * 0.9F));
               PoseStack poseStack = event.getPoseStack();
               BufferSource buffers = mc.renderBuffers().bufferSource();
               VertexConsumer vc = FxLayers.begin(buffers, FxLayer.LIGHT);
               poseStack.pushPose();
               poseStack.translate(-cam.x, -cam.y, -cam.z);
               Pose pose = poseStack.last();
               Vector3f a = BeamDraw.vec(origin);
               Vector3f b = BeamDraw.vec(end);
               BeamDraw.beamSegment(pose, vc, a, b, 0.16F * pulse, glow);
               BeamDraw.beamSegment(pose, vc, a, b, 0.08F * pulse, body);
               BeamDraw.beamSegment(pose, vc, a, b, 0.032F * pulse, core);
               poseStack.popPose();
               buffers.endBatch();
               return;
            }
         }
      }
   }
}
