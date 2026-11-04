package com.devilfruits.fruits.yomi_yomi.client;

import com.devilfruits.client.render.FormRenderers;
import com.devilfruits.core.form.FruitForm;
import com.devilfruits.fruits.yomi_yomi.Vessel;
import com.mojang.blaze3d.vertex.PoseStack;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.monster.AbstractSkeleton;

public final class SkeletonBody implements FormRenderers.FormRenderer {
   private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/skeleton/skeleton.png");
   @Nullable
   private static SkeletonModel<AbstractSkeleton> model;
   @Nullable
   private static EntityModelSet bakedFrom;

   private static SkeletonModel<AbstractSkeleton> model() {
      EntityModelSet set = Minecraft.getInstance().getEntityModels();
      if (model == null || bakedFrom != set) {
         model = new SkeletonModel(set.bakeLayer(ModelLayers.SKELETON));
         bakedFrom = set;
      }

      return model;
   }

   public static boolean render(AbstractClientPlayer player, PlayerModel<?> posed, PoseStack pose, MultiBufferSource buffers, int light, int overlay, int color) {
      if (shown(player) != Vessel.SKELETON) {
         return false;
      } else {
         SkeletonModel<AbstractSkeleton> skel = model();
         posed.copyPropertiesTo(skel);
         skel.hat.visible = false;
         skel.renderToBuffer(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, overlay, color);
         return true;
      }
   }

   public static void renderGhost(PlayerModel<?> posed, PoseStack pose, MultiBufferSource buffers, int color) {
      SkeletonModel<AbstractSkeleton> skel = model();
      posed.copyPropertiesTo(skel);
      skel.hat.visible = false;
      skel.renderToBuffer(pose, buffers.getBuffer(RenderType.entityTranslucent(TEXTURE)), 15728880, OverlayTexture.NO_OVERLAY, color);
   }

   @Nullable
   public static Vessel shown(AbstractClientPlayer player) {
      return VesselSkins.preview != null && player == Minecraft.getInstance().player ? VesselSkins.preview : VesselSkins.shown(player);
   }

   @Override
   public boolean render(AbstractClientPlayer player, FruitForm form, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
      return false;
   }

   @Override
   public boolean renderArm(AbstractClientPlayer player, FruitForm form, HumanoidArm arm, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
      if (Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer) {
         PlayerModel<AbstractClientPlayer> pm = (PlayerModel<AbstractClientPlayer>)renderer.getModel();
         pm.attackTime = 0.0F;
         pm.crouching = false;
         pm.swimAmount = 0.0F;
         pm.setupAnim(player, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
         SkeletonModel skel = model();
         ModelPart src = arm == HumanoidArm.RIGHT ? pm.rightArm : pm.leftArm;
         ModelPart dst = arm == HumanoidArm.RIGHT ? skel.rightArm : skel.leftArm;
         dst.copyFrom(src);
         dst.xRot = 0.0F;
         dst.render(poseStack, buffers.getBuffer(RenderType.entitySolid(TEXTURE)), packedLight, OverlayTexture.NO_OVERLAY);
         return true;
      } else {
         return false;
      }
   }
}
