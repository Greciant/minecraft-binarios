package com.devilfruits.fruits.yomi_yomi.client;

import com.devilfruits.fruits.yomi_yomi.entity.PlayerCorpse;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.resources.PlayerSkin.Model;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class PlayerCorpseRenderer extends EntityRenderer<PlayerCorpse> {
   private final PlayerModel<?> wide;
   private final PlayerModel<?> slim;

   public PlayerCorpseRenderer(Context ctx) {
      super(ctx);
      this.wide = new PlayerModel(ctx.bakeLayer(ModelLayers.PLAYER), false);
      this.slim = new PlayerModel(ctx.bakeLayer(ModelLayers.PLAYER_SLIM), true);
      this.shadowRadius = 0.5F;
   }

   public static PlayerSkin skin(UUID owner) {
      ClientPacketListener connection = Minecraft.getInstance().getConnection();
      PlayerInfo info = connection != null && owner != null ? connection.getPlayerInfo(owner) : null;
      return info != null ? info.getSkin() : DefaultPlayerSkin.get(owner == null ? UUID.randomUUID() : owner);
   }

   public void render(PlayerCorpse corpse, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
      PlayerSkin skin = skin(corpse.ownerId());
      PlayerModel<?> model = skin.model() == Model.SLIM ? this.slim : this.wide;
      lay(model);
      pose.pushPose();
      pose.mulPose(Axis.YP.rotationDegrees(180.0F - corpse.getYRot()));
      float shiver = corpse.hurtTicks() < 8 ? Mth.sin(((float)corpse.hurtTicks() + partialTick) * 2.6F) * (float)(8 - corpse.hurtTicks()) * 0.6F : 0.0F;
      pose.mulPose(Axis.ZP.rotationDegrees(shiver));
      pose.translate(0.0F, 0.13F, 0.9F);
      pose.mulPose(Axis.XP.rotationDegrees(-90.0F));
      pose.scale(-1.0F, -1.0F, 1.0F);
      pose.translate(0.0F, -1.501F, 0.0F);
      model.renderToBuffer(pose, buffers.getBuffer(RenderType.entityTranslucent(skin.texture())), light, OverlayTexture.NO_OVERLAY);
      pose.popPose();
      super.render(corpse, yaw, partialTick, pose, buffers, light);
   }

   private static void lay(PlayerModel<?> model) {
      model.young = false;
      model.riding = false;
      model.attackTime = 0.0F;
      model.setAllVisible(true);
      model.head.setRotation(0.0F, 0.35F, 0.1F);
      model.hat.copyFrom(model.head);
      model.body.setRotation(0.0F, 0.0F, 0.0F);
      model.rightArm.setRotation(0.0F, 0.0F, 0.28F);
      model.leftArm.setRotation(0.0F, 0.0F, -0.2F);
      model.rightLeg.setRotation(0.0F, 0.0F, 0.08F);
      model.leftLeg.setRotation(0.0F, 0.0F, -0.12F);
      model.rightSleeve.copyFrom(model.rightArm);
      model.leftSleeve.copyFrom(model.leftArm);
      model.rightPants.copyFrom(model.rightLeg);
      model.leftPants.copyFrom(model.leftLeg);
      model.jacket.copyFrom(model.body);
   }

   public ResourceLocation getTextureLocation(PlayerCorpse corpse) {
      return skin(corpse.ownerId()).texture();
   }
}
