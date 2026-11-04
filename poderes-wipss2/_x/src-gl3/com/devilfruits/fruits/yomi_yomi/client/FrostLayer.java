package com.devilfruits.fruits.yomi_yomi.client;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.model.geo.GeoPose;
import com.devilfruits.client.model.geo.GeoRenderer;
import com.devilfruits.fruits.yomi_yomi.frost.YomiFrost;
import com.mojang.blaze3d.vertex.PoseStack;
import javax.annotation.Nullable;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

public class FrostLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
   private static final ResourceLocation[] COATS = new ResourceLocation[4];
   private static final ResourceLocation SHELL = DevilFruits.id("textures/entity/yomi_yomi/frost_shell.png");

   @Nullable
   private static ResourceLocation coat(LivingEntity e) {
      if (!e.isInvisible() && e.hasData(YomiFrost.TYPE)) {
         YomiFrost.Data d = YomiFrost.get(e);
         long now = e.level().getGameTime();
         int level = d.frozen(now) ? 4 : Math.max(d.cold(now) ? 2 : 0, d.chillAt(now));
         return level <= 0 ? null : COATS[Math.min(4, level) - 1];
      } else {
         return null;
      }
   }

   public static void geo(LivingEntity e, GeoRenderer geo, GeoPose pose, MultiBufferSource buffers, int light, int overlay) {
      ResourceLocation tex = coat(e);
      if (tex != null) {
         geo.render(buffers.getBuffer(RenderType.entityTranslucent(tex)), pose, light, overlay, -1);
      }
   }

   public FrostLayer(RenderLayerParent<T, M> parent) {
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
         this.getParentModel().renderToBuffer(pose, buffers.getBuffer(RenderType.entityTranslucent(tex)), light, overlay, -1);
         if (YomiFrost.frozen(e)) {
            int glow = LightTexture.pack(Math.max(LightTexture.block(light), 9), LightTexture.sky(light));
            pose.pushPose();
            pose.translate(0.0F, 0.75F, 0.0F);
            pose.scale(1.1F, 1.06F, 1.1F);
            pose.translate(0.0F, -0.75F, 0.0F);
            this.getParentModel().renderToBuffer(pose, buffers.getBuffer(RenderType.entityTranslucent(SHELL)), glow, overlay, -419430401);
            pose.popPose();
         }
      }
   }

   static {
      for (int i = 0; i < COATS.length; i++) {
         COATS[i] = DevilFruits.id("textures/entity/yomi_yomi/frost_" + (i + 1) + ".png");
      }
   }
}
