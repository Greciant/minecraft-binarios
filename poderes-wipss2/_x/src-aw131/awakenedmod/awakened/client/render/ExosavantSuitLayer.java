package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.client.ExoSuitAnim;
import com.awakenedmod.awakened.client.ExoSuitAssembly;
import com.awakenedmod.awakened.client.ExoSuitHide;
import com.awakenedmod.awakened.client.ExoSuitVisibility;
import com.awakenedmod.awakened.content.exosavant.ExoPiece;
import com.awakenedmod.awakened.content.exosavant.ExoSkin;
import com.awakenedmod.awakened.content.exosavant.ExoSkins;
import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.data.ExoVisual;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;
import java.util.function.IntPredicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

public final class ExosavantSuitLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> implements FirstPersonPassLayer {
   static final ResourceLocation UNDERSUIT_TEXTURE = AwId.of("textures/entity/exosavant_undersuit.png");
   private static final float CHIP_X = 0.0F;
   private static final float CHIP_Y = 8.0F;
   private static final float CHIP_Z = -2.6F;
   private static final float CHIP_SCALE = 0.7F;
   private static final float MIN_VISIBLE = 0.002F;
   private static final float BONE_SPAN = 0.5F;
   private static final float STREAK_OUT = 1.8F;
   private static final float STREAK_UP = 5.0F;
   private static final float SKY_DROP = 6.0F;
   private static final float SKY_DRIFT = 3.4F;
   private static final float[] SEAT_HEAD = new float[]{0.0F, 13.5F, 0.0F};
   private static final float[] SEAT_BODY = new float[]{0.0F, 12.0F, 0.0F};
   private static final float[] SEAT_LEFT_ARM = new float[]{-5.0F, 10.0F, 0.0F};
   private static final float[] SEAT_RIGHT_ARM = new float[]{5.0F, 10.0F, 0.0F};
   private static final float[] SEAT_LEFT_LEG = new float[]{-1.9F, 12.0F, 0.0F};
   private static final float[] SEAT_RIGHT_LEG = new float[]{1.9F, 12.0F, 0.0F};
   private static final float[] SEAT_LEFT_GAUNTLET = new float[]{-5.0F, -2.0F, 0.0F};
   private static final float[] SEAT_RIGHT_GAUNTLET = new float[]{5.0F, -2.0F, 0.0F};
   private static final float FP_ARM_PITCH_DEG = -82.0F;
   private static final float FP_ARM_YAW_DEG = 0.0F;
   private static final float FP_ARM_ROLL_DEG = 0.0F;
   private static final float FP_ARM_PITCH_FOLLOW = 1.0F;
   private static final float FP_ARM_OFFSET_X = 0.0F;
   private static final float FP_ARM_OFFSET_Y = 0.0F;
   private static final float FP_ARM_OFFSET_Z = 0.0F;
   private static final ExosavantSuitLayer.Bone[] BONES = new ExosavantSuitLayer.Bone[]{
      new ExosavantSuitLayer.Bone("Waist", "Body", ExosavantSuitLayer.Limb.BODY, ExoPiece.CHESTPLATE, 0.0F, -1.0F, 0.6F, SEAT_BODY),
      new ExosavantSuitLayer.Bone("Waist", "LeftArm", ExosavantSuitLayer.Limb.LARM, ExoPiece.GAUNTLETS, 1.2F, -0.4F, 0.0F, SEAT_LEFT_ARM),
      new ExosavantSuitLayer.Bone("Waist", "RightArm", ExosavantSuitLayer.Limb.RARM, ExoPiece.GAUNTLETS, -1.2F, -0.4F, 0.0F, SEAT_RIGHT_ARM),
      new ExosavantSuitLayer.Bone("Waist", "Head", ExosavantSuitLayer.Limb.HEAD, ExoPiece.HELMET, 0.0F, -1.4F, 0.0F, SEAT_HEAD),
      new ExosavantSuitLayer.Bone(null, "LeftLeg", ExosavantSuitLayer.Limb.LLEG, ExoPiece.LEGGINGS, 0.9F, -0.3F, -0.4F, SEAT_LEFT_LEG),
      new ExosavantSuitLayer.Bone(null, "RightLeg", ExosavantSuitLayer.Limb.RLEG, ExoPiece.LEGGINGS, -0.9F, -0.3F, -0.4F, SEAT_RIGHT_LEG),
      new ExosavantSuitLayer.Bone(null, "LeftBoot", ExosavantSuitLayer.Limb.LLEG, ExoPiece.BOOTS, 0.9F, 0.6F, -0.5F, SEAT_LEFT_LEG),
      new ExosavantSuitLayer.Bone(null, "RightBoot", ExosavantSuitLayer.Limb.RLEG, ExoPiece.BOOTS, -0.9F, 0.6F, -0.5F, SEAT_RIGHT_LEG),
      new ExosavantSuitLayer.Bone("Gauntlets", "LeftGauntlet", ExosavantSuitLayer.Limb.LARM, ExoPiece.GAUNTLETS, 1.4F, 0.2F, -0.3F, SEAT_LEFT_GAUNTLET),
      new ExosavantSuitLayer.Bone("Gauntlets", "RightGauntlet", ExosavantSuitLayer.Limb.RARM, ExoPiece.GAUNTLETS, -1.4F, 0.2F, -0.3F, SEAT_RIGHT_GAUNTLET)
   };
   private static final ExosavantSuitLayer.UnderBone[] UNDER_BONES = new ExosavantSuitLayer.UnderBone[]{
      new ExosavantSuitLayer.UnderBone(ExosavantSuitLayer.Limb.HEAD, "head", ExoSuitHide::headCovered),
      new ExosavantSuitLayer.UnderBone(ExosavantSuitLayer.Limb.BODY, "body", ExoSuitHide::bodyCovered),
      new ExosavantSuitLayer.UnderBone(ExosavantSuitLayer.Limb.LARM, "left_arm", ExoSuitHide::armsCovered),
      new ExosavantSuitLayer.UnderBone(ExosavantSuitLayer.Limb.RARM, "right_arm", ExoSuitHide::armsCovered),
      new ExosavantSuitLayer.UnderBone(ExosavantSuitLayer.Limb.LLEG, "left_leg", ExoSuitHide::legsCovered),
      new ExosavantSuitLayer.UnderBone(ExosavantSuitLayer.Limb.RLEG, "right_leg", ExoSuitHide::legsCovered)
   };
   private final ModelPart[] parts;
   private final ModelPart root;
   private final ModelPart chipPart;
   private final ModelPart[] undersuitParts;

