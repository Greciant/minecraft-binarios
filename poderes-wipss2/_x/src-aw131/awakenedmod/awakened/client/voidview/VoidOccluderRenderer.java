package com.awakenedmod.awakened.client.voidview;

import com.awakenedmod.awakened.client.compat.ShaderPacks;
import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.voidview.VoidOccluderEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;

public final class VoidOccluderRenderer extends EntityRenderer<VoidOccluderEntity> {
   private static final ResourceLocation WHITE = AwId.of("textures/misc/white.png");

   public VoidOccluderRenderer(Context ctx) {
      super(ctx);
   }

   public ResourceLocation getTextureLocation(VoidOccluderEntity entity) {
      return null;
   }

   public boolean shouldRender(VoidOccluderEntity entity, Frustum frustum, double camX, double camY, double camZ) {
      return true;
   }

   public void render(VoidOccluderEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
      if (ShaderPacks.shadowPass()) {
         VoidViewRenderer.shellQuads(poseStack.last(), buffers.getBuffer(RenderType.entitySolid(WHITE)), entity.radius());
      }
   }
}
