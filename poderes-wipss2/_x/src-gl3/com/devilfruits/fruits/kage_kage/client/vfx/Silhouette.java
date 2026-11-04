package com.devilfruits.fruits.kage_kage.client.vfx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class Silhouette {
   public static void draw(Entity e, PoseStack ps, MultiBufferSource buffers, float partial, boolean rim) {
      EntityRenderer<Entity> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(e);
      float bodyO = 0.0F;
      float body = 0.0F;
      float headO = 0.0F;
      float head = 0.0F;
      LivingEntity living = e instanceof LivingEntity l ? l : null;
      if (living != null) {
         bodyO = living.yBodyRotO;
         body = living.yBodyRot;
         headO = living.yHeadRotO;
         head = living.yHeadRot;
         living.yBodyRotO = living.yBodyRot = 180.0F;
         living.yHeadRotO = living.yHeadRot = 180.0F;
      }

      ps.pushPose();
      if (rim) {
         ps.translate(0.0F, -0.04F, 0.0F);
         ps.scale(1.08F, 1.04F, 1.0F);
      }

      ps.scale(1.0F, 1.0F, 0.06F);
      renderer.render(e, 0.0F, partial, ps, new Silhouette.Tint(buffers, rim ? -3908640 : -16317428, rim), 15728880);
      ps.popPose();
      if (living != null) {
         living.yBodyRotO = bodyO;
         living.yBodyRot = body;
         living.yHeadRotO = headO;
         living.yHeadRot = head;
      }
   }

   private Silhouette() {
   }

   private static record Painter(VertexConsumer vc, int argb) implements VertexConsumer {
      public void addVertex(float x, float y, float z, int color, float u, float v, int overlay, int light, float nx, float ny, float nz) {
         this.vc.addVertex(x, y, z, this.argb, u, v, OverlayTexture.NO_OVERLAY, 15728880, nx, ny, nz);
      }

      public VertexConsumer addVertex(float x, float y, float z) {
         this.vc.addVertex(x, y, z).setColor(this.argb).setLight(15728880).setOverlay(OverlayTexture.NO_OVERLAY);
         return this;
      }

      public VertexConsumer setColor(int r, int g, int b, int a) {
         return this;
      }

      public VertexConsumer setUv(float u, float v) {
         this.vc.setUv(u, v);
         return this;
      }

      public VertexConsumer setUv1(int u, int v) {
         return this;
      }

      public VertexConsumer setUv2(int u, int v) {
         return this;
      }

      public VertexConsumer setNormal(float x, float y, float z) {
         this.vc.setNormal(x, y, z);
         return this;
      }
   }

   private static record Tint(MultiBufferSource inner, int argb, boolean glow) implements MultiBufferSource {
      public VertexConsumer getBuffer(RenderType type) {
         VertexConsumer vc = this.inner.getBuffer(this.glow ? RenderType.eyes(KageLook.white()) : type);
         return new Silhouette.Painter(vc, this.argb);
      }
   }
}
