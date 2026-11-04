package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public final class ExosavantSuitModel {
   public static final ModelLayerLocation LAYER = new ModelLayerLocation(AwId.of("exosavant_suit"), "main");
   public static final String WAIST = "Waist";
   public static final String HEAD = "Head";
   public static final String BODY = "Body";
   public static final String LEFT_ARM = "LeftArm";
   public static final String RIGHT_ARM = "RightArm";
   public static final String LEFT_LEG = "LeftLeg";
   public static final String RIGHT_LEG = "RightLeg";
   public static final String LEFT_BOOT = "LeftBoot";
   public static final String RIGHT_BOOT = "RightBoot";
   public static final String GAUNTLETS = "Gauntlets";
   public static final String LEFT_GAUNTLET = "LeftGauntlet";
   public static final String RIGHT_GAUNTLET = "RightGauntlet";
   public static final String RIGHT_CANNON = "RightCannon";

   private ExosavantSuitModel() {
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      PartDefinition Waist = partdefinition.addOrReplaceChild("Waist", CubeListBuilder.create(), PartPose.offset(0.0F, 12.0F, 0.0F));
      PartDefinition Head = Waist.addOrReplaceChild("Head", CubeListBuilder.create(), PartPose.offset(0.0F, 12.0F, 0.0F));
      PartDefinition Visor = Head.addOrReplaceChild(
         "Visor",
         CubeListBuilder.create()
            .texOffs(88, 67)
            .addBox(2.0F, 3.0F, -1.75F, 2.0F, 3.0F, 6.0F, new CubeDeformation(0.51F))
            .texOffs(36, 94)
            .addBox(-4.0F, 3.0F, -1.75F, 2.0F, 3.0F, 6.0F, new CubeDeformation(0.51F))
            .texOffs(88, 76)
            .addBox(-4.0F, 3.0F, -3.75F, 8.0F, 3.0F, 1.0F, new CubeDeformation(0.51F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition Mask = Head.addOrReplaceChild(
         "Mask",
         CubeListBuilder.create()
            .texOffs(62, 57)
            .addBox(-1.0F, 0.0F, -4.75F, 2.0F, 3.0F, 1.0F, new CubeDeformation(0.65F))
            .texOffs(68, 26)
            .addBox(-4.0F, 0.0F, -4.75F, 8.0F, 2.0F, 1.0F, new CubeDeformation(0.5F))
            .texOffs(86, 32)
            .addBox(-4.0F, 0.0F, -2.75F, 8.0F, 2.0F, 3.0F, new CubeDeformation(0.5F))
            .texOffs(48, 29)
            .addBox(-3.0F, 0.0F, -4.75F, 6.0F, 1.0F, 1.0F, new CubeDeformation(0.6F))
            .texOffs(102, 43)
            .addBox(-4.0F, 0.0F, -4.75F, 8.0F, 1.0F, 1.0F, new CubeDeformation(0.575F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition HelmetBack = Head.addOrReplaceChild(
         "HelmetBack",
         CubeListBuilder.create()
            .texOffs(64, 29)
            .addBox(-4.5F, 1.0F, 4.25F, 9.0F, 7.0F, 2.0F, new CubeDeformation(-0.2F))
            .texOffs(86, 18)
            .addBox(-3.5F, 1.0F, 4.25F, 7.0F, 7.0F, 2.0F, new CubeDeformation(-0.1F))
            .texOffs(0, 84)
            .addBox(-3.5F, 0.0F, 4.25F, 7.0F, 9.0F, 2.0F, new CubeDeformation(-0.2F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition MaskBack = Head.addOrReplaceChild(
         "MaskBack",
         CubeListBuilder.create().texOffs(86, 27).addBox(-4.0F, 0.0F, 1.25F, 8.0F, 2.0F, 3.0F, new CubeDeformation(0.5F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition HelmetLeft = Head.addOrReplaceChild(
         "HelmetLeft",
         CubeListBuilder.create()
            .texOffs(0, 72)
            .addBox(4.0F, 2.0F, -2.75F, 2.0F, 5.0F, 7.0F, new CubeDeformation(-0.2F))
            .texOffs(28, 0)
            .addBox(3.5F, 1.0F, -3.75F, 2.0F, 7.0F, 9.0F, new CubeDeformation(-0.1F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition HelmetRight = Head.addOrReplaceChild(
         "HelmetRight",
         CubeListBuilder.create()
            .texOffs(50, 72)
            .addBox(-6.0F, 2.0F, -2.75F, 2.0F, 5.0F, 7.0F, new CubeDeformation(-0.2F))
            .texOffs(26, 17)
            .addBox(-5.5F, 1.0F, -3.75F, 2.0F, 7.0F, 9.0F, new CubeDeformation(-0.1F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition HelmetUp = Head.addOrReplaceChild(
         "HelmetUp",
         CubeListBuilder.create()
            .texOffs(0, 17)
            .addBox(-2.0F, 6.0F, -4.75F, 4.0F, 4.0F, 9.0F, new CubeDeformation(0.5F))
            .texOffs(50, 61)
            .addBox(3.0F, 7.0F, -4.75F, 1.0F, 2.0F, 9.0F, new CubeDeformation(0.55F))
            .texOffs(62, 46)
            .addBox(-4.0F, 7.0F, -4.75F, 1.0F, 2.0F, 9.0F, new CubeDeformation(0.55F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition Body = Waist.addOrReplaceChild("Body", CubeListBuilder.create(), PartPose.offset(0.0F, 12.0F, 0.0F));
      PartDefinition Front = Body.addOrReplaceChild(
         "Front",
         CubeListBuilder.create()
            .texOffs(82, 38)
            .addBox(-4.0F, -12.5F, -3.0F, 8.0F, 7.0F, 2.0F, new CubeDeformation(-0.51F))
            .texOffs(54, 84)
            .addBox(3.0F, -12.5F, -3.0F, 2.0F, 13.0F, 3.0F, new CubeDeformation(-0.51F))
            .texOffs(36, 61)
            .addBox(-5.0F, -12.5F, -3.0F, 2.0F, 13.0F, 5.0F, new CubeDeformation(-0.51F))
            .texOffs(104, 67)
            .addBox(-2.0F, -7.0F, -3.25F, 4.0F, 7.0F, 1.0F, new CubeDeformation(-0.26F))
            .texOffs(64, 104)
            .addBox(1.0F, -8.0F, -3.0F, 4.0F, 8.0F, 1.0F, new CubeDeformation(-0.25F))
            .texOffs(104, 18)
            .addBox(-5.0F, -8.0F, -3.0F, 4.0F, 8.0F, 1.0F, new CubeDeformation(-0.25F))
            .texOffs(94, 108)
            .addBox(-2.0F, -4.0F, -3.75F, 4.0F, 4.0F, 1.0F, new CubeDeformation(-0.19F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition Back = Body.addOrReplaceChild(
         "Back",
         CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -12.5F, -1.0F, 10.0F, 13.0F, 4.0F, new CubeDeformation(-0.501F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition Thrusters = Back.addOrReplaceChild(
         "Thrusters",
         CubeListBuilder.create()
            .texOffs(86, 90)
            .addBox(1.0F, -7.5F, 1.0F, 4.0F, 8.0F, 3.0F, new CubeDeformation(-0.51F))
            .texOffs(64, 93)
            .addBox(-5.0F, -7.5F, 1.0F, 4.0F, 8.0F, 3.0F, new CubeDeformation(-0.51F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition BackPlate = Back.addOrReplaceChild(
         "BackPlate",
         CubeListBuilder.create()
            .texOffs(36, 79)
            .addBox(-2.0F, -6.5F, 2.0F, 4.0F, 2.0F, 3.0F, new CubeDeformation(-0.51F))
            .texOffs(105, 114)
            .addBox(-2.0F, -5.5F, 3.0F, 4.0F, 6.0F, 3.0F, new CubeDeformation(-0.51F))
            .texOffs(106, 75)
            .addBox(-2.0F, -7.5F, 2.0F, 4.0F, 2.0F, 2.0F, new CubeDeformation(-0.51F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition FrontPlate1 = Body.addOrReplaceChild(
         "FrontPlate1",
         CubeListBuilder.create().texOffs(84, 107).addBox(-0.2F, -5.0F, -3.5F, 4.0F, 5.0F, 1.0F, new CubeDeformation(-0.2F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition FrontPlate2 = Body.addOrReplaceChild(
         "FrontPlate2",
         CubeListBuilder.create().texOffs(108, 27).addBox(-3.8F, -5.0F, -3.5F, 4.0F, 5.0F, 1.0F, new CubeDeformation(-0.2F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition LeftArm = Waist.addOrReplaceChild("LeftArm", CubeListBuilder.create(), PartPose.offset(4.0F, 10.0F, 0.0F));
      PartDefinition LeftArmFront = LeftArm.addOrReplaceChild(
         "LeftArmFront",
         CubeListBuilder.create()
            .texOffs(86, 0)
            .addBox(0.0F, -2.0F, -3.0F, 5.0F, 5.0F, 4.0F, new CubeDeformation(-0.5F))
            .texOffs(44, 46)
            .addBox(0.0F, -11.0F, -3.0F, 5.0F, 11.0F, 4.0F, new CubeDeformation(-0.75F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition LeftArmBack = LeftArm.addOrReplaceChild(
         "LeftArmBack",
         CubeListBuilder.create()
            .texOffs(49, 0)
            .addBox(-1.0F, -11.0F, -1.0F, 6.0F, 11.0F, 4.0F, new CubeDeformation(-0.751F))
            .texOffs(86, 81)
            .addBox(0.0F, -2.0F, -1.0F, 5.0F, 5.0F, 4.0F, new CubeDeformation(-0.51F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition LeftArmSide = LeftArm.addOrReplaceChild(
         "LeftArmSide",
         CubeListBuilder.create()
            .texOffs(100, 47)
            .addBox(3.0F, -4.0F, -2.0F, 3.0F, 6.0F, 4.0F, new CubeDeformation(-0.45F))
            .texOffs(68, 13)
            .addBox(2.0F, -4.0F, -3.0F, 3.0F, 7.0F, 6.0F, new CubeDeformation(-0.39F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition ShoulderCannonLeft = LeftArm.addOrReplaceChild(
         "ShoulderCannonLeft",
         CubeListBuilder.create()
            .texOffs(82, 47)
            .addBox(1.0F, -0.5F, -3.5F, 4.0F, 5.0F, 5.0F, new CubeDeformation(-0.75F))
            .texOffs(0, 95)
            .addBox(3.0F, 0.5F, -3.5F, 3.0F, 4.0F, 5.0F, new CubeDeformation(-0.755F))
            .texOffs(12, 104)
            .addBox(1.5F, -2.5F, -2.5F, 3.0F, 7.0F, 3.0F, new CubeDeformation(-0.8F)),
         PartPose.offset(0.0F, -1.0F, 1.0F)
      );
      PartDefinition RightArm = Waist.addOrReplaceChild("RightArm", CubeListBuilder.create(), PartPose.offset(-4.0F, 10.0F, 0.0F));
      PartDefinition RightArmSide = RightArm.addOrReplaceChild(
         "RightArmSide",
         CubeListBuilder.create()
            .texOffs(68, 0)
            .addBox(-5.0F, -4.0F, -3.0F, 3.0F, 7.0F, 6.0F, new CubeDeformation(-0.39F))
            .texOffs(52, 100)
            .addBox(-6.0F, -4.0F, -2.0F, 2.0F, 6.0F, 4.0F, new CubeDeformation(-0.45F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition ShoulderCannonRight = RightArm.addOrReplaceChild(
         "ShoulderCannonRight",
         CubeListBuilder.create()
            .texOffs(16, 95)
            .addBox(-6.0F, -0.5F, -2.5F, 3.0F, 4.0F, 5.0F, new CubeDeformation(-0.755F))
            .texOffs(36, 84)
            .addBox(-5.0F, -1.5F, -2.5F, 4.0F, 5.0F, 5.0F, new CubeDeformation(-0.75F))
            .texOffs(104, 57)
            .addBox(-4.5F, -4.0F, -1.5F, 3.0F, 7.0F, 3.0F, new CubeDeformation(-0.8F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition RightArmFront = RightArm.addOrReplaceChild(
         "RightArmFront",
         CubeListBuilder.create()
            .texOffs(86, 9)
            .addBox(-5.0F, -2.0F, -3.0F, 5.0F, 5.0F, 4.0F, new CubeDeformation(-0.5F))
            .texOffs(0, 57)
            .addBox(-5.0F, -11.0F, -3.0F, 5.0F, 11.0F, 4.0F, new CubeDeformation(-0.75F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition RightArmBack = RightArm.addOrReplaceChild(
         "RightArmBack",
         CubeListBuilder.create()
            .texOffs(100, 100)
            .addBox(-5.0F, -2.0F, 0.0F, 5.0F, 5.0F, 3.0F, new CubeDeformation(-0.51F))
            .texOffs(18, 60)
            .addBox(-5.0F, -11.0F, -1.0F, 6.0F, 11.0F, 4.0F, new CubeDeformation(-0.751F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition LeftLeg = partdefinition.addOrReplaceChild("LeftLeg", CubeListBuilder.create(), PartPose.offset(2.0F, 12.0F, 0.0F));
      PartDefinition LeftLegFront = LeftLeg.addOrReplaceChild(
         "LeftLegFront",
         CubeListBuilder.create()
            .texOffs(0, 44)
            .addBox(-3.0F, -8.0F, -3.0F, 6.0F, 9.0F, 4.0F, new CubeDeformation(-1.0F))
            .texOffs(70, 69)
            .addBox(-2.0F, -7.23F, -3.0F, 5.0F, 8.0F, 4.0F, new CubeDeformation(-0.69F))
            .texOffs(74, 107)
            .addBox(-1.0F, -6.25F, -3.3F, 3.0F, 6.0F, 2.0F, new CubeDeformation(-0.5F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition LeftLegBack = LeftLeg.addOrReplaceChild(
         "LeftLegBack",
         CubeListBuilder.create()
            .texOffs(20, 47)
            .addBox(-3.0F, -8.0F, -1.0F, 6.0F, 9.0F, 4.0F, new CubeDeformation(-1.0F))
            .texOffs(18, 75)
            .addBox(-2.0F, -7.23F, -1.0F, 5.0F, 8.0F, 4.0F, new CubeDeformation(-0.7F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition LeftLegSide = LeftLeg.addOrReplaceChild(
         "LeftLegSide",
         CubeListBuilder.create().texOffs(0, 104).addBox(1.5F, -6.25F, -2.3F, 2.0F, 6.0F, 4.0F, new CubeDeformation(-0.5F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition RightLeg = partdefinition.addOrReplaceChild("RightLeg", CubeListBuilder.create(), PartPose.offset(-2.0F, 12.0F, 0.0F));
      PartDefinition RightLegFront = RightLeg.addOrReplaceChild(
         "RightLegFront",
         CubeListBuilder.create()
            .texOffs(104, 80)
            .addBox(-2.0F, -6.25F, -3.3F, 3.0F, 6.0F, 2.0F, new CubeDeformation(-0.5F))
            .texOffs(44, 33)
            .addBox(-3.0F, -8.0F, -3.0F, 6.0F, 9.0F, 4.0F, new CubeDeformation(-1.0F))
            .texOffs(70, 57)
            .addBox(-3.0F, -7.23F, -3.0F, 5.0F, 8.0F, 4.0F, new CubeDeformation(-0.69F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition RightLegBack = RightLeg.addOrReplaceChild(
         "RightLegBack",
         CubeListBuilder.create()
            .texOffs(48, 16)
            .addBox(-3.0F, -8.0F, -1.0F, 6.0F, 9.0F, 4.0F, new CubeDeformation(-1.0F))
            .texOffs(68, 81)
            .addBox(-3.0F, -7.23F, -1.0F, 5.0F, 8.0F, 4.0F, new CubeDeformation(-0.7F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition RightLegSide = RightLeg.addOrReplaceChild(
         "RightLegSide",
         CubeListBuilder.create().texOffs(32, 103).addBox(-3.5F, -6.25F, -2.3F, 2.0F, 6.0F, 4.0F, new CubeDeformation(-0.5F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition LeftBoot = partdefinition.addOrReplaceChild(
         "LeftBoot",
         CubeListBuilder.create()
            .texOffs(18, 87)
            .addBox(-2.0F, -12.0F, -3.0F, 4.0F, 3.0F, 5.0F, new CubeDeformation(0.05F))
            .texOffs(78, 101)
            .addBox(-2.0F, -9.0F, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.041F)),
         PartPose.offset(2.0F, 12.0F, 0.0F)
      );
      PartDefinition LeftBootPlate = LeftBoot.addOrReplaceChild(
         "LeftBootPlate",
         CubeListBuilder.create().texOffs(44, 103).addBox(1.0F, -10.0F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(-0.25F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition RightBoot = partdefinition.addOrReplaceChild(
         "RightBoot",
         CubeListBuilder.create().texOffs(64, 38).addBox(-2.0F, -12.0F, -3.0F, 4.0F, 3.0F, 5.0F, new CubeDeformation(0.051F)),
         PartPose.offset(-2.0F, 12.0F, 0.0F)
      );
      PartDefinition RightBootPlate = RightBoot.addOrReplaceChild(
         "RightBootPlate",
         CubeListBuilder.create()
            .texOffs(24, 104)
            .addBox(-3.0F, -10.0F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(-0.25F))
            .texOffs(102, 37)
            .addBox(-2.0F, -9.0F, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.04F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition Gauntlets = partdefinition.addOrReplaceChild("Gauntlets", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
      PartDefinition LeftGauntlet = Gauntlets.addOrReplaceChild(
         "LeftGauntlet",
         CubeListBuilder.create().texOffs(0, 30).addBox(0.0F, -11.0F, -3.0F, 5.0F, 8.0F, 6.0F, new CubeDeformation(-0.5F)),
         PartPose.offset(4.0F, -2.0F, 0.0F)
      );
      PartDefinition RightGauntlet = Gauntlets.addOrReplaceChild(
         "RightGauntlet",
         CubeListBuilder.create().texOffs(22, 33).addBox(-5.0F, -11.0F, -3.0F, 5.0F, 8.0F, 6.0F, new CubeDeformation(-0.5F)),
         PartPose.offset(-4.0F, -2.0F, 0.0F)
      );
      PartDefinition RightCannon = RightGauntlet.addOrReplaceChild("RightCannon", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
      PartDefinition RightMissile = RightCannon.addOrReplaceChild(
         "RightMissile",
         CubeListBuilder.create()
            .texOffs(78, 122)
            .addBox(-3.824F, -10.551F, -2.351F, 2.0F, 4.0F, 2.0F, new CubeDeformation(-0.66F))
            .texOffs(120, 106)
            .addBox(-3.8375F, -11.1125F, -2.3375F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.75F))
            .texOffs(37, 118)
            .addBox(-3.3375F, -9.4625F, -1.3775F, 1.0F, 1.0F, 3.0F, new CubeDeformation(-0.25F))
            .texOffs(8, 120)
            .addBox(-3.825F, -9.0002F, -3.0542F, 2.0F, 3.0F, 3.0F, new CubeDeformation(-0.955F))
            .texOffs(2, 117)
            .addBox(-4.15F, -8.9895F, -2.3075F, 3.0F, 3.0F, 2.0F, new CubeDeformation(-0.95F))
            .texOffs(2, 117)
            .addBox(-4.5F, -8.9895F, -2.3075F, 3.0F, 3.0F, 2.0F, new CubeDeformation(-0.951F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition RightPlate2 = RightCannon.addOrReplaceChild(
         "RightPlate2",
         CubeListBuilder.create().texOffs(24, 104).addBox(-4.0F, -9.0F, -3.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(-0.25F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition RightPlate = RightCannon.addOrReplaceChild(
         "RightPlate",
         CubeListBuilder.create().texOffs(24, 114).addBox(-3.0F, -9.0F, -3.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(-0.251F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      return LayerDefinition.create(meshdefinition, 128, 128);
   }
}
