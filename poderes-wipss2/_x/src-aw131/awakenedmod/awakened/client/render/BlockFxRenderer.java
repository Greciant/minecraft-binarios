package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.vfx.BlockFxEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;

public final class BlockFxRenderer extends EntityRenderer<BlockFxEntity> {
   private final BlockRenderDispatcher blockRenderer;
   private static final double LIGHT_ABOVE_TOP = 0.05;
   private static final float HEAVE_UP = 0.3F;
   private static final float UNDER_LIGHT_SPREAD = 0.35F;
   private static final float UNDER_LIGHT_ALPHA = 150.0F;
   private static final float UNDER_LIGHT_LIFT = 0.02F;
   private static final int UNDER_LIGHT_SEGS = 10;
   private static final double INSIDE_MARGIN = 0.2;
   private static final int NO_TINT = 16777215;
   private static final boolean SODIUM_PRESENT = ModList.get().isLoaded("sodium");

   public BlockFxRenderer(Context ctx) {
      super(ctx);
      this.blockRenderer = ctx.getBlockRenderDispatcher();
   }

   public ResourceLocation getTextureLocation(BlockFxEntity entity) {
      return null;
   }

   public boolean shouldRender(BlockFxEntity entity, Frustum frustum, double camX, double camY, double camZ) {
      return true;
   }

   public void render(BlockFxEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
      super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
      if (!cameraInsideModel(
         Minecraft.getInstance().gameRenderer.getMainCamera().getPosition(),
         entity.anchorId(),
         entity.getX(),
         entity.getY(),
         entity.getZ(),
         entity.scaleX(),
         entity.scaleY(),
         entity.scaleZ(),
         entity.anchorBottom()
      )) {
         BlockState blockState = entity.blockState();
         int lifetime = entity.lifetime();
         float ageInTicks = (float)entity.tickCount + partialTick - (float)entity.delay();
         if (!(ageInTicks < 0.0F)) {
            int light = lightAt(entity);
            if (entity.alpha() < 1.0F) {
               int tint = entity.tint();
               float a = pulsedAlpha(entity.alpha(), ageInTicks, tint);
               poseStack.pushPose();
               applyBox(poseStack, entity.yawDeg(), 0.0F, entity.scaleX(), entity.scaleY(), entity.scaleZ(), 1.0F, entity.anchorBottom());
               this.blockRenderer
                  .getModelRenderer()
                  .renderModel(
                     poseStack.last(),
                     ghostConsumer(buffers.getBuffer(RenderType.translucentMovingBlock()), a, tint),
                     blockState,
                     this.blockRenderer.getBlockModel(blockState),
                     1.0F,
                     1.0F,
                     1.0F,
                     liftedLight(light),
                     OverlayTexture.NO_OVERLAY
                  );
               poseStack.popPose();
            } else {
               float life = visibleLife(ageInTicks, lifetime, entity.delay());
               this.draw(
                  poseStack,
                  life,
                  ageInTicks,
                  entity.yawDeg() + spin(entity, partialTick),
                  entity.scaleX(),
                  entity.scaleY(),
                  entity.scaleZ(),
                  entity.growTicks(),
                  entity.anchorBottom(),
                  entity.heaveRise(),
                  entity.heaveTilt(),
                  (pose, l) -> this.blockRenderer.renderSingleBlock(blockState, pose, buffers, l, OverlayTexture.NO_OVERLAY),
                  light
               );
               underLight(
                  poseStack,
                  new VfxDraw.Sink(buffers),
                  life,
                  entity.scaleX(),
                  entity.scaleY(),
                  entity.scaleZ(),
                  entity.heaveRise(),
                  entity.glowRgb(),
                  entity.glowStrength()
               );
            }
         }
      }
   }

   private static float spin(BlockFxEntity entity, float partialTick) {
      return !entity.launched() ? 0.0F : entity.tumble() * ((float)entity.flightTicks() + (entity.landed() ? 0.0F : partialTick));
   }

   private static int lightAt(BlockFxEntity entity) {
      int own = LevelRenderer.getLightColor(entity.level(), entity.blockPosition());
      double topFace = entity.anchorBottom() ? entity.getY() + (double)entity.scaleY() : entity.getY() + (double)entity.scaleY() * 0.5;
      int top = LevelRenderer.getLightColor(entity.level(), BlockPos.containing(entity.getX(), topFace + 0.05, entity.getZ()));
      return brighter(own, top);
   }

   private static int brighter(int a, int b) {
      return Math.max(a & 65535, b & 65535) | Math.max(a >>> 16, b >>> 16) << 16;
   }

   private static float visibleLife(float age, int lifetime, int delay) {
      int visible = lifetime - delay;
      return visible <= 0 ? 0.0F : Mth.clamp(age / (float)visible, 0.0F, 1.0F);
   }

