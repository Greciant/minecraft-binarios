package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.velocity.SpeedEchoEntity;
import com.awakenedmod.awakened.content.velocity.VelocityPalette;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.UUID;
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
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.Vec3;

public final class SpeedEchoRenderer extends EntityRenderer<SpeedEchoEntity> {
   private static final float STRETCH_ALONG = 1.35F;
   private static final float SQUASH_ACROSS = 0.9F;
   private static final float LEAN_DEG = 8.0F;
   private static final float PEAK_ALPHA = 0.4F;
   private static final float WHITE_MIX_SPAWN = 0.45F;
   private static final float WHITE_MIX_END = 0.1F;
   private static final float WHITE_MIX_PER_GEAR = 0.06F;
   private static final float MIN_RUN_SPEED = 0.02F;
   private static final float MODEL_SCALE = 0.9375F;
   private static final float MODEL_FEET_OFFSET = 1.501F;
   private static final int MIN_VISIBLE_ALPHA = 3;
   private final PlayerModel<AbstractClientPlayer> wide;
   private final PlayerModel<AbstractClientPlayer> slim;
   private static final double OWNER_FP_HIDE_DISTANCE = 1.8;

   public SpeedEchoRenderer(Context ctx) {
      super(ctx);
      this.wide = new PlayerModel(ctx.bakeLayer(ModelLayers.PLAYER), false);
      this.slim = new PlayerModel(ctx.bakeLayer(ModelLayers.PLAYER_SLIM), true);
   }

   public ResourceLocation getTextureLocation(SpeedEchoEntity entity) {
      return null;
   }

   public boolean shouldRender(SpeedEchoEntity entity, Frustum frustum, double camX, double camY, double camZ) {
      return true;
   }

   public void render(SpeedEchoEntity entity, float entityYaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int packedLight) {
      super.render(entity, entityYaw, partialTick, pose, buffers, packedLight);
      float life = lifeFraction((float)entity.tickCount + partialTick, (float)entity.lifetime());
      int argb = tint(life, entity.gear(), VelocityPalette.of(DyeColor.byId(entity.colorId())));
      if (argb >>> 24 >= 3 && !hiddenFromOwnerCamera(entity.playerId(), entity.getX(), entity.getY(), entity.getZ())) {
         int light = LevelRenderer.getLightColor(entity.level(), entity.blockPosition());
         ClientPacketListener conn = Minecraft.getInstance().getConnection();
         PlayerInfo info = conn == null ? null : conn.getPlayerInfo(entity.playerId());
         PlayerSkin skin = info != null ? info.getSkin() : DefaultPlayerSkin.get(entity.playerId());
         PlayerModel<AbstractClientPlayer> model = skin.model() == Model.SLIM ? this.slim : this.wide;
         ResourceLocation texture = skin.texture();
         poseStride(model, entity.limbPhase(), entity.limbAmount());
         pose.pushPose();
         smear(pose, entity.velocity(), entity.yawDeg());
         uprightTransform(pose, entity.yawDeg());
         RenderType type = RenderType.entityTranslucentEmissive(texture);
         VertexConsumer vc = buffers.getBuffer(type);
         model.renderToBuffer(pose, vc, light, OverlayTexture.NO_OVERLAY, argb);
         pose.popPose();
      }
   }

   private static void poseStride(PlayerModel<AbstractClientPlayer> m, float phase, float amount) {
      float swing = phase * 0.6662F;
      m.head.xRot = 0.0F;
      m.head.yRot = 0.0F;
      m.hat.copyFrom(m.head);
      m.body.yRot = 0.0F;
      m.rightArm.xRot = Mth.cos(swing + (float) Math.PI) * 2.0F * amount * 0.5F;
      m.leftArm.xRot = Mth.cos(swing) * 2.0F * amount * 0.5F;
      m.rightArm.yRot = 0.0F;
      m.leftArm.yRot = 0.0F;
      m.rightArm.zRot = 0.0F;
      m.leftArm.zRot = 0.0F;
      m.rightLeg.xRot = Mth.cos(swing) * 1.4F * amount;
      m.leftLeg.xRot = Mth.cos(swing + (float) Math.PI) * 1.4F * amount;
      m.rightLeg.yRot = 0.005F;
      m.leftLeg.yRot = -0.005F;
      m.rightLeg.zRot = 0.005F;
      m.leftLeg.zRot = -0.005F;
      m.leftPants.copyFrom(m.leftLeg);
      m.rightPants.copyFrom(m.rightLeg);
      m.leftSleeve.copyFrom(m.leftArm);
      m.rightSleeve.copyFrom(m.rightArm);
      m.jacket.copyFrom(m.body);
   }

   private static boolean hiddenFromOwnerCamera(UUID owner, double x, double y, double z) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.options.getCameraType().isFirstPerson() && mc.player.getUUID().equals(owner)) {
         Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
         return cam.distanceToSqr(x, y, z) < 3.24;
      } else {
         return false;
      }
   }

   private static float lifeFraction(float ageInTicks, float lifetime) {
      return lifetime <= 0.0F ? 1.0F : Mth.clamp(ageInTicks / lifetime, 0.0F, 1.0F);
   }

   private static int tint(float life, int gear, VelocityPalette palette) {
      float remaining = (1.0F - life) * (1.0F - life);
      int alpha = Mth.clamp(Math.round(0.4F * remaining * 255.0F), 0, 255);
      float endMix = Mth.clamp(0.1F + 0.06F * (float)gear, 0.0F, 1.0F);
      float mix = Mth.lerp(remaining, endMix, 0.45F);
      return alpha << 24 | VelocityStreaks.lerpRgb(palette.body(), palette.hot(), mix);
   }

   private static void smear(PoseStack pose, Vec3 velocity, float bodyYawDeg) {
      float runYaw = runYawDeg(velocity, bodyYawDeg);
      pose.mulPose(Axis.YP.rotationDegrees(runYaw));
      pose.scale(0.9F, 0.9F, 1.35F);
      pose.mulPose(Axis.XP.rotationDegrees(8.0F));
      pose.mulPose(Axis.YP.rotationDegrees(-runYaw));
   }

   private static float runYawDeg(Vec3 velocity, float bodyYawDeg) {
      double hx = velocity.x;
      double hz = velocity.z;
      return hx * hx + hz * hz < 4.0E-4F ? -bodyYawDeg : (float)Math.toDegrees(Math.atan2(hx, hz));
   }

   private static void uprightTransform(PoseStack pose, float bodyYawDeg) {
      pose.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYawDeg));
      pose.scale(-1.0F, -1.0F, 1.0F);
      pose.scale(0.9375F, 0.9375F, 0.9375F);
      pose.translate(0.0F, -1.501F, 0.0F);
   }
}
