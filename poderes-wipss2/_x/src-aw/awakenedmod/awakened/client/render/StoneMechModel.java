package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.earth.StoneMechEntity;
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

public class StoneMechModel extends HierarchicalModel<StoneMechEntity> {
   public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("awakened", "stone_mech"), "main");
   private static final float FEET_ANCHOR = 24.0F;
   private static final float DEG = (float) (Math.PI / 180.0);
   private static final float PI = (float) Math.PI;
   private final ModelPart root;
   private final ModelPart pelvis;
   private final ModelPart torso;
   private final ModelPart head;
   private final ModelPart rightPauldron;
   private final ModelPart leftPauldron;
   private final ModelPart rightArm;
   private final ModelPart leftArm;
   private final ModelPart rightThigh;
   private final ModelPart leftThigh;
   private final ModelPart rightShin;
   private final ModelPart leftShin;

   public StoneMechModel(ModelPart root) {
      this.root = root;
      this.pelvis = root.getChild("pelvis");
      this.torso = root.getChild("torso");
      this.head = this.torso.getChild("head");
      this.rightPauldron = root.getChild("right_pauldron");
      this.leftPauldron = root.getChild("left_pauldron");
      this.rightArm = root.getChild("right_arm");
      this.leftArm = root.getChild("left_arm");
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
         CubeListBuilder.create().texOffs(64, 16).addBox(-6.0F, -6.0F, -5.0F, 12.0F, 6.0F, 10.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, -19.0F, 0.0F)
      );
      PartDefinition torso = partdefinition.addOrReplaceChild(
         "torso",
         CubeListBuilder.create().texOffs(0, 30).addBox(-8.0F, -12.0F, -6.0F, 16.0F, 12.0F, 12.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, -29.0F, 0.0F)
      );
      PartDefinition head = torso.addOrReplaceChild(
         "head",
         CubeListBuilder.create().texOffs(72, 16).addBox(-4.0F, -7.0F, -4.0F, 8.0F, 7.0F, 8.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, -15.0F, 0.0F)
      );
      PartDefinition head_turf = head.addOrReplaceChild(
         "head_turf",
         CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 1.0F, 8.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition crest = head.addOrReplaceChild(
         "crest",
         CubeListBuilder.create().texOffs(104, 16).addBox(-2.0F, -10.0F, -3.0F, 4.0F, 3.0F, 7.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition visor = head.addOrReplaceChild(
         "visor",
         CubeListBuilder.create().texOffs(108, 58).addBox(-4.0F, -5.0F, -4.5F, 8.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition waist = torso.addOrReplaceChild(
         "waist",
         CubeListBuilder.create().texOffs(96, 40).addBox(-5.0F, 0.0F, -5.0F, 10.0F, 5.0F, 10.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition chest_plate = torso.addOrReplaceChild(
         "chest_plate",
         CubeListBuilder.create().texOffs(40, 16).addBox(-6.0F, -11.0F, -7.0F, 12.0F, 9.0F, 1.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition back_hump = torso.addOrReplaceChild(
         "back_hump",
         CubeListBuilder.create().texOffs(64, 36).addBox(-7.0F, -12.0F, 5.0F, 14.0F, 11.0F, 7.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition neck = torso.addOrReplaceChild(
         "neck",
         CubeListBuilder.create().texOffs(0, 48).addBox(-3.0F, -15.0F, -3.0F, 6.0F, 3.0F, 6.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition right_pauldron = partdefinition.addOrReplaceChild(
         "right_pauldron",
         CubeListBuilder.create().texOffs(0, 16).addBox(-5.0F, -4.0F, -5.0F, 10.0F, 8.0F, 10.0F, new CubeDeformation(0.0F)),
         PartPose.offset(-9.0F, -38.0F, 0.0F)
      );
      PartDefinition right_pauldron_turf = right_pauldron.addOrReplaceChild(
         "right_pauldron_turf",
         CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -5.0F, -5.0F, 10.0F, 1.0F, 10.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition left_pauldron = partdefinition.addOrReplaceChild(
         "left_pauldron",
         CubeListBuilder.create().texOffs(44, 29).addBox(-5.0F, -4.0F, -5.0F, 11.0F, 9.0F, 10.0F, new CubeDeformation(0.0F)),
         PartPose.offset(9.0F, -38.0F, 0.0F)
      );
      PartDefinition left_pauldron_turf = left_pauldron.addOrReplaceChild(
         "left_pauldron_turf",
         CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -5.0F, -5.0F, 10.0F, 1.0F, 10.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition right_arm = partdefinition.addOrReplaceChild(
         "right_arm",
         CubeListBuilder.create()
            .texOffs(96, 16)
            .addBox(-3.0F, -2.0F, -3.0F, 6.0F, 16.0F, 6.0F, new CubeDeformation(0.0F))
            .texOffs(0, 34)
            .addBox(-4.0F, 15.0F, -5.0F, 9.0F, 9.0F, 9.0F, new CubeDeformation(0.0F)),
         PartPose.offset(-11.0F, -40.0F, 0.0F)
      );
      PartDefinition left_arm = partdefinition.addOrReplaceChild(
         "left_arm",
         CubeListBuilder.create()
            .texOffs(64, 40)
            .addBox(-3.0F, -2.0F, -3.0F, 6.0F, 16.0F, 6.0F, new CubeDeformation(0.0F))
            .texOffs(40, 16)
            .addBox(-5.0F, 15.0F, -5.0F, 10.0F, 10.0F, 10.0F, new CubeDeformation(0.0F)),
         PartPose.offset(11.0F, -40.0F, 0.0F)
      );
      PartDefinition right_thigh = partdefinition.addOrReplaceChild(
         "right_thigh",
         CubeListBuilder.create().texOffs(88, 34).addBox(-3.5F, 0.0F, -3.5F, 7.0F, 9.0F, 7.0F, new CubeDeformation(0.0F)),
         PartPose.offset(-5.0F, -19.0F, 0.0F)
      );
      PartDefinition right_shin = right_thigh.addOrReplaceChild(
         "right_shin",
         CubeListBuilder.create().texOffs(88, 16).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 6.0F, 8.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 9.0F, 0.0F)
      );
      PartDefinition right_foot = right_shin.addOrReplaceChild(
         "right_foot",
         CubeListBuilder.create().texOffs(40, 0).addBox(-4.0F, 0.0F, -5.0F, 8.0F, 4.0F, 11.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 6.0F, 0.0F)
      );
      PartDefinition left_thigh = partdefinition.addOrReplaceChild(
         "left_thigh",
         CubeListBuilder.create().texOffs(88, 34).mirror().addBox(-3.5F, 0.0F, -3.5F, 7.0F, 9.0F, 7.0F, new CubeDeformation(0.0F)).mirror(false),
         PartPose.offset(5.0F, -19.0F, 0.0F)
      );
      PartDefinition left_shin = left_thigh.addOrReplaceChild(
         "left_shin",
         CubeListBuilder.create().texOffs(88, 16).mirror().addBox(-4.0F, 0.0F, -4.0F, 8.0F, 6.0F, 8.0F, new CubeDeformation(0.0F)).mirror(false),
         PartPose.offset(0.0F, 9.0F, 0.0F)
      );
      PartDefinition left_foot = left_shin.addOrReplaceChild(
         "left_foot",
         CubeListBuilder.create().texOffs(40, 0).mirror().addBox(-4.0F, 0.0F, -5.0F, 8.0F, 4.0F, 11.0F, new CubeDeformation(0.0F)).mirror(false),
         PartPose.offset(0.0F, 6.0F, 0.0F)
      );
      return LayerDefinition.create(meshdefinition, 128, 64);
   }

   public void setupAnim(StoneMechEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitchDeg) {
      float gait = Math.min(limbSwingAmount, 1.0F);
      byte action = entity.getAction();
      float p = entity.getActionProgress();
      this.root.y = 24.0F;
      this.root.xRot = 0.0F;
      this.root.zRot = Mth.cos(ageInTicks * 0.04F) * 0.015F;
      this.torso.xRot = 0.0F;
      this.torso.zRot = 0.0F;
      this.rightArm.xRot = this.leftArm.xRot = 0.0F;
      this.rightArm.zRot = this.leftArm.zRot = 0.0F;
      this.rightPauldron.zRot = this.leftPauldron.zRot = 0.0F;
      this.rightThigh.xRot = this.leftThigh.xRot = 0.0F;
      this.rightThigh.zRot = this.leftThigh.zRot = 0.0F;
      this.rightShin.xRot = this.leftShin.xRot = 0.0F;
      this.torso.y = -29.0F + Mth.cos(ageInTicks * 0.06F) * 0.4F;
      this.rightArm.zRot = 0.08F;
      this.leftArm.zRot = -0.08F;
      this.head.yRot = netHeadYaw * (float) (Math.PI / 180.0) * 0.6F;
      this.head.xRot = headPitchDeg * (float) (Math.PI / 180.0) * 0.5F;
      this.rightThigh.xRot = this.rightThigh.xRot + Mth.cos(limbSwing * 0.5F) * 0.6F * gait;
      this.leftThigh.xRot = this.leftThigh.xRot + Mth.cos(limbSwing * 0.5F + (float) Math.PI) * 0.6F * gait;
      this.rightShin.xRot = this.rightShin.xRot + Math.max(0.0F, Mth.cos(limbSwing * 0.5F + 1.2F)) * 0.45F * gait;
      this.leftShin.xRot = this.leftShin.xRot + Math.max(0.0F, Mth.cos(limbSwing * 0.5F + (float) Math.PI + 1.2F)) * 0.45F * gait;
      this.rightArm.xRot = this.rightArm.xRot + Mth.cos(limbSwing * 0.5F + (float) Math.PI) * 0.35F * gait;
      this.leftArm.xRot = this.leftArm.xRot + Mth.cos(limbSwing * 0.5F) * 0.35F * gait;
      this.root.y = this.root.y + Math.abs(Mth.cos(limbSwing * 0.5F)) * 0.6F * gait;
      this.applyAction(action, p, ageInTicks);
   }

   private static float smooth(float t) {
      t = Mth.clamp(t, 0.0F, 1.0F);
      return t * t * (3.0F - 2.0F * t);
   }

   private void applyAction(byte action, float p, float age) {
      float env = p < 0.5F ? smooth(p / 0.5F) : smooth((1.0F - p) / 0.5F);
      float recoil = smooth(Mth.clamp((p - 0.45F) / 0.1F, 0.0F, 1.0F)) * (1.0F - smooth(Mth.clamp((p - 0.62F) / 0.38F, 0.0F, 1.0F)));
      float rise = smooth(p);
      switch (action) {
         case 1:
            float crouch = 1.0F - rise;
            this.root.y += 18.0F * crouch;
            this.rightThigh.xRot += 1.4F * crouch;
            this.leftThigh.xRot += 1.4F * crouch;
            this.rightShin.xRot += 1.3F * crouch;
            this.leftShin.xRot += 1.3F * crouch;
            this.torso.xRot += 0.5F * crouch;
            this.head.xRot += 0.4F * crouch;
            this.rightArm.xRot += 0.6F * crouch;
            this.leftArm.xRot += 0.6F * crouch;
            break;
         case 2:
            this.rightArm.xRot -= 2.3F * env;
            this.leftArm.xRot -= 2.3F * env;
            this.rightArm.xRot += 1.7F * recoil;
            this.leftArm.xRot += 1.7F * recoil;
            this.torso.xRot += 0.18F * recoil;
            this.root.y += 1.2F * recoil;
            break;
         case 3:
            this.rightThigh.xRot -= 0.9F * env;
            this.leftThigh.xRot -= 0.9F * env;
            this.rightShin.xRot += 1.0F * env;
            this.leftShin.xRot += 1.0F * env;
            this.rightArm.xRot -= 1.0F * env;
            this.leftArm.xRot -= 1.0F * env;
            this.rightArm.xRot += 1.6F * recoil;
            this.leftArm.xRot += 1.6F * recoil;
            this.torso.xRot += 0.22F * recoil;
            this.root.y += 1.6F * recoil;
            break;
         case 4:
            this.rightArm.xRot -= 1.6F * env;
            this.rightArm.zRot += 0.3F * env;
            this.rightArm.xRot += 1.9F * recoil;
            this.torso.zRot += 0.06F * env;
      }
   }
}