   private void draw(
      PoseStack poseStack,
      float life,
      float ageInTicks,
      float yawDeg,
      float sx,
      float sy,
      float sz,
      int growTicks,
      boolean anchorBottom,
      float heaveRise,
      float heaveTilt,
      BlockFxRenderer.BlockSubmit submit,
      int light
   ) {
      float grow;
      if (growTicks > 0) {
         float t = Mth.clamp(ageInTicks / (float)growTicks, 0.0F, 1.0F);
         grow = 1.0F - (1.0F - t) * (1.0F - t);
      } else {
         grow = Mth.clamp(ageInTicks / 2.0F, 0.2F, 1.0F);
      }

      float melt = life < 0.8F ? 1.0F : 1.0F - (life - 0.8F) / 0.2F;
      float k = grow * melt;
      if (!(k <= 0.01F)) {
         poseStack.pushPose();
         float roll = 0.0F;
         if (heaveRise != 0.0F || heaveTilt != 0.0F) {
            float h = heave(life);
            poseStack.translate(0.0, (double)(heaveRise * h), 0.0);
            roll = heaveTilt * h;
         }

         applyBox(poseStack, yawDeg, roll, sx, sy, sz, k, anchorBottom);
         submit.submit(poseStack, light);
         poseStack.popPose();
      }
   }

   private static void underLight(PoseStack poseStack, VfxDraw.Sink sink, float life, float sx, float sy, float sz, float heaveRise, int rgb, float strength) {
      if (rgb != 0 && heaveRise != 0.0F && !(strength <= 0.0F)) {
         int a = Math.round(150.0F * heave(life) * Math.min(3.0F, strength));
         if (a > 2) {
            int centre = VfxColor.argb(Math.min(255, a), rgb);
            int rim = VfxColor.argb(0, rgb);
            float rx = sx * 0.5F + 0.35F;
            float rz = sz * 0.5F + 0.35F;
            float y = sy + 0.02F;
            VfxDraw.additive(
               poseStack,
               sink,
               (pose, vc) -> {
                  for (int i = 0; i < 10; i++) {
                     float a0 = (float)i * 6.2832F / 10.0F;
                     float a1 = (float)(i + 1) * 6.2832F / 10.0F;
                     VelocityStreaks.tri(
                        pose, vc, 0.0F, y, 0.0F, centre, Mth.cos(a0) * rx, y, Mth.sin(a0) * rz, rim, Mth.cos(a1) * rx, y, Mth.sin(a1) * rz, rim, true
                     );
                  }
               }
            );
         }
      }
   }

   private static float heave(float life) {
      if (life < 0.3F) {
         float t = life / 0.3F;
         return 1.0F - (1.0F - t) * (1.0F - t);
      } else {
         float t = (life - 0.3F) / 0.7F;
         return 1.0F - t * t;
      }
   }

   private static void applyBox(PoseStack poseStack, float yawDeg, float rollDeg, float sx, float sy, float sz, float k, boolean anchorBottom) {
      if (yawDeg != 0.0F) {
         poseStack.mulPose(Axis.YP.rotationDegrees(yawDeg));
      }

      if (rollDeg != 0.0F) {
         poseStack.mulPose(Axis.ZP.rotationDegrees(rollDeg));
      }

      poseStack.scale(sx * k, sy * k, sz * k);
      poseStack.translate(-0.5, anchorBottom ? 0.0 : -0.5, -0.5);
   }

   private static boolean cameraInsideModel(Vec3 cam, int anchorId, double ex, double ey, double ez, float sx, float sy, float sz, boolean anchorBottom) {
      if (anchorId < 0) {
         return false;
      } else {
         double hx = (double)sx / 2.0 + 0.2;
         double hz = (double)sz / 2.0 + 0.2;
         double yLo = (anchorBottom ? ey : ey - (double)sy / 2.0) - 0.2;
         double yHi = (anchorBottom ? ey + (double)sy : ey + (double)sy / 2.0) + 0.2;
         return cam.x >= ex - hx && cam.x <= ex + hx && cam.z >= ez - hz && cam.z <= ez + hz && cam.y >= yLo && cam.y <= yHi;
      }
   }

   private static GhostVertexConsumer ghostConsumer(VertexConsumer inner, float alpha, int tint) {
      float red = (float)(tint >> 16 & 0xFF) / 255.0F;
      float green = (float)(tint >> 8 & 0xFF) / 255.0F;
      float blue = (float)(tint & 0xFF) / 255.0F;
      return SODIUM_PRESENT ? SodiumGhostFactory.create(inner, alpha, red, green, blue) : new GhostVertexConsumer(inner, alpha, red, green, blue);
   }

   private static float pulsedAlpha(float baseAlpha, float ageInTicks, int tint) {
      return tint == 16777215 ? baseAlpha * (0.85F + 0.15F * Mth.sin(ageInTicks * 0.16F)) : baseAlpha * (0.75F + 0.25F * Mth.sin(ageInTicks * 0.31F));
   }

   private static int liftedLight(int packedLight) {
      return LightTexture.pack(Math.max(LightTexture.block(packedLight), 9), LightTexture.sky(packedLight));
   }

   @FunctionalInterface
   private interface BlockSubmit {
      void submit(PoseStack var1, int var2);
   }
}
