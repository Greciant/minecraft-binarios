package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.entity.projectile.KineticProjectile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class IceShardRenderer extends EntityRenderer<KineticProjectile> {
   private static final float LENGTH = 3.4F;
   private static final float RADIUS = 0.1F;
   private static final float POINT_AT = 0.6F;
   private static final float BUTT_LENGTH = 0.45F;
   private static final float BUTT_POINT_AT = 0.3F;
   private static final float TAIL = 2.2F;
   private static final float[][] GHOSTS = new float[][]{{0.9F, 110.0F}, {1.8F, 55.0F}};

   public IceShardRenderer(Context ctx) {
      super(ctx);
   }

   public ResourceLocation getTextureLocation(KineticProjectile entity) {
      return null;
   }

   public boolean shouldRender(KineticProjectile entity, Frustum frustum, double camX, double camY, double camZ) {
      return true;
   }

   public void render(KineticProjectile entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
      super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
      Vec3 v = entity.getDeltaMovement();
      draw(
         poseStack,
         new Vector3f((float)v.x, (float)v.y, (float)v.z),
         (float)entity.tickCount + partialTick,
         entity.getId(),
         LevelRenderer.getLightColor(entity.level(), entity.blockPosition()),
         (pose, writer) -> writer.write(pose.last(), buffers.getBuffer(RenderType.translucentMovingBlock()))
      );
   }

   private static void draw(PoseStack poseStack, Vector3f velocity, float ageInTicks, int seed, int light, IceShardRenderer.GeometrySubmit geometry) {
      poseStack.pushPose();
      if (velocity.lengthSquared() > 1.0E-6F) {
         poseStack.mulPose(new Quaternionf().rotateTo(new Vector3f(0.0F, 1.0F, 0.0F), new Vector3f(velocity).normalize()));
      }

      poseStack.mulPose(new Quaternionf().rotateY(ageInTicks * 0.35F));
      poseStack.translate(0.0F, -2.2F, 0.0F);
      TextureAtlasSprite body = CrystalMesh.sprite(Blocks.PACKED_ICE);
      TextureAtlasSprite tip = CrystalMesh.sprite(Blocks.ICE);
      geometry.submit(poseStack, (pose, vc) -> {
         javelin(pose, vc, body, tip, seed, light, 255, 0.0F);

         for (float[] ghost : GHOSTS) {
            javelin(pose, vc, body, tip, seed, light, (int)ghost[1], ghost[0]);
         }
      });
      poseStack.popPose();
   }

   private static void javelin(Pose base, VertexConsumer vc, TextureAtlasSprite body, TextureAtlasSprite tip, int seed, int light, int alpha, float back) {
      PoseStack local = new PoseStack();
      local.last().pose().set(base.pose());
      local.last().normal().set(base.normal());
      local.translate(0.0F, -back, 0.0F);
      CrystalMesh.emit(local.last(), vc, body, tip, 3.4F, 0.1F, 0.6F, (long)seed, light, alpha);
      local.mulPose(new Quaternionf().rotateZ((float) Math.PI));
      CrystalMesh.emit(local.last(), vc, body, tip, 0.45F, 0.1F, 0.3F, (long)seed, light, alpha);
   }

   @FunctionalInterface
   private interface GeometrySubmit {
      void submit(PoseStack var1, IceShardRenderer.GeometryWriter var2);
   }

   @FunctionalInterface
   private interface GeometryWriter {
      void write(Pose var1, VertexConsumer var2);
   }
}
