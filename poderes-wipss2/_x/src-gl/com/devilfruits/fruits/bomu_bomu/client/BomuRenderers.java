package com.devilfruits.fruits.bomu_bomu.client;

import com.devilfruits.DevilFruits;
import com.devilfruits.fruits.bomu_bomu.entity.BombBullet;
import com.devilfruits.fruits.bomu_bomu.entity.BoogerBomb;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

final class BomuRenderers {
   private static final ResourceLocation BOOGER = DevilFruits.id("textures/entity/bomu_bomu/booger.png");

   private BomuRenderers() {
   }

   static final class Booger extends EntityRenderer<BoogerBomb> {
      Booger(Context ctx) {
         super(ctx);
      }

      public void render(BoogerBomb e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
         pose.pushPose();
         pose.translate(0.0, (double)e.getBbHeight() * 0.5, 0.0);
         pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
         float t = (float)e.tickCount + partial;
         float wob = e.stuck() ? 1.0F + 0.12F * Mth.sin(t * 1.7F) : 1.0F;
         float s = 0.36F;
         pose.scale(s * wob, s / wob, s);
         VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(BomuRenderers.BOOGER));
         Pose last = pose.last();
         int lit = LightTexture.pack(Math.max(LightTexture.block(light), 6), LightTexture.sky(light));
         v(vc, last, -0.5F, -0.5F, 0.0F, 1.0F, lit);
         v(vc, last, 0.5F, -0.5F, 1.0F, 1.0F, lit);
         v(vc, last, 0.5F, 0.5F, 1.0F, 0.0F, lit);
         v(vc, last, -0.5F, 0.5F, 0.0F, 0.0F, lit);
         pose.popPose();
         super.render(e, yaw, partial, pose, buffers, light);
      }

      private static void v(VertexConsumer vc, Pose pose, float x, float y, float u, float v, int light) {
         vc.addVertex(pose, x, y, 0.0F)
            .setColor(255, 255, 255, 255)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(pose, 0.0F, 1.0F, 0.0F);
      }

      public ResourceLocation getTextureLocation(BoogerBomb e) {
         return BomuRenderers.BOOGER;
      }
   }

   static final class Bullet extends EntityRenderer<BombBullet> {
      Bullet(Context ctx) {
         super(ctx);
      }

      public void render(BombBullet e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
      }

      public ResourceLocation getTextureLocation(BombBullet e) {
         return BomuRenderers.BOOGER;
      }
   }
}
