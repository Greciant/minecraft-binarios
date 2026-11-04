package com.awakenedmod.awakened.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

final class VfxDraw {
   private VfxDraw() {
   }

   static void additive(PoseStack poseStack, VfxDraw.Sink sink, VfxDraw.Geometry g) {
      custom(poseStack, sink, sink.light, g);
   }

   static void translucent(PoseStack poseStack, VfxDraw.Sink sink, VfxDraw.Geometry g) {
      custom(poseStack, sink, FxLayer.PAINT, g);
   }

   static void flame(PoseStack poseStack, VfxDraw.Sink sink, VfxDraw.Geometry g) {
      bodyThenGlow(
         poseStack,
         sink,
         FxLayer.PAINT,
         (pose, vc) -> VoxelFlameDraw.pass(VoxelFlameDraw.Pass.BODY, pose, vc, g),
         (pose, vc) -> VoxelFlameDraw.pass(VoxelFlameDraw.Pass.GLOW, pose, vc, g)
      );
   }

   static void bodyThenGlow(PoseStack poseStack, VfxDraw.Sink sink, VfxDraw.Geometry body, VfxDraw.Geometry glow) {
      bodyThenGlow(poseStack, sink, FxLayer.PAINT, body, glow);
   }

   static void bodyThenGlow(PoseStack poseStack, VfxDraw.Sink sink, FxLayer bodyLayer, VfxDraw.Geometry body, VfxDraw.Geometry glow) {
      body.build(poseStack.last(), FxLayers.begin(sink.buffers, bodyLayer));
      glow.build(poseStack.last(), FxLayers.begin(sink.buffers, sink.light));
   }

   static void custom(PoseStack poseStack, VfxDraw.Sink sink, FxLayer layer, VfxDraw.Geometry g) {
      g.build(poseStack.last(), FxLayers.begin(sink.buffers, layer));
   }

   static void vanillaLayer(PoseStack poseStack, VfxDraw.Sink sink, RenderType type, VfxDraw.Geometry g) {
      g.build(poseStack.last(), sink.buffers.getBuffer(type));
   }

   static int ia(float baseAlpha, float intensity) {
      return (int)(baseAlpha * intensity);
   }

   @FunctionalInterface
   interface Geometry {
      void build(Pose var1, VertexConsumer var2);
   }

   static final class Sink {
      FxLayer light = FxLayer.LIGHT;
      final MultiBufferSource buffers;

      Sink(MultiBufferSource buffers) {
         this.buffers = buffers;
      }
   }
}
