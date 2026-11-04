package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.client.beam.PyroBeamSim;
import com.awakenedmod.awakened.client.beam.PyroBeamStyle;
import com.awakenedmod.awakened.client.render.VoxelFlameDraw;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage;
import org.joml.Vector3f;

public final class PyroBeamRenderer {
   private PyroBeamRenderer() {
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
      if (mc.level != null && mc.player != null) {
         Collection<PyroBeamSim.Buffer> buffers = PyroBeamSim.buffers();
         if (!buffers.isEmpty()) {
            float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
            Vec3 cam = event.getCamera().getPosition();
            PoseStack poseStack = event.getPoseStack();
            BufferSource bufferSource = mc.renderBuffers().bufferSource();
            Vector3f[] camAxes = VoxelFlameDraw.cameraAxes();
            float time = (float)mc.player.tickCount + partialTick;
            boolean firstPersonCam = mc.options.getCameraType().isFirstPerson();
            poseStack.pushPose();
            poseStack.translate(-cam.x, -cam.y, -cam.z);
            Pose pose = poseStack.last();
            List<PyroBeamRenderer.Prepared> prepared = new ArrayList<>(buffers.size());

            for (PyroBeamSim.Buffer b : buffers) {
               LivingEntity p = mc.level.getEntity(b.entityId()) instanceof LivingEntity pl ? pl : null;
               boolean channeling = b.channeling() && p != null;
               boolean ownView = p == mc.player && firstPersonCam;
               PyroSourceAnchors.Nozzles nozzles = null;
               if (channeling) {
                  Vec3 eye = p.getEyePosition(partialTick);
                  nozzles = b.source().palms(p, partialTick, eye, ownView);
               }

               float blueFraction = p != null ? PyroClientHeat.blueFraction(p) : b.blueFraction();
               prepared.add(new PyroBeamRenderer.Prepared(b, channeling, nozzles, VoxelFlameDraw.blend(blueFraction), ownView && b.source().fadesNearCamera()));
            }

            VoxelFlameDraw.twoPass(
               bufferSource,
               vc -> {
                  for (PyroBeamRenderer.Prepared f : prepared) {
                     PyroBeamSim.Buffer bx = f.buffer();
                     bx.source()
                        .style()
                        .draw(
                           new PyroBeamStyle.Frame(
                              pose, vc, partialTick, f.channeling(), f.nozzles(), f.palette(), time, bx.entityId(), camAxes, cam, f.nearFade()
                           ),
                           bx
                        );
                  }
               }
            );
            poseStack.popPose();
            bufferSource.endBatch();
         }
      }
   }

   private static record Prepared(
      PyroBeamSim.Buffer buffer, boolean channeling, PyroSourceAnchors.Nozzles nozzles, VoxelFlameDraw.Palette palette, boolean nearFade
   ) {
   }
}
