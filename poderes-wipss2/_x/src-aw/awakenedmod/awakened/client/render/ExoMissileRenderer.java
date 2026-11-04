package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.entity.projectile.KineticProjectile;
import com.awakenedmod.awakened.core.AwId;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class ExoMissileRenderer extends EntityRenderer<KineticProjectile> {
   private static final ResourceLocation TEXTURE = AwId.of("textures/entity/exosavant_missile.png");
   private static final float SCALE = 2.2F;
   private static final float ROLL_PER_TICK = 9.0F;
   private final ModelPart missile;

   public ExoMissileRenderer(Context ctx) {
      super(ctx);
      this.missile = ctx.bakeLayer(ExoMissileModel.LAYER).getChild("RightMissile");
   }

   public ResourceLocation getTextureLocation(KineticProjectile entity) {
      return TEXTURE;
   }

   public boolean shouldRender(KineticProjectile entity, Frustum frustum, double camX, double camY, double camZ) {
      return true;
   }

   public void render(KineticProjectile entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
      super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
      Vec3 v = entity.getDeltaMovement();
      Vector3f velocity = new Vector3f((float)v.x, (float)v.y, (float)v.z);
      float ageInTicks = (float)entity.tickCount + partialTick;
      int light = LevelRenderer.getLightColor(entity.level(), entity.blockPosition());
      this.draw(
         poseStack,
         velocity,
         ageInTicks,
         (pose, l) -> this.missile.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), l, OverlayTexture.NO_OVERLAY),
         light
      );
   }

   private void draw(PoseStack poseStack, Vector3f velocity, float ageInTicks, ExoMissileRenderer.ModelSubmit submit, int light) {
      poseStack.pushPose();
      if (velocity.lengthSquared() > 1.0E-6F) {
         poseStack.mulPose(new Quaternionf().rotateTo(new Vector3f(0.0F, 0.0F, -1.0F), new Vector3f(velocity).normalize()));
      }

      poseStack.mulPose(Axis.ZP.rotationDegrees(ageInTicks * 9.0F));
      poseStack.scale(2.2F, 2.2F, 2.2F);
      poseStack.translate(-0.03125F, -1.5F, -0.0F);
      submit.submit(poseStack, light);
      poseStack.popPose();
   }

   @FunctionalInterface
   private interface ModelSubmit {
      void submit(PoseStack var1, int var2);
   }
}
