package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.entity.projectile.KineticProjectile;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;

public final class InvisibleProjectileRenderer extends EntityRenderer<KineticProjectile> {
   public InvisibleProjectileRenderer(Context context) {
      super(context);
   }

   public ResourceLocation getTextureLocation(KineticProjectile entity) {
      return null;
   }

   public void render(KineticProjectile entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
   }
}
