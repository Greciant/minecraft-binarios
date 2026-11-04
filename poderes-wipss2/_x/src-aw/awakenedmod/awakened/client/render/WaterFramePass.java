package com.awakenedmod.awakened.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage;

public final class WaterFramePass {
   private static final int QUEUE_CAP = 4096;
   private static final List<WaterFramePass.Drawer> DRAWERS = new ArrayList<>();
   private static final List<WaterFramePass.Queued> QUEUE = new ArrayList<>();
   private static final List<WaterFramePass.Queued> FOAM = new ArrayList<>();
   private static boolean listening;
   private static boolean running;

   private WaterFramePass() {
   }

   public static void register(IEventBus gameBus, WaterFramePass.Drawer drawer) {
      DRAWERS.add(drawer);
      install(gameBus);
   }

   public static void install(IEventBus gameBus) {
      if (!listening) {
         listening = true;
         gameBus.addListener(event -> {
            if (event.getStage() == Stage.AFTER_TRANSLUCENT_BLOCKS) {
               run(event);
            }
         });
      }
   }

   public static void queue(PoseStack poseStack, WaterMesh.Shape shape) {
      if (QUEUE.size() >= 4096) {
         QUEUE.clear();
      }

      QUEUE.add(new WaterFramePass.Queued(poseStack.last().copy(), shape));
   }

   public static void foam(Pose pose, VertexConsumer fallback, WaterMesh.Shape shape) {
      if (!running) {
         shape.build(pose, fallback);
      } else {
         if (FOAM.size() >= 4096) {
            FOAM.clear();
         }

         FOAM.add(new WaterFramePass.Queued(pose.copy(), shape));
      }
   }

   private static void run(RenderLevelStageEvent event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null) {
         QUEUE.clear();
      } else {
         float pt = event.getPartialTick().getGameTimeDeltaPartialTick(false);
         Vec3 cam = event.getCamera().getPosition();
         BufferSource buffers = mc.renderBuffers().bufferSource();
         PoseStack poseStack = event.getPoseStack();
         running = true;

         try {
            for (Player p : mc.level.players()) {
               for (WaterFramePass.Drawer drawer : DRAWERS) {
                  drawer.draw(mc.level, p, pt, poseStack, buffers, cam);
               }
            }

            if (!QUEUE.isEmpty()) {
               VertexConsumer vc = buffers.getBuffer(ModRenderTypes.WATER_TRANSLUCENT);

               for (WaterFramePass.Queued q : QUEUE) {
                  q.shape().build(q.pose(), vc);
               }

               QUEUE.clear();
            }

            buffers.endBatch(ModRenderTypes.WATER_TRANSLUCENT);
            if (!FOAM.isEmpty()) {
               VertexConsumer vc = buffers.getBuffer(ModRenderTypes.WATER_FOAM);

               for (WaterFramePass.Queued q : FOAM) {
                  q.shape().build(q.pose(), vc);
               }

               FOAM.clear();
               buffers.endBatch(ModRenderTypes.WATER_FOAM);
            }
         } finally {
            running = false;
         }
      }
   }

   @FunctionalInterface
   public interface Drawer {
      void draw(ClientLevel var1, Player var2, float var3, PoseStack var4, MultiBufferSource var5, Vec3 var6);
   }

   private static record Queued(Pose pose, WaterMesh.Shape shape) {
   }
}
