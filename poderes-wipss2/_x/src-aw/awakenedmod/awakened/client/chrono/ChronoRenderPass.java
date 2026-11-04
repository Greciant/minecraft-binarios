package com.awakenedmod.awakened.client.chrono;

import com.awakenedmod.awakened.client.chrono.ending.FilmBurnEnding;
import com.awakenedmod.awakened.client.chrono.ending.HeldImpactLook;
import com.awakenedmod.awakened.client.chrono.ending.StopDraw;
import com.awakenedmod.awakened.client.compat.ShaderPacks;
import com.awakenedmod.awakened.client.render.ChronoGlass;
import com.awakenedmod.awakened.client.render.FxLayers;
import com.awakenedmod.awakened.client.render.NegativeLayers;
import com.awakenedmod.awakened.core.net.ChronoFxPayload;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage;

public final class ChronoRenderPass {
   private ChronoRenderPass() {
   }

   public static void register(IEventBus gameBus) {
      ChronoNegative.preloadLabKeys();
      ChronoTimeStop.preloadLabKeys();
      ChronoGlass.preloadLabKeys();
      ChronoFlicker.preloadLabKeys();
      ChronoRewindLook.preloadLabKeys();
      gameBus.addListener(event -> {
         Stage stage = ShaderPacks.active() ? Stage.AFTER_TRANSLUCENT_BLOCKS : Stage.AFTER_LEVEL;
         if (event.getStage() == stage) {
            draw(event);
         }
      });
   }

   private static void draw(RenderLevelStageEvent event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && !ChronoFxClient.effects().isEmpty()) {
         Vec3 cam = event.getCamera().getPosition();
         float partialTick = ChronoFxClient.partialTick();
         BufferSource buffers = mc.renderBuffers().bufferSource();
         boolean negatives = NegativeLayers.available();
         PoseStack world;
         if (negatives) {
            world = new PoseStack();
            world.mulPose(event.getModelViewMatrix());
            negatives(world, buffers, cam, partialTick);
         } else {
            world = event.getPoseStack();
         }

         for (ChronoGlass.Layer layer : ChronoGlass.Layer.values()) {
            VertexConsumer vc = FxLayers.begin(buffers, layer.fx());
            Pose pose = world.last();
            StopDraw d = new StopDraw(pose, vc, layer, cam, partialTick);

            for (ChronoFxClient.Effect e : ChronoFxClient.effects()) {
               if (e.kind == ChronoFxPayload.Kind.TIME_STOP) {
                  glass(d, e);
               } else if (e.kind == ChronoFxPayload.Kind.HELD_IMPACT) {
                  HeldImpactLook.draw(d, e);
               } else if (e.kind == ChronoFxPayload.Kind.FLICKER) {
                  ChronoFlicker.draw(pose, vc, layer, e, mc.level, cam, partialTick);
               }
            }

            FxLayers.end(buffers, layer.fx());
         }

         ChronoRewindLook.draw(world, buffers, cam, partialTick);
         buffers.endBatch();
      }
   }

   private static void negatives(PoseStack world, BufferSource buffers, Vec3 cam, float partialTick) {
      PoseStack view = new PoseStack();
      boolean shellParity = false;

      for (ChronoFxClient.Effect e : ChronoFxClient.effects()) {
         if (e.kind == ChronoFxPayload.Kind.TIME_STOP && ChronoNegative.draw(world, buffers, e, cam, partialTick)) {
            shellParity = !shellParity;
         }
      }

      ChronoFxClient.Effect governing = ChronoView.governing();
      boolean flip = governing != null && !ChronoView.noNegative() && ChronoTimeStop.flipInverted(governing.age(partialTick));
      boolean negativeFrame = flip || ChronoRewindScreen.negativeFrame(partialTick);
      if (shellParity != negativeFrame) {
         ChronoNegative.screenQuad(view, buffers);
      }

      buffers.endBatch(NegativeLayers.triangles());
      buffers.endBatch(NegativeLayers.trianglesNoDepth());
   }

   private static void glass(StopDraw d, ChronoFxClient.Effect e) {
      if (!e.ended() && e.resolveAge(d.partialTick()) < 0.0F) {
         FilmBurnEnding.prepare(e);
         ChronoGlass.dome(d.pose(), d.vc(), d.layer(), d.rel(e.pos), e.radius, ChronoTimeStop.dome(e.age(d.partialTick())));
      } else {
         FilmBurnEnding.world(d, e);
      }
   }
}
