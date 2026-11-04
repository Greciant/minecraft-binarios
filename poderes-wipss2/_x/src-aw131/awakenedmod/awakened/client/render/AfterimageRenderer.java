package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.teleporter.AfterimageEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.resources.PlayerSkin.Model;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class AfterimageRenderer extends EntityRenderer<AfterimageEntity> {
   private static final int VIOLET = 11565823;
   private static final float PEAK_ALPHA = 150.0F;
   private final PlayerModel<AbstractClientPlayer> wide;
   private final PlayerModel<AbstractClientPlayer> slim;

   public AfterimageRenderer(Context ctx) {
      super(ctx);
      this.wide = new PlayerModel(ctx.bakeLayer(ModelLayers.PLAYER), false);
      this.slim = new PlayerModel(ctx.bakeLayer(ModelLayers.PLAYER_SLIM), true);
   }

   public ResourceLocation getTextureLocation(AfterimageEntity entity) {
      return null;
   }

   public boolean shouldRender(AfterimageEntity entity, Frustum frustum, double camX, double camY, double camZ) {
      return true;
   }

   public void render(AfterimageEntity entity, float entityYaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int packedLight) {
      super.render(entity, entityYaw, partialTick, pose, buffers, packedLight);
      float lifetime = (float)entity.lifetime();
      float age = (float)entity.tickCount + partialTick;
      float life = lifetime <= 0.0F ? 1.0F : Mth.clamp(age / lifetime, 0.0F, 1.0F);
      int alpha = (int)((1.0F - life) * 150.0F);
      if (alpha > 2) {
         float yawDeg = entity.yawDeg();
         int light = LevelRenderer.getLightColor(entity.level(), entity.blockPosition());
         ClientPacketListener conn = Minecraft.getInstance().getConnection();
         PlayerInfo info = conn == null ? null : conn.getPlayerInfo(entity.playerId());
         PlayerSkin skin = info != null ? info.getSkin() : DefaultPlayerSkin.get(entity.playerId());
         boolean isSlim = skin.model() == Model.SLIM;
         PlayerModel<AbstractClientPlayer> model = isSlim ? this.slim : this.wide;
         ResourceLocation texture = skin.texture();
         pose.pushPose();
         pose.mulPose(Axis.YP.rotationDegrees(180.0F - yawDeg));
         pose.scale(-1.0F, -1.0F, 1.0F);
         pose.scale(0.9375F, 0.9375F, 0.9375F);
         pose.translate(0.0F, -1.501F, 0.0F);
         int colorARGB = Math.min(255, alpha) << 24 | 11565823;
         RenderType type = RenderType.entityTranslucent(texture);
         VertexConsumer vc = buffers.getBuffer(type);
         model.renderToBuffer(pose, vc, light, OverlayTexture.NO_OVERLAY, colorARGB);
         pose.popPose();
      }
   }
}
