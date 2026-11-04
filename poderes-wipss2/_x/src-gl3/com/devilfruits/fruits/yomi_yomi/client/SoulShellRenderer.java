package com.devilfruits.fruits.yomi_yomi.client;

import com.devilfruits.client.animation.AnimationLoader;
import com.devilfruits.client.animation.PlayerAnimation;
import com.devilfruits.fruits.yomi_yomi.Vessel;
import com.devilfruits.fruits.yomi_yomi.YomiYomi;
import com.devilfruits.fruits.yomi_yomi.entity.SoulShell;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.resources.PlayerSkin.Model;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.AbstractSkeleton;

public class SoulShellRenderer extends EntityRenderer<SoulShell> {
   private static final ResourceLocation KNEEL = YomiYomi.id("kneel");
   private static final ResourceLocation SKELETON = ResourceLocation.withDefaultNamespace("textures/entity/skeleton/skeleton.png");
   private static final float[] ROT = new float[3];
   private static final float[] POS = new float[3];
   private final PlayerModel<?> wide;
   private final PlayerModel<?> slim;
   private final SkeletonModel<AbstractSkeleton> skeleton;

   public SoulShellRenderer(Context ctx) {
      super(ctx);
      this.wide = new PlayerModel(ctx.bakeLayer(ModelLayers.PLAYER), false);
      this.slim = new PlayerModel(ctx.bakeLayer(ModelLayers.PLAYER_SLIM), true);
      this.skeleton = new SkeletonModel(ctx.bakeLayer(ModelLayers.SKELETON));
      this.shadowRadius = 0.45F;
   }

   public void render(SoulShell shell, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
      UUID owner = shell.ownerId();
      PlayerSkin skin = PlayerCorpseRenderer.skin(owner);
      Vessel vessel = shell.vessel();
      HumanoidModel<?> model = (HumanoidModel<?>)(vessel == Vessel.SKELETON ? this.skeleton : (skin.model() == Model.SLIM ? this.slim : this.wide));
      ResourceLocation texture = texture(shell, skin, vessel);
      PlayerAnimation kneel = AnimationLoader.INSTANCE.get(KNEEL);
      float t = kneel == null ? 0.0F : Math.min((float)shell.tickCount + partialTick, kneel.length());
      pose(model, kneel, t);
      pose.pushPose();
      pose.mulPose(Axis.YP.rotationDegrees(180.0F - shell.getYRot()));
      if (kneel != null && kneel.root != null) {
         kneel.root.sample(t, ROT, POS);
         pose.translate(POS[0], POS[1], -POS[2]);
         pose.translate(0.0F, 0.9F, 0.0F);
         pose.mulPose(Axis.YP.rotation(ROT[1]));
         pose.mulPose(Axis.XP.rotation(-ROT[0]));
         pose.mulPose(Axis.ZP.rotation(ROT[2]));
         pose.translate(0.0F, -0.9F, 0.0F);
      }

      float shiver = shell.hurtTicks() < 8 ? Mth.sin(((float)shell.hurtTicks() + partialTick) * 2.6F) * (float)(8 - shell.hurtTicks()) * 0.5F : 0.0F;
      pose.mulPose(Axis.ZP.rotationDegrees(shiver));
      pose.scale(-1.0F, -1.0F, 1.0F);
      pose.translate(0.0F, -1.501F, 0.0F);
      model.renderToBuffer(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(texture)), light, OverlayTexture.NO_OVERLAY);
      pose.popPose();
      super.render(shell, yaw, partialTick, pose, buffers, light);
   }

   private static ResourceLocation texture(SoulShell shell, PlayerSkin skin, @Nullable Vessel vessel) {
      if (vessel == Vessel.SKELETON) {
         return SKELETON;
      } else {
         if (vessel == Vessel.CORPSE
            && shell.ownerId() != null
            && Minecraft.getInstance().level != null
            && Minecraft.getInstance().level.getPlayerByUUID(shell.ownerId()) instanceof AbstractClientPlayer p) {
            ResourceLocation corpse = VesselSkins.corpseSkin(p);
            if (corpse != null) {
               return corpse;
            }
         }

         return skin.texture();
      }
   }

   private static void pose(HumanoidModel<?> model, @Nullable PlayerAnimation clip, float t) {
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
      if (model instanceof SkeletonModel) {
         model.hat.visible = false;
      }

      if (model instanceof PlayerModel<?> player) {
         player.jacket.copyFrom(model.body);
         player.rightSleeve.copyFrom(model.rightArm);
         player.leftSleeve.copyFrom(model.leftArm);
         player.rightPants.copyFrom(model.rightLeg);
         player.leftPants.copyFrom(model.leftLeg);
      }
   }

   public ResourceLocation getTextureLocation(SoulShell shell) {
      return PlayerCorpseRenderer.skin(shell.ownerId()).texture();
   }
}
