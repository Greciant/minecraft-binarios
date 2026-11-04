package com.devilfruits.fruits.doru_doru.client.render;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.model.geo.GeoPose;
import com.devilfruits.client.model.geo.GeoRenderer;
import com.devilfruits.fruits.doru_doru.coating.WaxCoating;
import com.mojang.blaze3d.vertex.PoseStack;
import javax.annotation.Nullable;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

public class WaxCoatingLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
   private static final ResourceLocation[] COATS = new ResourceLocation[10];

   @Nullable
   private static ResourceLocation coat(LivingEntity e) {
      if (!e.isInvisible() && e.hasData(WaxCoating.TYPE)) {
         WaxCoating.Data d = WaxCoating.get(e);
         int stacks = d.encased(e.level().getGameTime()) ? 10 : Math.min(10, d.stacks());
         return stacks <= 0 ? null : COATS[stacks - 1];
      } else {
         return null;
      }
   }

   public static void geo(LivingEntity e, GeoRenderer geo, GeoPose pose, MultiBufferSource buffers, int light, int overlay) {
      ResourceLocation tex = coat(e);
      if (tex != null) {
         geo.render(buffers.getBuffer(RenderType.entityCutoutNoCull(tex)), pose, light, overlay, -1);
      }
   }

   public WaxCoatingLayer(RenderLayerParent<T, M> parent) {
      super(parent);
   }

   public void render(
      PoseStack pose,
      MultiBufferSource buffers,
      int light,
      T e,
      float limbSwing,
      float limbSwingAmount,
      float partial,
      float age,
      float netHeadYaw,
      float headPitch
   ) {
      ResourceLocation tex = coat(e);
      if (tex != null) {
         int overlay = LivingEntityRenderer.getOverlayCoords(e, 0.0F);
         this.getParentModel().renderToBuffer(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(tex)), light, overlay, -1);
         if (WaxCoating.get(e).encased(e.level().getGameTime())) {
            pose.pushPose();
            pose.translate(0.0F, 0.75F, 0.0F);
            pose.scale(1.08F, 1.05F, 1.08F);
            pose.translate(0.0F, -0.75F, 0.0F);
            this.getParentModel().renderToBuffer(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(tex)), light, overlay, -1);
            pose.popPose();
         }
      }
   }

   static {
      for (int i = 0; i < COATS.length; i++) {
         COATS[i] = DevilFruits.id("textures/entity/doru_doru/coating_" + (i + 1) + ".png");
      }
   }
}
