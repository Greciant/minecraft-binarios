package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.client.VelocityLaneTransitClient;
import com.awakenedmod.awakened.content.velocity.VelocityPalette;
import com.awakenedmod.awakened.content.velocity.VelocityVisualState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class VelocityTunnelTubeRenderer {
   private static final float FORWARD = -1.0F;
   private static final int RING_NEAR_BLUE = 1196232;
   private static final int RING_FAR_BLUE = 396856;
   private static final int WALL_BLUE = 7320831;
   private static final int CORE_WHITE = 16055039;
   private static final int THROAT_BLUE = 728942;
   private static final int RINGS = 12;
   private static final float NEAR = 1.5F;
   private static final float FAR = 14.0F;
   private static final float RADIUS_NEAR = 2.4F;
   private static final float RADIUS_FAR = 1.3F;
   private static final float RING_BAND = 0.1F;
   private static final float RING_ALPHA = 0.42F;
   private static final int RING_SEGS = 28;
   private static final float RING_SPEED = 5.0F;
   private static final int WALL_LINES = 16;
   private static final int LIGHTNING_LINES = 12;
   private static final float LINE_LEN_MIN = 2.0F;
   private static final float LINE_LEN_MAX = 5.0F;
   private static final float LINE_SPEED_MIN = 5.0F;
   private static final float LINE_SPEED_MAX = 8.0F;
   private static final float LINE_W = 0.025F;
   private static final float WALL_ALPHA = 120.0F;
   private static final float LIGHTNING_ALPHA = 200.0F;
   private static final float LINE_INSET = 0.92F;
   private static final float VANISH_Z = 15.0F;
   private static final float VANISH_CORE_R = 0.9F;
   private static final float VANISH_HALO_R = 2.2F;
   private static final float VANISH_CORE_ALPHA = 0.75F;
   private static final float VANISH_HALO_ALPHA = 0.35F;
   private static final float VANISH_BREATH = 0.12F;
   private static final float VANISH_BREATH_TICKS = 30.0F;
   private static final float THROAT_ALPHA = 0.5F;
   private static final int VANISH_SEGS = 24;
   private static final float ROLL_PER_DEG = 0.35F;
   private static final float ROLL_MAX_DEG = 12.0F;
   private static final float ROLL_EASE = 0.2F;
   private static float roll;
   private static float lastYaw;
   private static boolean yawKnown;

   private VelocityTunnelTubeRenderer() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(VelocityTunnelTubeRenderer::onClientTick);
      gameBus.addListener(event -> {
         if (event.getStage() == Stage.AFTER_TRANSLUCENT_BLOCKS) {
            draw(event);
         }
      });
   }

   private static void onClientTick(Post event) {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer local = mc.player;
      if (local == null || !inside(local)) {
         roll = 0.0F;
         yawKnown = false;
      } else if (!mc.isPaused()) {
         float yaw = local.getYRot();
         float delta = yawKnown ? Mth.wrapDegrees(yaw - lastYaw) : 0.0F;
         lastYaw = yaw;
         yawKnown = true;
         float target = Mth.clamp(delta * 0.35F, -12.0F, 12.0F);
         roll = roll + (target - roll) * 0.2F;
      }
   }

   private static boolean inside(LocalPlayer local) {
      return VelocityVisualState.of(local).tunnel() || VelocityLaneTransitClient.active();
   }

   private static void draw(RenderLevelStageEvent event) {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer local = mc.player;
      if (mc.level != null && local != null && inside(local)) {
         float eased = VelocityScreenFx.tunnel();
         if (!(eased <= 0.01F)) {
            PoseStack poseStack = event.getPoseStack();
            if (poseStack != null) {
               float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
               Quaternionf orientation = event.getCamera().rotation();
               float time = (float)mc.level.getGameTime() + partialTick;
               VelocityPalette palette = VelocityPalette.of(local);
               BufferSource buffers = mc.renderBuffers().bufferSource();
               VfxDraw.Sink sink = new VfxDraw.Sink(buffers);
               poseStack.pushPose();
               poseStack.mulPose(orientation);
               poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
               if (VelocityLaneTransitClient.active()) {
                  VelocityLaneSceneRenderer.draw(poseStack, sink, palette, partialTick, eased);
               } else {
                  tube(poseStack, sink, palette, time, eased);
               }

               poseStack.popPose();
               buffers.endBatch();
            }
         }
      }
   }

   private static void tube(PoseStack poseStack, VfxDraw.Sink sink, VelocityPalette palette, float time, float env) {
      VfxDraw.custom(poseStack, sink, FxLayer.PAINT_THROUGH_WALLS, (pose, vc) -> {
         throat(pose, vc, time, env);
         vanishingPoint(pose, vc, time, env);
         rings(pose, vc, time, env);
      });
      VfxDraw.custom(poseStack, sink, FxLayer.LIGHT_THROUGH_WALLS, (pose, vc) -> {
         lines(pose, vc, time, env, 16, 0, 7320831, 7320831, 120.0F);
         lines(pose, vc, time, env, 12, 100, palette.hot(), palette.body(), 200.0F);
      });
   }

   private static float radiusAt(float d) {
      return Mth.lerp(Mth.clamp((d - 1.5F) / 12.5F, 0.0F, 1.0F), 2.4F, 1.3F);
   }

   private static float breath(float time) {
      return 1.0F + 0.12F * Mth.sin(time * (float) (Math.PI * 2) / 30.0F);
   }

   private static void throat(Pose pose, VertexConsumer vc, float time, float env) {
      int col = VfxColor.argb(Math.round(127.5F * env), 728942);
      disc(pose, vc, 15.5F, 1.3F * breath(time), col, col, false);
   }

   private static void rings(Pose pose, VertexConsumer vc, float time, float env) {
      float span = 12.5F;
      float spacing = span / 12.0F;
      float scroll = time * 5.0F % spacing;

      for (int i = 0; i < 12; i++) {
         float d = 1.5F + (((float)i * spacing - scroll) % span + span) % span;
         float depth = (d - 1.5F) / span;
         float r = radiusAt(d);
         float fade = Math.min(1.0F, Math.min(depth / 0.12F, (1.0F - depth) / 0.25F));
         int col = VfxColor.argb(Math.round(107.1F * env * fade), VelocityPalette.mix(1196232, 396856, depth));
         band(pose, vc, d, r, 0.1F, col);
      }
   }

   private static void lines(Pose pose, VertexConsumer vc, float time, float env, int count, int salt, int headRgb, int tailRgb, float alpha) {
      float span = 12.5F;

      for (int i = 0; i < count; i++) {
         float theta = VelocityStreaks.noise(i, salt, 1) * (float) (Math.PI * 2);
         float len = 2.0F + 3.0F * VelocityStreaks.noise(i, salt, 2);
         float speed = 5.0F + 3.0F * VelocityStreaks.noise(i, salt, 3);
         float cycle = span + len;
         float p = ((time * speed + VelocityStreaks.noise(i, salt, 4) * cycle) % cycle + cycle) % cycle;
         float head = 14.0F - p;
         float tail = head + len;
         if (!(head < 1.5F - len) && !(tail < 1.5F)) {
            float h = Math.max(head, 1.5F);
            float t = Math.min(tail, 14.0F);
            if (!(t <= h)) {
               float inset = 0.92F * (0.9F + 0.1F * VelocityStreaks.noise(i, salt, 5));
               Vector3f from = wallPoint(theta, h, inset);
               Vector3f to = wallPoint(theta, t, inset);
               float born = Math.min(1.0F, p / 1.5F);
               int headCol = VfxColor.argb(Math.round(alpha * env * born), headRgb);
               int tailCol = VfxColor.argb(0, tailRgb);
               VelocityStreaks.streak(pose, vc, from, to, 0.025F, 0.0125F, headCol, tailCol);
            }
         }
      }
   }

   private static void vanishingPoint(Pose pose, VertexConsumer vc, float time, float env) {
      float b = breath(time);
      int halo = VfxColor.argb(Math.round(89.25F * env), 7320831);
      int core = VfxColor.argb(Math.round(191.25F * env), 16055039);
      int clear = VfxColor.argb(0, 7320831);
      disc(pose, vc, 15.0F, 2.2F * b, halo, clear, false);
      disc(pose, vc, 15.0F, 0.9F * b, core, clear, false);
   }

   private static Vector3f wallPoint(float theta, float d, float inset) {
      float r = radiusAt(d) * inset;
      return new Vector3f(Mth.cos(theta) * r, Mth.sin(theta) * r, -1.0F * d);
   }

   private static void band(Pose pose, VertexConsumer vc, float d, float r, float thick, int col) {
      float zNear = -1.0F * (d - thick * 0.5F);
      float zFar = -1.0F * (d + thick * 0.5F);
      Vector3f prevNear = null;
      Vector3f prevFar = null;

      for (int i = 0; i <= 28; i++) {
         float theta = (float)i * (float) (Math.PI * 2) / 28.0F;
         Vector3f near = new Vector3f(Mth.cos(theta) * r, Mth.sin(theta) * r, zNear);
         Vector3f far = new Vector3f(Mth.cos(theta) * r, Mth.sin(theta) * r, zFar);
         if (prevNear != null) {
            VelocityStreaks.quad(pose, vc, prevNear, col, near, col, far, col, prevFar, col, false);
         }

         prevNear = near;
         prevFar = far;
      }
   }

   private static void disc(Pose pose, VertexConsumer vc, float d, float r, int centreCol, int edgeCol, boolean twoSided) {
      Vector3f c = new Vector3f(0.0F, 0.0F, -1.0F * d);
      Vector3f prev = null;

      for (int i = 0; i <= 24; i++) {
         float theta = (float)i * (float) (Math.PI * 2) / 24.0F;
         Vector3f p = new Vector3f(Mth.cos(theta) * r, Mth.sin(theta) * r, -1.0F * d);
         if (prev != null) {
            VelocityStreaks.tri(pose, vc, c, centreCol, prev, edgeCol, p, edgeCol, twoSided);
         }

         prev = p;
      }
   }
}
