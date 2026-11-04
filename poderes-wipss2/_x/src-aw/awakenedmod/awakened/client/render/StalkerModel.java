package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.entity.StalkerEntity;
import com.awakenedmod.awakened.core.AwId;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

public class StalkerModel extends HierarchicalModel<StalkerEntity> {
   public static final ModelLayerLocation LAYER = new ModelLayerLocation(AwId.of("stalker"), "main");
   private final ModelPart root;
   private final ModelPart body;
   private final ModelPart hipMass;
   private final ModelPart neckStump;
   private final ModelPart headGroup;
   private final ModelPart skullPlateTop;
   private final ModelPart skullPlateBottom;
   private final ModelPart frontLeftLeg;
   private final ModelPart frontRightLeg;
   private final ModelPart backLeftLeg;
   private final ModelPart backRightLeg;
   private final ModelPart tailBase;
   private final ModelPart tailBlade;

   public StalkerModel(ModelPart root) {
      this.root = root;
      this.body = root.getChild("body");
      this.hipMass = root.getChild("hip_mass");
      this.neckStump = root.getChild("neck_stump");
      this.headGroup = root.getChild("head_group");
      this.skullPlateTop = this.headGroup.getChild("skull_plate_top");
      this.skullPlateBottom = this.headGroup.getChild("skull_plate_bottom");
      this.frontLeftLeg = root.getChild("front_left_leg");
      this.frontRightLeg = root.getChild("front_right_leg");
      this.backLeftLeg = root.getChild("back_left_leg");
      this.backRightLeg = root.getChild("back_right_leg");
      this.tailBase = root.getChild("tail_base");
      this.tailBlade = this.tailBase.getChild("tail_blade");
   }

   public ModelPart root() {
      return this.root;
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      PartDefinition body = partdefinition.addOrReplaceChild(
         "body",
         CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -3.0F, -7.0F, 6.0F, 5.0F, 14.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 16.0F, 0.0F)
      );
      PartDefinition hip_mass = partdefinition.addOrReplaceChild(
         "hip_mass",
         CubeListBuilder.create().texOffs(0, 20).addBox(-3.5F, -3.0F, 3.0F, 7.0F, 6.0F, 5.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 16.0F, 2.0F)
      );
      PartDefinition neck_stump = partdefinition.addOrReplaceChild(
         "neck_stump",
         CubeListBuilder.create().texOffs(0, 32).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 15.0F, -7.0F)
      );
      PartDefinition head_group = partdefinition.addOrReplaceChild("head_group", CubeListBuilder.create(), PartPose.offset(0.0F, 16.0F, -10.0F));
      PartDefinition skull_plate_top = head_group.addOrReplaceChild(
         "skull_plate_top",
         CubeListBuilder.create().texOffs(20, 32).addBox(-3.0F, -2.0F, -3.0F, 6.0F, 2.0F, 5.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition skull_plate_bottom = head_group.addOrReplaceChild(
         "skull_plate_bottom",
         CubeListBuilder.create().texOffs(20, 40).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 2.0F, 5.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition front_left_leg = partdefinition.addOrReplaceChild(
         "front_left_leg",
         CubeListBuilder.create().texOffs(40, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F, new CubeDeformation(0.0F)),
         PartPose.offset(2.5F, 16.0F, -5.0F)
      );
      PartDefinition front_right_leg = partdefinition.addOrReplaceChild(
         "front_right_leg",
         CubeListBuilder.create().texOffs(40, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F, new CubeDeformation(0.0F)),
         PartPose.offset(-2.5F, 16.0F, -5.0F)
      );
      PartDefinition back_left_leg = partdefinition.addOrReplaceChild(
         "back_left_leg",
         CubeListBuilder.create().texOffs(48, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)),
         PartPose.offset(2.5F, 17.0F, 5.0F)
      );
      PartDefinition back_right_leg = partdefinition.addOrReplaceChild(
         "back_right_leg",
         CubeListBuilder.create().texOffs(48, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)),
         PartPose.offset(-2.5F, 17.0F, 5.0F)
      );
      PartDefinition tail_base = partdefinition.addOrReplaceChild(
         "tail_base",
         CubeListBuilder.create().texOffs(40, 20).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 7.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 15.0F, 7.0F)
      );
      PartDefinition tail_blade = tail_base.addOrReplaceChild(
         "tail_blade",
         CubeListBuilder.create().texOffs(56, 20).addBox(-0.5F, -1.0F, 5.0F, 1.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      return LayerDefinition.create(meshdefinition, 64, 64);
   }

   public void setupAnim(StalkerEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
      float DEG = (float) (Math.PI / 180.0);
      float attackProgress = entity.getAttackAnim(Mth.frac(ageInTicks));
      float gait = Mth.cos(limbSwing * 0.9F) * 1.4F * limbSwingAmount;
      float gaitOpp = Mth.cos(limbSwing * 0.9F + (float) Math.PI) * 1.4F * limbSwingAmount;
      this.frontLeftLeg.xRot = gait;
      this.frontRightLeg.xRot = gaitOpp;
      this.backLeftLeg.xRot = gaitOpp;
      this.backRightLeg.xRot = gait;
      this.body.y = 16.0F + Mth.abs(Mth.cos(limbSwing * 0.9F)) * limbSwingAmount * 1.5F;
      this.body.xRot = -0.15F;
      this.headGroup.y = 16.0F + Mth.cos(ageInTicks * 0.2F) * 0.6F;
      this.headGroup.yRot = netHeadYaw * (float) (Math.PI / 180.0);
      this.headGroup.xRot = headPitch * (float) (Math.PI / 180.0);
      float part = Mth.cos(ageInTicks * 0.1F) * 0.4F;
      this.skullPlateTop.y = -part;
      this.skullPlateBottom.y = part;
      this.tailBase.yRot = Mth.cos(ageInTicks * 0.25F + limbSwing) * 0.4F;
      this.tailBlade.zRot = Mth.cos(ageInTicks * 0.25F + limbSwing) * 0.2F;
      if (attackProgress > 0.0F) {
         this.headGroup.z = -10.0F - attackProgress * 2.0F;
         this.headGroup.xRot = this.headGroup.xRot + Mth.sin(attackProgress * (float) Math.PI) * 0.6F;
      } else {
         this.headGroup.z = -10.0F;
      }
   }
}
