package com.devilfruits.fruits.horo_horo.client;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.animation.AnimationLoader;
import com.devilfruits.client.animation.PlayerAnimation;
import com.devilfruits.fruits.horo_horo.HoroHoro;
import com.devilfruits.fruits.horo_horo.entity.GhostBody;
import com.devilfruits.fruits.yomi_yomi.client.PlayerCorpseRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import javax.annotation.Nullable;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.resources.PlayerSkin.Model;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class GhostBodyRenderer extends EntityRenderer<GhostBody> {
   private static final ResourceLocation EXIT = HoroHoro.id("ghost_exit");
   private static final ResourceLocation WASH = DevilFruits.id("textures/entity/horo_horo/negative_wash.png");
   private static final float DRAIN_TICKS = 24.0F;
   private static final float[] ROT = new float[3];
   private static final float[] POS = new float[3];
   private final PlayerModel<?> wide;
   private final PlayerModel<?> slim;

   public GhostBodyRenderer(Context ctx) {
      super(ctx);
      this.wide = new PlayerModel(ctx.bakeLayer(ModelLayers.PLAYER), false);
      this.slim = new PlayerModel(ctx.bakeLayer(ModelLayers.PLAYER_SLIM), true);
      this.shadowRadius = 0.45F;
   }

   public void render(GhostBody body, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
      PlayerSkin skin = PlayerCorpseRenderer.skin(body.ownerId());
      PlayerModel<?> model = skin.model() == Model.SLIM ? this.slim : this.wide;
      PlayerAnimation exit = AnimationLoader.INSTANCE.get(EXIT);
      float age = (float)body.tickCount + partialTick;
      float t = exit == null ? 0.0F : Math.min(age, exit.length());
      pose(model, exit, t);
      pose.pushPose();
      pose.mulPose(Axis.YP.rotationDegrees(180.0F - body.getYRot()));
      if (exit != null && exit.root != null) {
         exit.root.sample(t, ROT, POS);
         pose.translate(POS[0], POS[1], -POS[2]);
         pose.translate(0.0F, 0.9F, 0.0F);
         pose.mulPose(Axis.YP.rotation(ROT[1]));
         pose.mulPose(Axis.XP.rotation(-ROT[0]));
         pose.mulPose(Axis.ZP.rotation(ROT[2]));
         pose.translate(0.0F, -0.9F, 0.0F);
      }

      float shudder = body.hurtTicks() < 8 ? Mth.sin(((float)body.hurtTicks() + partialTick) * 2.6F) * (float)(8 - body.hurtTicks()) * 0.5F : 0.0F;
      pose.mulPose(Axis.ZP.rotationDegrees(shudder));
      pose.scale(-1.0F, -1.0F, 1.0F);
      pose.translate(0.0F, -1.501F, 0.0F);
      model.renderToBuffer(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(skin.texture())), light, OverlayTexture.NO_OVERLAY);
      float drain = Mth.clamp((age - 4.0F) / 24.0F, 0.0F, 1.0F);
      if (drain > 0.0F) {
         model.renderToBuffer(
            pose, buffers.getBuffer(RenderType.entityTranslucent(WASH)), light, OverlayTexture.NO_OVERLAY, Math.round(drain * 175.0F) << 24 | 10921138
         );
      }

      pose.popPose();
      super.render(body, yaw, partialTick, pose, buffers, light);
   }

   private static void pose(PlayerModel<?> model, @Nullable PlayerAnimation clip, float t) {
      model.young = false;
      model.riding = false;
      model.attackTime = 0.0F;
      model.setAllVisible(true);
      ModelPart[] parts = new ModelPart[]{model.head, model.body, model.rightArm, model.leftArm, model.rightLeg, model.leftLeg};
      PlayerAnimation.Part[] keys = PlayerAnimation.Part.values();

      for (int i = 0; i < parts.length; i++) {
         parts[i].resetPose();
         PlayerAnimation.Channel channel = clip == null ? null : clip.parts.get(keys[i]);
         if (channel != null) {
            channel.sample(t, ROT, POS);
            parts[i].setRotation(ROT[0], ROT[1], ROT[2]);
         }
      }

      model.hat.copyFrom(model.head);
      model.jacket.copyFrom(model.body);
      model.rightSleeve.copyFrom(model.rightArm);
      model.leftSleeve.copyFrom(model.leftArm);
      model.rightPants.copyFrom(model.rightLeg);
      model.leftPants.copyFrom(model.leftLeg);
   }

   public ResourceLocation getTextureLocation(GhostBody body) {
      return PlayerCorpseRenderer.skin(body.ownerId()).texture();
   }
}
