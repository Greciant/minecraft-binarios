package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.client.TimelessColdBodies;
import com.awakenedmod.awakened.content.velocity.VelocityPalette;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;

public final class TimelessColdLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
   public static final float OVERLAY_ALPHA_THIRD = 0.35F;
   public static final float OVERLAY_ALPHA_FIFTH = 0.46F;
   public static final int COLD_TINT_THIRD = 9214638;
   public static final int COLD_TINT_FIFTH = 7768230;
   private static final int DIVISOR_THIRD = 3;
   private static final int DIVISOR_FIFTH = 5;
   public static final float RIM_SCALE = 1.045F;
   public static final int RIM_COLOR = 11127272;
   public static final float RIM_ALPHA_THIRD = 0.06F;
   public static final float RIM_ALPHA_FIFTH = 0.09F;
   public static final int ARC_PERIOD_TICKS = 15;
   public static final int ARC_LIFE_TICKS = 4;
   private static final float JITTER_WINDOW_TICKS = 2.0F;
   private static final float FLICKER_MIN = 0.55F;
   private static final float ARC_HALF_WIDTH = 0.022F;
   private static final float ARC_HALO_HALF_WIDTH = 0.06F;
   private static final int ARC_ALPHA = 165;
   private static final int ARC_HALO_ALPHA = 55;
   private static final float ARC_JITTER = 0.16F;
   private static final int ARC_SECTIONS = 3;
   private static final int ARC_SEGS_PER_SECTION = 3;
   private static final float ARC_STANDOFF = 0.03F;
   private static final float ARC_HEIGHT_MIN = 0.08F;
   private static final float ARC_HEIGHT_MAX = 0.95F;
   private static final float ARC_SPAN_MIN = 1.2F;
   private static final float ARC_SPAN_MAX = 2.6F;
   private static final float MODEL_FEET_Y = 1.501F;
   private final LivingEntityRenderer<T, M> renderer;

   public TimelessColdLayer(LivingEntityRenderer<T, M> renderer) {
      super(renderer);
      this.renderer = renderer;
   }

   public static void attach(LivingEntityRenderer<?, ?> renderer) {
      renderer.addLayer(new TimelessColdLayer((LivingEntityRenderer<T, M>)renderer));
   }

   public void render(
      PoseStack poseStack,
      MultiBufferSource buffers,
      int packedLight,
      T entity,
      float limbSwing,
      float limbSwingAmount,
      float partialTick,
      float ageInTicks,
      float netHeadYaw,
      float headPitch
   ) {
      TimelessColdBodies.Cold cold = TimelessColdBodies.of(entity);
      if (cold != null && !entity.isInvisible()) {
         ResourceLocation texture = this.renderer.getTextureLocation(entity);
         float scale = entity.getScale() <= 0.0F ? 1.0F : entity.getScale();
         float width = entity.getBbWidth() / scale;
         float height = entity.getBbHeight() / scale;
         M model = (M)this.getParentModel();
         model.renderToBuffer(poseStack, buffers.getBuffer(RenderType.entityTranslucent(texture)), packedLight, OverlayTexture.NO_OVERLAY, overlayArgb(cold));
         poseStack.pushPose();
         rimTransform(poseStack, height);
         model.renderToBuffer(poseStack, buffers.getBuffer(RenderType.eyes(texture)), packedLight, OverlayTexture.NO_OVERLAY, rimArgb(cold));
         poseStack.popPose();
         arc(poseStack, new VfxDraw.Sink(buffers), cold, cold.seed(), width, height);
      }
   }

   private static float depth(TimelessColdBodies.Cold cold) {
      return Mth.clamp((float)(cold.divisor() - 3) / 2.0F, 0.0F, 1.0F);
   }

   private static int overlayArgb(TimelessColdBodies.Cold cold) {
      float depth = depth(cold);
      float alpha = Mth.lerp(depth, 0.35F, 0.46F) * cold.intensity();
      return VfxColor.argb(Math.round(alpha * 255.0F), VelocityPalette.mix(9214638, 7768230, depth));
   }

   private static int rimArgb(TimelessColdBodies.Cold cold) {
      float alpha = Mth.lerp(depth(cold), 0.06F, 0.09F) * cold.intensity();
      return VfxColor.argb(Math.round(alpha * 255.0F), 11127272);
   }

   private static void rimTransform(PoseStack poseStack, float height) {
      float pivotY = 1.501F - height * 0.5F;
      poseStack.translate(0.0F, pivotY, 0.0F);
      poseStack.scale(1.045F, 1.045F, 1.045F);
      poseStack.translate(0.0F, -pivotY, 0.0F);
   }

   private static void arc(PoseStack poseStack, VfxDraw.Sink sink, TimelessColdBodies.Cold cold, int seed, float width, float height) {
      float clock = TimelessColdBodies.clock();
      float offset = VelocityStreaks.noise(seed, 0, 113) * 15.0F;
      float local = clock + offset;
      int period = (int)(local / 15.0F);
      float age = local - (float)(period * 15);
      if (!(age >= 4.0F)) {
         int window = (int)(clock / 2.0F);
         float flicker = Mth.lerp(VelocityStreaks.noise(seed, window, 114), 0.55F, 1.0F);
         float life = 1.0F - VelocityStreaks.easeOut(age / 4.0F) * 0.7F;
         float bright = flicker * life * cold.intensity();
         int core = VfxColor.argb(Math.round(165.0F * bright), cold.bodyColor());
         int halo = VfxColor.argb(Math.round(55.0F * bright), cold.bodyColor());
         float radius = width * 0.5F + 0.03F;
         float a0 = VelocityStreaks.noise(seed, period, 115) * (float) (Math.PI * 2);
         float span = Mth.lerp(VelocityStreaks.noise(seed, period, 116), 1.2F, 2.6F) * (VelocityStreaks.noise(seed, period, 117) < 0.5F ? 1.0F : -1.0F);
         float h0 = Mth.lerp(VelocityStreaks.noise(seed, period, 118), 0.08F, 0.95F) * height;
         float h1 = Mth.lerp(VelocityStreaks.noise(seed, period, 119), 0.08F, 0.95F) * height;
         Vector3f[] path = new Vector3f[4];

         for (int i = 0; i <= 3; i++) {
            float t = (float)i / 3.0F;
            float a = a0 + span * t;
            float ey = Mth.lerp(t, h0, h1);
            path[i] = new Vector3f(-Mth.cos(a) * radius, 1.501F - ey, Mth.sin(a) * radius);
         }

         float sectionLength = new Vector3f(path[1]).sub(path[0]).length();
         float jitter = sectionLength * 0.16F;
         VfxDraw.additive(poseStack, sink, (pose, vc) -> drawPath(pose, vc, path, jitter, seed ^ window * 40503, core, halo));
      }
   }

   private static void drawPath(Pose pose, VertexConsumer vc, Vector3f[] path, float jitter, int seed, int core, int halo) {
      for (int i = 0; i < path.length - 1; i++) {
         VelocityStreaks.bolt(pose, vc, path[i], path[i + 1], 3, jitter, seed + i * 31, 0.022F, core, 0.06F, halo);
      }
   }
}
