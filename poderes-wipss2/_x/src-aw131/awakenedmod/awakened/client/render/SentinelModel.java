package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.entity.sentinel.SentinelEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class SentinelModel extends HierarchicalModel<SentinelEntity> {
   public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("awakened", "sentinel"), "main");
   private static final float FEET_ANCHOR = 24.0F;
   private static final float DEG = (float) (Math.PI / 180.0);
   private static final float PI = (float) Math.PI;
   private final ModelPart root;
   private final ModelPart pelvis;
   private final ModelPart abdomen;
   private final ModelPart chest;
   private final ModelPart head;
   private final ModelPart rightArm;
   private final ModelPart leftArm;
   private final ModelPart rightFore;
   private final ModelPart leftFore;
   private final ModelPart rightThigh;
   private final ModelPart leftThigh;
   private final ModelPart rightShin;
   private final ModelPart leftShin;

   public SentinelModel(ModelPart root) {
      this.root = root;
      this.pelvis = root.getChild("pelvis");
      this.abdomen = root.getChild("abdomen");
      this.chest = root.getChild("chest");
      this.head = this.chest.getChild("head");
      this.rightArm = root.getChild("right_arm");
      this.leftArm = root.getChild("left_arm");
      this.rightFore = this.rightArm.getChild("right_fore");
      this.leftFore = this.leftArm.getChild("left_fore");
      this.rightThigh = root.getChild("right_thigh");
      this.leftThigh = root.getChild("left_thigh");
      this.rightShin = this.rightThigh.getChild("right_shin");
      this.leftShin = this.leftThigh.getChild("left_shin");
   }

   public ModelPart root() {
      return this.root;
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      PartDefinition pelvis = partdefinition.addOrReplaceChild(
         "pelvis",
         CubeListBuilder.create().texOffs(0, 0).addBox(-11.0F, -14.0F, -6.0F, 22.0F, 14.0F, 12.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, -60.0F, 0.0F)
      );
      PartDefinition abdomen = partdefinition.addOrReplaceChild(
         "abdomen",
         CubeListBuilder.create().texOffs(0, 32).addBox(-10.0F, -18.0F, -6.0F, 20.0F, 18.0F, 12.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, -74.0F, 0.0F)
      );
      PartDefinition chest = partdefinition.addOrReplaceChild(
         "chest",
         CubeListBuilder.create().texOffs(0, 64).addBox(-14.0F, -18.0F, -7.0F, 28.0F, 18.0F, 14.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, -92.0F, 0.0F)
      );
      PartDefinition head = chest.addOrReplaceChild(
         "head",
         CubeListBuilder.create().texOffs(0, 100).addBox(-6.0F, -12.0F, -6.0F, 12.0F, 12.0F, 12.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, -18.0F, 0.0F)
      );
      PartDefinition visor = head.addOrReplaceChild(
         "visor",
         CubeListBuilder.create().texOffs(48, 100).addBox(-5.0F, -9.0F, -6.6F, 10.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition core_seam = chest.addOrReplaceChild(
         "core_seam",
         CubeListBuilder.create().texOffs(140, 100).addBox(-2.0F, -16.0F, -7.6F, 4.0F, 16.0F, 2.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition back_vent = chest.addOrReplaceChild(
         "back_vent",
         CubeListBuilder.create().texOffs(96, 100).addBox(-8.0F, -16.0F, 5.0F, 16.0F, 16.0F, 2.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition right_arm = partdefinition.addOrReplaceChild(
         "right_arm",
         CubeListBuilder.create()
            .texOffs(90, 0)
            .addBox(-7.0F, -6.0F, -7.0F, 14.0F, 10.0F, 14.0F, new CubeDeformation(0.0F))
            .texOffs(90, 24)
            .addBox(-5.0F, 0.0F, -5.0F, 10.0F, 30.0F, 10.0F, new CubeDeformation(0.0F)),
         PartPose.offset(-15.0F, -106.0F, 0.0F)
      );
      PartDefinition right_fore = right_arm.addOrReplaceChild(
         "right_fore",
         CubeListBuilder.create().texOffs(130, 0).addBox(-5.0F, 0.0F, -5.0F, 10.0F, 26.0F, 10.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 30.0F, 0.0F)
      );
      PartDefinition right_fist = right_fore.addOrReplaceChild(
         "right_fist",
         CubeListBuilder.create().texOffs(172, 0).addBox(-6.0F, 0.0F, -6.0F, 12.0F, 12.0F, 12.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 26.0F, 0.0F)
      );
      PartDefinition left_arm = partdefinition.addOrReplaceChild(
         "left_arm",
         CubeListBuilder.create()
            .texOffs(90, 0)
            .mirror()
            .addBox(-7.0F, -6.0F, -7.0F, 14.0F, 10.0F, 14.0F, new CubeDeformation(0.0F))
            .mirror(false)
            .texOffs(90, 24)
            .mirror()
            .addBox(-5.0F, 0.0F, -5.0F, 10.0F, 30.0F, 10.0F, new CubeDeformation(0.0F))
            .mirror(false),
         PartPose.offset(15.0F, -106.0F, 0.0F)
      );
      PartDefinition left_fore = left_arm.addOrReplaceChild(
         "left_fore",
         CubeListBuilder.create().texOffs(130, 0).mirror().addBox(-5.0F, 0.0F, -5.0F, 10.0F, 26.0F, 10.0F, new CubeDeformation(0.0F)).mirror(false),
         PartPose.offset(0.0F, 30.0F, 0.0F)
      );
      PartDefinition left_fist = left_fore.addOrReplaceChild(
         "left_fist",
         CubeListBuilder.create().texOffs(172, 0).mirror().addBox(-6.0F, 0.0F, -6.0F, 12.0F, 12.0F, 12.0F, new CubeDeformation(0.0F)).mirror(false),
         PartPose.offset(0.0F, 26.0F, 0.0F)
      );
      PartDefinition right_thigh = partdefinition.addOrReplaceChild(
         "right_thigh",
         CubeListBuilder.create().texOffs(160, 64).addBox(-4.5F, 0.0F, -4.5F, 9.0F, 26.0F, 9.0F, new CubeDeformation(0.0F)),
         PartPose.offset(-6.0F, -66.0F, 0.0F)
      );
      PartDefinition right_shin = right_thigh.addOrReplaceChild(
         "right_shin",
         CubeListBuilder.create().texOffs(90, 110).addBox(-5.5F, 0.0F, -5.5F, 11.0F, 33.0F, 11.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 26.0F, 0.0F)
      );
      PartDefinition right_foot = right_shin.addOrReplaceChild(
         "right_foot",
         CubeListBuilder.create().texOffs(150, 110).addBox(-6.0F, 0.0F, -9.0F, 12.0F, 7.0F, 16.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 33.0F, 0.0F)
      );
      PartDefinition left_thigh = partdefinition.addOrReplaceChild(
         "left_thigh",
         CubeListBuilder.create().texOffs(160, 64).mirror().addBox(-4.5F, 0.0F, -4.5F, 9.0F, 26.0F, 9.0F, new CubeDeformation(0.0F)).mirror(false),
         PartPose.offset(6.0F, -66.0F, 0.0F)
      );
      PartDefinition left_shin = left_thigh.addOrReplaceChild(
         "left_shin",
         CubeListBuilder.create().texOffs(90, 110).mirror().addBox(-5.5F, 0.0F, -5.5F, 11.0F, 33.0F, 11.0F, new CubeDeformation(0.0F)).mirror(false),
         PartPose.offset(0.0F, 26.0F, 0.0F)
      );
      PartDefinition left_foot = left_shin.addOrReplaceChild(
         "left_foot",
         CubeListBuilder.create().texOffs(150, 110).mirror().addBox(-6.0F, 0.0F, -9.0F, 12.0F, 7.0F, 16.0F, new CubeDeformation(0.0F)).mirror(false),
         PartPose.offset(0.0F, 33.0F, 0.0F)
      );
      return LayerDefinition.create(meshdefinition, 256, 256);
   }

   public void setupAnim(SentinelEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitchDeg) {
      float gait = Math.min(limbSwingAmount, 1.0F);
      byte action = entity.getAction();
      float p = entity.getActionProgress();
      byte escalation = entity.getEscalation();
      boolean hovering = entity.isHovering();
      boolean shielded = entity.isShielded();
      this.root.y = 24.0F;
      this.root.xRot = 0.0F;
      this.root.zRot = Mth.cos(ageInTicks * 0.04F) * 0.02F;
      this.chest.xRot = 0.0F;
      this.abdomen.xRot = 0.0F;
      this.rightArm.xRot = this.leftArm.xRot = 0.0F;
      this.rightArm.zRot = this.leftArm.zRot = 0.0F;
      this.rightFore.xRot = this.leftFore.xRot = 0.0F;
      this.rightThigh.xRot = this.leftThigh.xRot = 0.0F;
      this.rightThigh.zRot = this.leftThigh.zRot = 0.0F;
      this.rightShin.xRot = this.leftShin.xRot = 0.0F;
      this.chest.y = -92.0F + Mth.cos(ageInTicks * 0.07F) * 0.4F;
      this.rightArm.zRot = 0.06F;
      this.leftArm.zRot = -0.06F;
      this.head.yRot = netHeadYaw * (float) (Math.PI / 180.0) * 0.6F;
      this.head.xRot = headPitchDeg * (float) (Math.PI / 180.0) * 0.5F;
      this.rightThigh.xRot = this.rightThigh.xRot + Mth.cos(limbSwing * 0.5F) * 0.7F * gait;
      this.leftThigh.xRot = this.leftThigh.xRot + Mth.cos(limbSwing * 0.5F + (float) Math.PI) * 0.7F * gait;
      this.rightShin.xRot = this.rightShin.xRot + Math.max(0.0F, Mth.cos(limbSwing * 0.5F + 1.2F)) * 0.5F * gait;
      this.leftShin.xRot = this.leftShin.xRot + Math.max(0.0F, Mth.cos(limbSwing * 0.5F + (float) Math.PI + 1.2F)) * 0.5F * gait;
      this.rightArm.xRot = this.rightArm.xRot + Mth.cos(limbSwing * 0.5F + (float) Math.PI) * 0.4F * gait;
      this.leftArm.xRot = this.leftArm.xRot + Mth.cos(limbSwing * 0.5F) * 0.4F * gait;
      if (escalation >= 3 && action != 14) {
         this.root.xRot += 0.06F;
         this.chest.zRot = Mth.sin(ageInTicks * 0.8F) * 0.015F;
      }

      if (hovering) {
         this.rightThigh.xRot += 0.7F;
         this.leftThigh.xRot += 0.7F;
         this.rightShin.xRot += 0.6F;
         this.leftShin.xRot += 0.6F;
         this.root.xRot += 0.12F;
         this.root.y = 24.0F + Mth.sin(ageInTicks * 0.18F) * 0.6F;
      }

      if (shielded) {
         this.leftArm.xRot = -1.5F;
         this.leftArm.zRot = -0.5F;
         this.leftFore.xRot = -1.1F;
      }

      this.applyAction(action, p, ageInTicks);
   }

   private static float smooth(float t) {
      t = Mth.clamp(t, 0.0F, 1.0F);
      return t * t * (3.0F - 2.0F * t);
   }

   private void applyAction(byte action, float p, float age) {
      float env = p < 0.5F ? smooth(p / 0.5F) : smooth((1.0F - p) / 0.5F);
      float recoil = smooth(Mth.clamp((p - 0.45F) / 0.1F, 0.0F, 1.0F)) * (1.0F - smooth(Mth.clamp((p - 0.62F) / 0.38F, 0.0F, 1.0F)));
      float hold = smooth(Mth.clamp(p / 0.3F, 0.0F, 1.0F)) * (1.0F - smooth(Mth.clamp((p - 0.8F) / 0.2F, 0.0F, 1.0F)));
      switch (action) {
         case 1:
            this.head.xRot -= 0.45F * env;
            this.root.xRot -= 0.04F * env;
            break;
         case 2:
            this.head.xRot -= 0.4F * hold;
            this.head.yRot = this.head.yRot + Mth.sin(p * (float) Math.PI) * 0.5F;
            this.rightThigh.zRot = 0.1F * hold;
            this.leftThigh.zRot = -0.1F * hold;
            break;
         case 3:
            this.rightThigh.xRot -= 1.1F * env;
            this.rightShin.xRot += 0.9F * env;
            this.root.y += 1.6F * recoil;
            this.root.xRot += 0.06F * recoil;
            break;
         case 4:
            this.rightArm.xRot -= 1.4F * env;
            this.rightFore.xRot -= 0.5F * env;
            this.rightArm.xRot += 2.3F * recoil;
            this.rightFore.xRot += 0.4F * recoil;
            break;
         case 5:
            this.rightArm.xRot -= 1.2F * hold;
            this.rightFore.xRot -= 0.3F * hold;
            this.rightArm.xRot = this.rightArm.xRot + Mth.sin(p * 48.0F) * 0.1F * recoil;
            break;
         case 6:
            this.rightArm.xRot -= 1.3F * env;
            this.rightFore.xRot -= 0.35F * env;
            this.rightArm.xRot += 0.5F * recoil;
            this.root.zRot += 0.03F * env;
            break;
         case 7:
            this.rightArm.xRot -= 0.9F * env;
            this.rightArm.zRot += env * 0.35F + recoil * 1.3F;
            break;
         case 8:
            this.rightArm.xRot -= 2.2F * env;
            this.leftArm.xRot -= 2.2F * env;
            this.rightArm.xRot += 1.6F * recoil;
            this.leftArm.xRot += 1.6F * recoil;
            this.root.xRot += 0.1F * recoil;
            break;
         case 9:
            this.rightArm.xRot -= 1.3F * env;
            this.leftArm.xRot -= 1.3F * env;
            this.rightArm.xRot += 0.9F * recoil;
            this.leftArm.xRot += 0.9F * recoil;
            this.root.xRot += 0.08F * recoil;
            break;
         case 10:
            this.rightArm.xRot -= 1.4F * hold;
            this.leftArm.xRot -= 1.4F * hold;
            this.rightArm.zRot = this.rightArm.zRot + Mth.sin(age * 0.6F) * 0.03F * hold;
            this.leftArm.zRot = this.leftArm.zRot - Mth.sin(age * 0.6F) * 0.03F * hold;
            break;
         case 11:
            this.rightArm.xRot -= 1.4F * env;
            this.leftArm.xRot -= 1.4F * env;
            this.rightFore.xRot -= 0.5F * recoil;
            this.leftFore.xRot -= 0.5F * recoil;
            break;
         case 12:
            this.rightArm.xRot -= 2.0F * env;
            this.leftArm.xRot -= 2.0F * env;
            this.rightFore.xRot -= 0.4F * env;
            this.leftFore.xRot -= 0.4F * env;
            this.rightArm.xRot += 1.4F * recoil;
            this.leftArm.xRot += 1.4F * recoil;
            break;
         case 13:
            float gather = 0.8F + Mth.sin(age * 0.3F) * 0.12F;
            this.rightArm.zRot += 0.9F * gather;
            this.leftArm.zRot -= 0.9F * gather;
            this.rightArm.xRot -= 0.5F * gather;
            this.leftArm.xRot -= 0.5F * gather;
            this.head.xRot += 0.4F;
            break;
         case 14:
            this.root.xRot += 0.5F;
            this.chest.xRot += 0.35F;
            this.abdomen.xRot += 0.2F;
            this.head.xRot += 0.6F;
            this.root.y += 6.0F;
            this.rightThigh.xRot++;
            this.rightShin.xRot++;
            this.leftThigh.xRot += 0.3F;
            this.rightArm.xRot += 0.6F;
            this.leftArm.xRot += 0.6F;
            this.rightArm.zRot = this.rightArm.zRot + Mth.sin(age * 1.3F) * 0.05F;
      }
   }
}