   public ExosavantSuitLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, ModelPart root, ModelPart chipRoot) {
      super(parent);
      SuitModelUtil.applyThrusterUnderside(root);
      this.root = root;
      this.parts = buildParts(root);
      this.chipPart = chipRoot.getChild("Chip");
      ModelPart underRoot = ExosavantUndersuitModel.createBodyLayer().bakeRoot();
      this.undersuitParts = new ModelPart[UNDER_BONES.length];

      for (int i = 0; i < UNDER_BONES.length; i++) {
         this.undersuitParts[i] = underRoot.getChild(UNDER_BONES[i].name());
      }
   }

   private void poseChipOnChest(PoseStack poseStack) {
      this.limbPart(ExosavantSuitLayer.Limb.BODY).translateAndRotate(poseStack);
      poseStack.translate(0.0F, 0.5F, -0.1625F);
      poseStack.scale(0.7F, 0.7F, 0.7F);
      poseStack.scale(1.0F, -1.0F, 1.0F);
   }

   private static ModelPart[] buildParts(ModelPart root) {
      ModelPart[] out = new ModelPart[BONES.length];

      for (int i = 0; i < BONES.length; i++) {
         ExosavantSuitLayer.Bone b = BONES[i];
         out[i] = b.parent() == null ? root.getChild(b.name()) : root.getChild(b.parent()).getChild(b.name());
      }

      return out;
   }

   @Override
   public boolean showsInFirstPerson(AbstractClientPlayer player) {
      return ((ExoVisual)player.getData(AwakenedAttachments.EXO_VISUAL)).suited();
   }

   public void render(
      PoseStack poseStack,
      MultiBufferSource buffers,
      int packedLight,
      AbstractClientPlayer player,
      float limbSwing,
      float limbSwingAmount,
      float partialTick,
      float ageInTicks,
      float netHeadYaw,
      float headPitch
   ) {
      ExoVisual vis = (ExoVisual)player.getData(AwakenedAttachments.EXO_VISUAL);
      boolean suited = vis.suited();
      float g = ExoSuitAssembly.progress(player.getId(), suited);
      if (suited || !(g <= 0.002F)) {
         int mask = ExoSuitAssembly.mask(player.getId(), suited, ExoSuitVisibility.visibleMask(player, vis.builtMask()));
         boolean cache = vis.chipInstalled();
         boolean stowOut = !suited && cache;
         boolean chipShown = cache && ExoSuitVisibility.showsChip(player);
         int alpha = flyAwayAlpha(g);
         ExoSkin skin = ExoSkins.worn(player);
         RenderType steel = stowOut ? RenderType.entityTranslucent(skin.suit()) : RenderType.entityCutoutNoCull(skin.suit());
         RenderType glow = RenderType.entityTranslucentEmissive(skin.emissive());
         int glowTint = stowOut ? alpha << 24 | 16777215 : -1;
         boolean firstPersonArm = FirstPersonMode.isFirstPersonPass() && player == Minecraft.getInstance().player;
         boolean aiming = firstPersonArm && vis.handCannonDeploy();
         this.applySuitAnimation(player.getId(), vis);
         if (!firstPersonArm && !stowOut && suited && g >= 0.97F) {
            VertexConsumer vcUnder = buffers.getBuffer(RenderType.entityCutoutNoCull(UNDERSUIT_TEXTURE));

            for (int i = 0; i < UNDER_BONES.length; i++) {
               if (UNDER_BONES[i].covered().test(mask)) {
                  poseStack.pushPose();
                  this.limbPart(UNDER_BONES[i].limb()).translateAndRotate(poseStack);
                  this.undersuitParts[i].render(poseStack, vcUnder, packedLight, OverlayTexture.NO_OVERLAY);
                  poseStack.popPose();
               }
            }
         }

         for (int ix = 0; ix < BONES.length; ix++) {
            ExosavantSuitLayer.Bone b = BONES[ix];
            if ((mask & b.piece().bit()) != 0 && (!firstPersonArm || isFirstPersonGroup(b))) {
               float f = 1.0F - boneEase(ix, g);
               poseStack.pushPose();
               if (stowOut) {
                  if (!firstPersonArm) {
                     flyAwayRise(poseStack, f);
                  }
               } else if (!firstPersonArm && cache) {
                  skyDrop(poseStack, ix, f);
               }

               if (aiming) {
                  poseFirstPersonArm(poseStack, this.limbPart(b.limb()), headPitch);
               } else {
                  this.limbPart(b.limb()).translateAndRotate(poseStack);
               }

               if (stowOut) {
                  flyAwayScatter(poseStack, b, f);
               } else {
                  poseStack.translate(b.sx() * f, b.sy() * f, b.sz() * f);
               }

               seat(poseStack, b);
               if (stowOut) {
                  this.parts[ix].render(poseStack, buffers.getBuffer(steel), packedLight, OverlayTexture.NO_OVERLAY, alpha << 24 | 16777215);
               } else {
                  this.parts[ix].render(poseStack, buffers.getBuffer(steel), packedLight, OverlayTexture.NO_OVERLAY);
               }

               this.parts[ix].render(poseStack, buffers.getBuffer(glow), packedLight, OverlayTexture.NO_OVERLAY, glowTint);
               poseStack.popPose();
            }
         }

         if (!firstPersonArm && !stowOut && g > 0.98F && chipShown && (mask & ExoPiece.CHESTPLATE.bit()) != 0) {
            poseStack.pushPose();
            this.poseChipOnChest(poseStack);
            this.chipPart.render(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(skin.chip())), packedLight, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
         }
      }
   }

   static void unflipAnimationDelta(ModelPart root) {
      root.getAllParts().forEach(part -> {
         PartPose base = part.getInitialPose();
         part.y = 2.0F * base.y - part.y;
         part.xRot = 2.0F * base.xRot - part.xRot;
         part.zRot = 2.0F * base.zRot - part.zRot;
      });
   }

   private void applySuitAnimation(int entityId, ExoVisual vis) {
      ExoSuitAnim.Phase phase = ExoSuitAnim.update(entityId, vis.flying(), vis.barrageActive(), vis.handCannonDeploy(), vis.visorOpen());
      this.root.getAllParts().forEach(ModelPart::resetPose);
      this.applyOpenClose(TechArmorAnimation.ThrustersOpen, TechArmorAnimation.ThrustersClose, phase.thruster());
      this.applyOpenClose(TechArmorAnimation.BarrageStart, TechArmorAnimation.BarrageEnd, phase.barrage());
      this.applyOpenClose(TechArmorAnimation.HandCannonOpen, TechArmorAnimation.HandCannonClose, phase.handCannon());
      this.applyOpenClose(TechArmorAnimation.HelmetOpen, TechArmorAnimation.HelmetClose, phase.visor());
      unflipAnimationDelta(this.root);
   }

   private void applyOpenClose(AnimationDefinition open, AnimationDefinition close, float phase) {
      float t = Mth.clamp(phase, 0.0F, 2.0F);
      if (!(t <= 0.0F)) {
         if (t <= 1.0F) {
            TechArmorAnimation.apply(this.root, open, (long)(t * open.lengthInSeconds() * 1000.0F));
         } else {
            TechArmorAnimation.apply(this.root, close, (long)((t - 1.0F) * close.lengthInSeconds() * 1000.0F));
         }
      }
   }

   private static void seat(PoseStack poseStack, ExosavantSuitLayer.Bone b) {
      poseStack.translate(b.ox() / 16.0F, b.oy() / 16.0F, b.oz() / 16.0F);
      poseStack.scale(1.0F, -1.0F, 1.0F);
   }

   private static void poseFirstPersonArm(PoseStack poseStack, ModelPart arm, float lookPitchDeg) {
      poseStack.translate(arm.x / 16.0F, arm.y / 16.0F, arm.z / 16.0F);
      float pitch = (float)Math.toRadians((double)(-82.0F + 1.0F * lookPitchDeg));
      poseStack.mulPose(new Quaternionf().rotationZYX((float)Math.toRadians(0.0), (float)Math.toRadians(0.0), pitch));
      poseStack.translate(0.0F, 0.0F, 0.0F);
   }

   private static float boneEase(int order, float g) {
      float start = boneHash(order, 0.9377F) * 0.5F;
      float t = Mth.clamp((g - start) / 0.5F, 0.0F, 1.0F);
      return t * t * (3.0F - 2.0F * t);
   }

   private static void flyAwayRise(PoseStack poseStack, float f) {
      poseStack.translate(0.0F, -5.0F * f * f, 0.0F);
   }

   private static void skyDrop(PoseStack poseStack, int order, float f) {
      float dx = (boneHash(order, 0.618034F) * 2.0F - 1.0F) * 3.4F;
      float dz = (boneHash(order, 0.7548777F) * 2.0F - 1.0F) * 3.4F;
      float h = 6.0F * (0.6F + 0.65F * boneHash(order, 0.381966F));
      poseStack.translate(dx * f, -h * f * f, dz * f);
   }

   private static float boneHash(int order, float k) {
      float x = (float)(order + 1) * k * 7.13F;
      return x - (float)Math.floor((double)x);
   }

   private static void flyAwayScatter(PoseStack poseStack, ExosavantSuitLayer.Bone b, float f) {
      float streak = f * f;
      poseStack.translate(b.sx() * 1.8F * streak, b.sy() * 1.8F * streak, b.sz() * 1.8F * streak);
   }

   private static int flyAwayAlpha(float g) {
      return (int)(Mth.clamp(g * 1.5F, 0.0F, 1.0F) * 255.0F);
   }

   private static boolean isFirstPersonGroup(ExosavantSuitLayer.Bone b) {
      return b.name().equals("RightArm") || b.name().equals("RightGauntlet");
   }

   private ModelPart limbPart(ExosavantSuitLayer.Limb limb) {
      return switch (limb) {
         case HEAD -> ((PlayerModel)this.getParentModel()).head;
         case BODY -> ((PlayerModel)this.getParentModel()).body;
         case RARM -> ((PlayerModel)this.getParentModel()).rightArm;
         case LARM -> ((PlayerModel)this.getParentModel()).leftArm;
         case RLEG -> ((PlayerModel)this.getParentModel()).rightLeg;
         case LLEG -> ((PlayerModel)this.getParentModel()).leftLeg;
      };
   }

   private static record Bone(
      String parent, String name, ExosavantSuitLayer.Limb limb, ExoPiece piece, float sx, float sy, float sz, float ox, float oy, float oz
   ) {
      Bone(String parent, String name, ExosavantSuitLayer.Limb limb, ExoPiece piece, float sx, float sy, float sz, float[] seat) {
         this(parent, name, limb, piece, sx, sy, sz, seat[0], seat[1], seat[2]);
      }
   }

   private static enum Limb {
      HEAD,
      BODY,
      RARM,
      LARM,
      RLEG,
      LLEG;
   }

   private static record UnderBone(ExosavantSuitLayer.Limb limb, String name, IntPredicate covered) {
   }
}
