package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.entity.EnforcerEntity;
import com.awakenedmod.awakened.content.entity.OverseerEntity;
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
import net.minecraft.world.entity.Mob;

public class OverseerModel<T extends Mob> extends HierarchicalModel<T> {
   public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("awakened", "overseer"), "main");
   private static final float FEET_ANCHOR = 24.0F;
   private final ModelPart root;
   private final ModelPart pelvis;
   private final ModelPart chestLower;
   private final ModelPart chestUpper;
   private final ModelPart coreSeam;
   private final ModelPart sensorBand;
   private final ModelPart backVent;
   private final ModelPart rightArm;
   private final ModelPart leftArm;
   private final ModelPart rightFore;
   private final ModelPart leftFore;
   private final ModelPart rightThigh;
   private final ModelPart leftThigh;
   private final ModelPart rightShin;
   private final ModelPart leftShin;
   private final ModelPart pylonGroupR;
   private final ModelPart pylonGroupL;

   public OverseerModel(ModelPart root) {
      this.root = root;
      this.pelvis = root.getChild("pelvis");
      this.chestLower = root.getChild("chest_lower");
      this.chestUpper = root.getChild("chest_upper");
      this.coreSeam = this.chestLower.getChild("core_seam");
      this.sensorBand = this.chestUpper.getChild("sensor_band");
      this.backVent = this.chestUpper.getChild("back_vent");
      this.rightArm = root.getChild("right_arm");
      this.leftArm = root.getChild("left_arm");
      this.rightFore = this.rightArm.getChild("right_fore");
      this.leftFore = this.leftArm.getChild("left_fore");
      this.rightThigh = root.getChild("right_thigh");
      this.leftThigh = root.getChild("left_thigh");
      this.rightShin = this.rightThigh.getChild("right_shin");
      this.leftShin = this.leftThigh.getChild("left_shin");
      this.pylonGroupR = root.getChild("pylon_group_r");
      this.pylonGroupL = root.getChild("pylon_group_l");
   }

   public ModelPart root() {
      return this.root;
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      PartDefinition pelvis = partdefinition.addOrReplaceChild(
         "pelvis",
         CubeListBuilder.create().texOffs(0, 0).addBox(-9.0F, -8.0F, -5.0F, 18.0F, 8.0F, 10.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, -24.0F, 0.0F)
      );
      PartDefinition chest_lower = partdefinition.addOrReplaceChild(
         "chest_lower",
         CubeListBuilder.create().texOffs(0, 28).addBox(-8.0F, -12.0F, -5.0F, 16.0F, 12.0F, 10.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, -32.0F, 0.0F)
      );
      PartDefinition core_seam = chest_lower.addOrReplaceChild(
         "core_seam",
         CubeListBuilder.create().texOffs(64, 0).addBox(-1.5F, -20.0F, -6.5F, 3.0F, 20.0F, 2.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition chest_upper = partdefinition.addOrReplaceChild(
         "chest_upper",
         CubeListBuilder.create().texOffs(0, 52).addBox(-11.0F, -12.0F, -6.0F, 22.0F, 12.0F, 12.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, -44.0F, 0.0F)
      );
      PartDefinition sensor_band = chest_upper.addOrReplaceChild(
         "sensor_band",
         CubeListBuilder.create().texOffs(80, 0).addBox(-6.0F, -15.0F, -4.0F, 12.0F, 3.0F, 8.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition back_vent = chest_upper.addOrReplaceChild(
         "back_vent",
         CubeListBuilder.create().texOffs(96, 40).addBox(-6.0F, -16.0F, 4.0F, 12.0F, 16.0F, 2.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition right_arm = partdefinition.addOrReplaceChild(
         "right_arm",
         CubeListBuilder.create()
            .texOffs(0, 80)
            .addBox(-5.0F, -4.0F, -5.0F, 10.0F, 8.0F, 10.0F, new CubeDeformation(0.0F))
            .texOffs(40, 80)
            .addBox(-4.0F, 0.0F, -4.0F, 8.0F, 14.0F, 8.0F, new CubeDeformation(0.0F)),
         PartPose.offset(-13.0F, -52.0F, 0.0F)
      );
      PartDefinition right_fore = right_arm.addOrReplaceChild(
         "right_fore",
         CubeListBuilder.create().texOffs(80, 40).addBox(-5.0F, 0.0F, -5.0F, 10.0F, 12.0F, 10.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 14.0F, 0.0F)
      );
      PartDefinition left_arm = partdefinition.addOrReplaceChild(
         "left_arm",
         CubeListBuilder.create()
            .texOffs(0, 80)
            .mirror()
            .addBox(-5.0F, -4.0F, -5.0F, 10.0F, 8.0F, 10.0F, new CubeDeformation(0.0F))
            .mirror(false)
            .texOffs(40, 80)
            .mirror()
            .addBox(-4.0F, 0.0F, -4.0F, 8.0F, 14.0F, 8.0F, new CubeDeformation(0.0F))
            .mirror(false),
         PartPose.offset(13.0F, -52.0F, 0.0F)
      );
      PartDefinition left_fore = left_arm.addOrReplaceChild(
         "left_fore",
         CubeListBuilder.create().texOffs(80, 40).mirror().addBox(-5.0F, 0.0F, -5.0F, 10.0F, 12.0F, 10.0F, new CubeDeformation(0.0F)).mirror(false),
         PartPose.offset(0.0F, 14.0F, 0.0F)
      );
      PartDefinition right_thigh = partdefinition.addOrReplaceChild(
         "right_thigh",
         CubeListBuilder.create().texOffs(0, 104).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 12.0F, 8.0F, new CubeDeformation(0.0F)),
         PartPose.offset(-5.0F, -24.0F, 0.0F)
      );
      PartDefinition right_shin = right_thigh.addOrReplaceChild(
         "right_shin",
         CubeListBuilder.create().texOffs(40, 104).addBox(-5.0F, 0.0F, -5.0F, 10.0F, 12.0F, 10.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 12.0F, 0.0F)
      );
      PartDefinition left_thigh = partdefinition.addOrReplaceChild(
         "left_thigh",
         CubeListBuilder.create().texOffs(0, 104).mirror().addBox(-4.0F, 0.0F, -4.0F, 8.0F, 12.0F, 8.0F, new CubeDeformation(0.0F)).mirror(false),
         PartPose.offset(5.0F, -24.0F, 0.0F)
      );
      PartDefinition left_shin = left_thigh.addOrReplaceChild(
         "left_shin",
         CubeListBuilder.create().texOffs(40, 104).mirror().addBox(-5.0F, 0.0F, -5.0F, 10.0F, 12.0F, 10.0F, new CubeDeformation(0.0F)).mirror(false),
         PartPose.offset(0.0F, 12.0F, 0.0F)
      );
      PartDefinition pylon_group_r = partdefinition.addOrReplaceChild("pylon_group_r", CubeListBuilder.create(), PartPose.offset(-15.0F, -60.0F, 0.0F));
      PartDefinition pylon_r = pylon_group_r.addOrReplaceChild(
         "pylon_r",
         CubeListBuilder.create()
            .texOffs(96, 80)
            .addBox(-5.0F, -1.0F, -5.0F, 10.0F, 6.0F, 10.0F, new CubeDeformation(0.0F))
            .texOffs(96, 96)
            .addBox(-3.0F, -7.0F, -3.0F, 6.0F, 6.0F, 6.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition pylon_group_l = partdefinition.addOrReplaceChild("pylon_group_l", CubeListBuilder.create(), PartPose.offset(15.0F, -60.0F, 0.0F));
      PartDefinition pylon_l = pylon_group_l.addOrReplaceChild(
         "pylon_l",
         CubeListBuilder.create()
            .texOffs(96, 80)
            .addBox(-5.0F, -1.0F, -5.0F, 10.0F, 6.0F, 10.0F, new CubeDeformation(0.0F))
            .texOffs(96, 96)
            .addBox(-3.0F, -7.0F, -3.0F, 6.0F, 6.0F, 6.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      return LayerDefinition.create(meshdefinition, 128, 128);
   }

   private static CubeListBuilder faceted() {
      return CubeListBuilder.create()
         .texOffs(96, 80)
         .addBox(-5.0F, -1.0F, -5.0F, 10.0F, 6.0F, 10.0F)
         .texOffs(96, 96)
         .addBox(-3.0F, -7.0F, -3.0F, 6.0F, 6.0F, 6.0F);
   }

   public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
      byte phase = 0;
      if (entity instanceof OverseerEntity overseer) {
         phase = overseer.getPhase();
      } else if (entity instanceof EnforcerEntity enforcer) {
         phase = enforcer.getPhase();
      }

      this.applyPose(limbSwing, Math.min(limbSwingAmount, 1.0F), ageInTicks, netHeadYaw, entity.getAttackAnim(Mth.frac(ageInTicks)), phase);
   }

   private void applyPose(float swing, float amount, float age, float headYaw, float attackProgress, byte phase) {
      float DEG = (float) (Math.PI / 180.0);
      this.root.y = 24.0F;
      this.chestUpper.y = -44.0F + Mth.cos(age * 0.08F) * 0.5F;
      this.root.zRot = Mth.cos(age * 0.05F) * 0.04F;
      this.leftThigh.xRot = Mth.cos(swing * 0.6F) * 0.8F * amount;
      this.rightThigh.xRot = Mth.cos(swing * 0.6F + (float) Math.PI) * 0.8F * amount;
      this.leftShin.xRot = Math.max(0.0F, Mth.cos(swing * 0.6F + 1.0F)) * 0.4F * amount;
      this.rightShin.xRot = Math.max(0.0F, Mth.cos(swing * 0.6F + (float) Math.PI + 1.0F)) * 0.4F * amount;
      this.leftArm.xRot = Mth.cos(swing * 0.6F + (float) Math.PI) * 0.5F * amount;
      this.rightArm.xRot = Mth.cos(swing * 0.6F) * 0.5F * amount;
      this.sensorBand.yRot = headYaw * (float) (Math.PI / 180.0) * 0.4F;
      float spin = age * 0.1F;
      float tip = 0.0F;
      float spinBoost = 1.0F;
      if (phase == 1) {
         tip = -0.3F;
         spinBoost = 2.2F;
      }

      this.pylonGroupR.yRot = spin * spinBoost;
      this.pylonGroupL.yRot = -spin * spinBoost;
      this.pylonGroupR.xRot = tip;
      this.pylonGroupL.xRot = tip;
      float hover = Mth.sin(age * 0.12F) * 0.6F;
      this.pylonGroupR.y = -60.0F + hover;
      this.pylonGroupL.y = -60.0F + hover;
      if (attackProgress > 0.0F) {
         float slam = -2.0F * Mth.sin(attackProgress * (float) Math.PI);
         this.rightArm.xRot += slam;
         this.leftArm.xRot += slam;
         float fore = -0.4F * Mth.sin(attackProgress * (float) Math.PI);
         this.rightFore.xRot = fore;
         this.leftFore.xRot = fore;
      } else {
         this.rightFore.xRot = 0.0F;
         this.leftFore.xRot = 0.0F;
      }

      if (phase == 2) {
         float dip = 1.2F;
         this.pelvis.y = -24.0F + dip;
         this.chestLower.y = -32.0F + dip;
         this.chestUpper.y += dip;
      } else {
         this.pelvis.y = -24.0F;
         this.chestLower.y = -32.0F;
      }

      if (phase == 3) {
         this.rightArm.xRot++;
         this.leftArm.xRot++;
         this.root.xRot = -0.28F;
      } else {
         this.root.xRot = 0.0F;
      }

      if (phase == 4) {
         this.rightArm.xRot -= 2.4F;
         this.leftArm.xRot -= 2.4F;
         this.rightFore.xRot = -0.5F;
         this.leftFore.xRot = -0.5F;
      }
   }
}
