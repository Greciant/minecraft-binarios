package com.awakenedmod.awakened.client.render.duelist;

import com.awakenedmod.awakened.client.render.AnimEnvelopes;
import com.awakenedmod.awakened.content.entity.duelist.VarkosEntity;
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

public class VarkosModel extends HierarchicalModel<VarkosEntity> {
   public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("awakened", "varkos"), "main");
   private static final float FEET_ANCHOR = 24.0F;
   private static final float DEG = (float) (Math.PI / 180.0);
   private static final float PI = (float) Math.PI;
   private static final int HALO_SEGMENTS = 8;
   private static final float HALO_Y = -15.0F;
   private static final float HALO_Z = 1.5F;
   private static final float HALO_RADIUS = 5.5F;
   private static final float HALO_TILT = (float) (Math.PI * 2.0 / 5.0);
   private static final float HALO_SPIN = 0.02F;
   private static final float SHEATH_XROT = 1.35F;
   private static final float IAI_DASH_TICKS = 10.0F;
   private static final float FLURRY_TICKS = 30.0F;
   private static final float CLOAK_HEM_TILT = 0.13962634F;
   private static final float HAMMER_REACH_END = 0.45F;
   private final ModelPart root;
   private final ModelPart pelvis;
   private final ModelPart abdomen;
   private final ModelPart chest;
   private final ModelPart head;
   private final ModelPart flywheelHub;
   private final ModelPart[] haloSegments = new ModelPart[8];
   private final ModelPart armBladePauldron;
   private final ModelPart armBladeUpper;
   private final ModelPart armBladeFore;
   private final ModelPart armBladeHand;
   private final ModelPart armOffUpper;
   private final ModelPart armOffFore;
   private final ModelPart armOffPalm;
   private final ModelPart cloakUpper;
   private final ModelPart cloakLower;
   private final ModelPart rightThigh;
   private final ModelPart leftThigh;
   private final ModelPart rightShin;
   private final ModelPart leftShin;
   private final ModelPart daggerStowedL;
   private final ModelPart daggerStowedR;
   private final ModelPart daggerMain;
   private final ModelPart daggerOff;
   private final ModelPart sheath;
   private final ModelPart katanaHilt;
   private final ModelPart katanaDrawn;
   private final ModelPart hammerStowed;
   private final ModelPart hammerDrawn;
   private float cloakLagYaw = 0.0F;

   public VarkosModel(ModelPart root) {
      this.root = root;
      this.pelvis = root.getChild("pelvis");
      this.abdomen = this.pelvis.getChild("abdomen");
      this.chest = this.abdomen.getChild("chest");
      this.head = this.chest.getChild("head");
      this.flywheelHub = this.chest.getChild("flywheel_hub");

      for (int i = 0; i < 8; i++) {
         this.haloSegments[i] = this.flywheelHub.getChild("flywheel_seg_" + i);
      }

      this.armBladePauldron = this.chest.getChild("arm_blade_pauldron");
      this.armBladeUpper = this.armBladePauldron.getChild("arm_blade_upper");
      this.armBladeFore = this.armBladeUpper.getChild("arm_blade_fore");
      this.armBladeHand = this.armBladeFore.getChild("arm_blade_hand");
      this.armOffUpper = this.chest.getChild("arm_off_upper");
      this.armOffFore = this.armOffUpper.getChild("arm_off_fore");
      this.armOffPalm = this.armOffFore.getChild("arm_off_palm");
      this.cloakUpper = this.chest.getChild("cloak_upper");
      this.cloakLower = this.pelvis.getChild("cloak_lower");
      this.rightThigh = root.getChild("right_thigh");
      this.leftThigh = root.getChild("left_thigh");
      this.rightShin = this.rightThigh.getChild("right_shin");
      this.leftShin = this.leftThigh.getChild("left_shin");
      this.daggerStowedL = this.pelvis.getChild("dagger_stowed_l");
      this.daggerStowedR = this.pelvis.getChild("dagger_stowed_r");
      this.sheath = this.pelvis.getChild("sheath");
      this.katanaHilt = this.sheath.getChild("katana_hilt");
      this.hammerStowed = this.chest.getChild("hammer_stowed");
      this.daggerMain = this.armBladeHand.getChild("dagger_main");
      this.katanaDrawn = this.armBladeHand.getChild("katana_drawn");
      this.hammerDrawn = this.armBladeHand.getChild("hammer_drawn");
      this.daggerOff = this.armOffFore.getChild("dagger_off");
   }

   public ModelPart root() {
      return this.root;
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      PartDefinition pelvis = root.addOrReplaceChild(
         "pelvis", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -5.0F, -2.5F, 8.0F, 5.0F, 5.0F), PartPose.offset(0.0F, -19.0F, 0.0F)
      );
      PartDefinition abdomen = pelvis.addOrReplaceChild(
         "abdomen", CubeListBuilder.create().texOffs(28, 0).addBox(-3.5F, -5.0F, -2.0F, 7.0F, 5.0F, 4.0F), PartPose.offset(0.0F, -5.0F, 0.0F)
      );
      PartDefinition chest = abdomen.addOrReplaceChild(
         "chest", CubeListBuilder.create().texOffs(0, 12).addBox(-4.5F, -7.0F, -2.5F, 9.0F, 7.0F, 5.0F), PartPose.offset(0.0F, -5.0F, 0.0F)
      );
      PartDefinition head = chest.addOrReplaceChild(
         "head", CubeListBuilder.create().texOffs(0, 30).addBox(-3.5F, -7.0F, -3.5F, 7.0F, 7.0F, 7.0F), PartPose.offset(0.0F, -7.0F, 0.0F)
      );
      head.addOrReplaceChild("visor", CubeListBuilder.create().texOffs(56, 30).addBox(-3.0F, -5.5F, -3.6F, 6.0F, 1.0F, 1.0F), PartPose.ZERO);
      PartDefinition flywheelHub = chest.addOrReplaceChild(
         "flywheel_hub",
         CubeListBuilder.create().texOffs(60, 0).addBox(-1.5F, -1.5F, -1.0F, 3.0F, 3.0F, 2.0F),
         PartPose.offsetAndRotation(0.0F, -15.0F, 1.5F, (float) (Math.PI * 2.0 / 5.0), 0.0F, 0.0F)
      );
      float radius = 5.5F;

      for (int i = 0; i < 8; i++) {
         float angle = (float)((double)i * 2.0 * Math.PI / 8.0);
         float cx = Mth.sin(angle) * radius;
         float cy = -Mth.cos(angle) * radius;
         flywheelHub.addOrReplaceChild(
            "flywheel_seg_" + i,
            CubeListBuilder.create().texOffs(70 + i * 6, 0).addBox(-1.0F, -1.0F, -0.5F, 2.0F, 2.0F, 1.0F),
            PartPose.offsetAndRotation(cx, cy, 0.0F, 0.0F, 0.0F, angle)
         );
      }

      PartDefinition armBladePauldron = chest.addOrReplaceChild(
         "arm_blade_pauldron", CubeListBuilder.create().texOffs(0, 56).addBox(-3.0F, -2.5F, -3.0F, 6.0F, 5.0F, 6.0F), PartPose.offset(-7.5F, -5.5F, 0.0F)
      );
      PartDefinition armBladeUpper = armBladePauldron.addOrReplaceChild(
         "arm_blade_upper", CubeListBuilder.create().texOffs(0, 68).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 9.0F, 4.0F), PartPose.offset(0.0F, 2.5F, 0.0F)
      );
      PartDefinition armBladeFore = armBladeUpper.addOrReplaceChild(
         "arm_blade_fore", CubeListBuilder.create().texOffs(0, 82).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 8.0F, 4.0F), PartPose.offset(0.0F, 9.0F, 0.0F)
      );
      PartDefinition armBladeHand = armBladeFore.addOrReplaceChild(
         "arm_blade_hand", CubeListBuilder.create().texOffs(18, 56).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 3.0F, 3.0F), PartPose.offset(0.0F, 8.0F, 0.0F)
      );
      PartDefinition armOffUpper = chest.addOrReplaceChild(
         "arm_off_upper", CubeListBuilder.create().texOffs(25, 56).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 9.0F, 3.0F), PartPose.offset(7.5F, -5.0F, 0.0F)
      );
      PartDefinition armOffFore = armOffUpper.addOrReplaceChild(
         "arm_off_fore", CubeListBuilder.create().texOffs(25, 68).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 8.0F, 3.0F), PartPose.offset(0.0F, 9.0F, 0.0F)
      );
      armOffFore.addOrReplaceChild(
         "arm_off_palm", CubeListBuilder.create().texOffs(25, 82).addBox(-2.0F, 0.0F, -0.5F, 4.0F, 4.0F, 1.0F), PartPose.offset(0.0F, 8.0F, 0.0F)
      );
      chest.addOrReplaceChild(
         "cloak_upper", CubeListBuilder.create().texOffs(72, 40).addBox(-4.0F, 0.0F, -0.5F, 8.0F, 11.0F, 1.0F), PartPose.offset(0.0F, -7.0F, 2.9F)
      );
      pelvis.addOrReplaceChild(
         "cloak_lower",
         CubeListBuilder.create().texOffs(92, 40).addBox(-4.0F, 0.0F, -0.5F, 8.0F, 13.0F, 1.0F),
         PartPose.offsetAndRotation(0.0F, -6.0F, 2.9F, 0.13962634F, 0.0F, 0.0F)
      );
      PartDefinition rightThigh = root.addOrReplaceChild(
         "right_thigh", CubeListBuilder.create().texOffs(50, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 8.0F, 4.0F), PartPose.offset(-2.5F, -19.0F, 0.0F)
      );
      rightThigh.addOrReplaceChild(
         "right_shin", CubeListBuilder.create().texOffs(50, 14).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 8.0F, 4.0F), PartPose.offset(0.0F, 8.0F, 0.0F)
      );
      rightThigh.getChild("right_shin")
         .addOrReplaceChild(
            "right_boot", CubeListBuilder.create().texOffs(50, 28).addBox(-2.5F, 0.0F, -3.0F, 5.0F, 3.0F, 6.0F), PartPose.offset(0.0F, 8.0F, 0.0F)
         );
      PartDefinition leftThigh = root.addOrReplaceChild(
         "left_thigh", CubeListBuilder.create().texOffs(50, 0).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 8.0F, 4.0F), PartPose.offset(2.5F, -19.0F, 0.0F)
      );
      leftThigh.addOrReplaceChild(
         "left_shin", CubeListBuilder.create().texOffs(50, 14).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 8.0F, 4.0F), PartPose.offset(0.0F, 8.0F, 0.0F)
      );
      leftThigh.getChild("left_shin")
         .addOrReplaceChild(
            "left_boot", CubeListBuilder.create().texOffs(50, 28).mirror().addBox(-2.5F, 0.0F, -3.0F, 5.0F, 3.0F, 6.0F), PartPose.offset(0.0F, 8.0F, 0.0F)
         );
      addDagger(pelvis, "dagger_stowed_l", 2.9F, -4.0F, -2.4F, 0.38F);
      addDagger(pelvis, "dagger_stowed_r", -2.9F, -4.0F, -2.4F, -0.38F);
      PartDefinition sheath = pelvis.addOrReplaceChild(
         "sheath",
         CubeListBuilder.create().texOffs(22, 102).addBox(-1.0F, -1.0F, -1.5F, 2.0F, 12.0F, 3.0F, new CubeDeformation(0.25F)),
         PartPose.offsetAndRotation(5.0F, -0.5F, -0.5F, 1.35F, 0.25F, 0.1F)
      );
      sheath.addOrReplaceChild(
         "katana_hilt",
         CubeListBuilder.create()
            .texOffs(6, 102)
            .addBox(-0.5F, -6.0F, -0.5F, 1.0F, 5.0F, 1.0F, new CubeDeformation(0.3F))
            .texOffs(6, 96)
            .addBox(-2.0F, -1.2F, -1.5F, 4.0F, 1.0F, 3.0F, new CubeDeformation(0.15F)),
         PartPose.ZERO
      );
      chest.addOrReplaceChild(
         "hammer_stowed",
         CubeListBuilder.create()
            .texOffs(34, 102)
            .addBox(-1.0F, -11.0F, -1.0F, 2.0F, 24.0F, 2.0F)
            .texOffs(44, 102)
            .addBox(-2.5F, -14.5F, -3.5F, 5.0F, 4.0F, 7.0F),
         PartPose.offsetAndRotation(0.5F, -0.5F, 4.5F, 0.0F, 0.0F, -0.66F)
      );
      addDagger(armBladeHand, "dagger_main", 0.0F, 1.5F, 0.0F, 0.0F);
      addDagger(armOffFore, "dagger_off", 0.0F, 8.5F, 0.0F, 0.0F);
      armBladeHand.addOrReplaceChild(
         "katana_drawn",
         CubeListBuilder.create().texOffs(0, 96).addBox(-1.0F, 2.0F, -0.5F, 2.0F, 22.0F, 1.0F).texOffs(6, 96).addBox(-2.0F, 1.0F, -1.5F, 4.0F, 1.0F, 3.0F),
         PartPose.ZERO
      );
      armBladeHand.addOrReplaceChild(
         "hammer_drawn",
         CubeListBuilder.create()
            .texOffs(34, 102)
            .addBox(-1.0F, -7.0F, -1.0F, 2.0F, 24.0F, 2.0F)
            .texOffs(44, 102)
            .addBox(-2.5F, 13.5F, -3.5F, 5.0F, 4.0F, 7.0F),
         PartPose.offset(0.0F, 1.5F, 0.0F)
      );
      return LayerDefinition.create(mesh, 128, 128);
   }

   private static void addDagger(PartDefinition parent, String name, float x, float y, float z, float zRot) {
      parent.addOrReplaceChild(
         name,
         CubeListBuilder.create().texOffs(12, 102).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 8.0F, 1.0F).texOffs(12, 114).addBox(-1.5F, -1.0F, -0.5F, 3.0F, 1.0F, 1.0F),
         PartPose.offsetAndRotation(x, y, z, 0.0F, 0.0F, zRot)
      );
   }

   public void setupAnim(VarkosEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitchDeg) {
      float gait = Math.min(limbSwingAmount, 1.0F);
      byte action = entity.getAction();
      float p = entity.getActionProgress();
      int windup = entity.getMoveWindup() & 255;
      int recover = entity.getMoveRecover() & 255;
      byte weapon = entity.getWeapon();
      float death = entity.deathTime > 0 ? (float)entity.deathTime + Mth.frac(ageInTicks) : 0.0F;
      this.root.y = 24.0F;
      this.root.xRot = 0.0F;
      this.root.zRot = 0.0F;
      this.pelvis.xRot = 0.0F;
      this.pelvis.zRot = 0.0F;
      this.pelvis.yRot = 0.0F;
      this.abdomen.xRot = 0.0F;
      this.abdomen.zRot = 0.0F;
      this.chest.xRot = 0.0F;
      this.chest.yRot = 0.0F;
      this.chest.zRot = 0.0F;
      this.head.xRot = 0.0F;
      this.head.yRot = 0.0F;
      this.armBladePauldron.xRot = this.armBladePauldron.yRot = this.armBladePauldron.zRot = 0.0F;
      this.armBladeUpper.xRot = this.armBladeUpper.zRot = 0.0F;
      this.armBladeFore.xRot = 0.0F;
      this.armOffUpper.xRot = this.armOffUpper.yRot = this.armOffUpper.zRot = 0.0F;
      this.armOffFore.xRot = 0.0F;
      this.rightThigh.xRot = this.leftThigh.xRot = 0.0F;
      this.rightThigh.zRot = this.leftThigh.zRot = 0.0F;
      this.rightShin.xRot = this.leftShin.xRot = 0.0F;
      this.cloakUpper.xRot = 0.0F;
      this.cloakLower.xRot = 0.13962634F;
      this.sheath.xRot = 1.35F;
      this.flywheelHub.xRot = (float) (Math.PI * 2.0 / 5.0);
      this.flywheelHub.yRot = 0.0F;
      boolean acting = action != 0 && windup > 0;
      int total = windup + recover;
      float tick = acting ? p * (float)total : 0.0F;
      float still = 0.0F;
      if (acting && action == 4 && tick < (float)windup) {
         still = AnimEnvelopes.easeOutCubic(seg(tick, 0.0F, Math.min(6.0F, (float)windup * 0.5F)));
      }

      this.flywheelHub.zRot = ageInTicks * 0.02F;
      this.flywheelHub.y = -15.0F + Mth.sin(ageInTicks * 0.06F) * 0.5F;

      for (int i = 0; i < 8; i++) {
         this.haloSegments[i].visible = true;
      }

      this.cloakUpper.visible = true;
      this.cloakLower.visible = true;
      if (still < 0.5F) {
         this.cloakLagYaw = this.cloakLagYaw + 0.15F * (0.0F - this.cloakLagYaw);
      }

      float cloakSway = Mth.sin(ageInTicks * 0.05F) * 0.04F * (1.0F - still);
      this.cloakUpper.yRot = this.cloakLagYaw + cloakSway;
      this.cloakLower.yRot = this.cloakLagYaw + cloakSway * 1.3F;
      float breathe = Mth.sin(ageInTicks * 0.08F) * 0.25F * (1.0F - still);
      this.root.y = 24.0F + breathe * 0.3F;
      this.root.xRot = 0.04F;
      this.armBladePauldron.zRot = 0.12F;
      this.armBladeUpper.xRot = 0.15F;
      this.armOffUpper.zRot = -0.1F;
      this.head.yRot = netHeadYaw * (float) (Math.PI / 180.0) * 0.5F;
      this.head.xRot = headPitchDeg * (float) (Math.PI / 180.0) * 0.4F;
      gait *= 1.0F - still;
      this.rightThigh.xRot = this.rightThigh.xRot + Mth.cos(limbSwing * 0.6F) * 0.65F * gait;
      this.leftThigh.xRot = this.leftThigh.xRot + Mth.cos(limbSwing * 0.6F + (float) Math.PI) * 0.65F * gait;
      this.rightShin.xRot = this.rightShin.xRot + Math.max(0.0F, Mth.cos(limbSwing * 0.6F + 0.8F)) * 0.45F * gait;
      this.leftShin.xRot = this.leftShin.xRot + Math.max(0.0F, Mth.cos(limbSwing * 0.6F + (float) Math.PI + 0.8F)) * 0.45F * gait;
      this.armBladePauldron.xRot = this.armBladePauldron.xRot + Mth.cos(limbSwing * 0.6F + (float) Math.PI) * 0.3F * gait;
      this.armOffUpper.xRot = this.armOffUpper.xRot + Mth.cos(limbSwing * 0.6F) * 0.3F * gait;
      this.cloakUpper.xRot += 0.18F * gait;
      this.cloakLower.xRot += 0.3F * gait;
      if (acting) {
         this.applyAction(action, p, tick, windup, recover, ageInTicks);
      }

      this.applyGear(weapon, action, tick, windup, recover);
      if (death > 0.0F) {
         this.applyDeath(death, ageInTicks);
      }
   }

   private void applyGear(byte weapon, byte action, float tick, int windup, int recover) {
      boolean daggers = weapon == 1 && (action != 1 || tick >= (float)windup * 0.45F);
      boolean katana = weapon == 2 && (action != 4 || tick >= (float)windup);
      boolean hammer = weapon == 3 && (action != 9 || windup <= 0 || tick >= hammerGrabTick(windup, recover));
      this.daggerMain.visible = daggers;
      this.daggerOff.visible = daggers;
      this.daggerStowedL.visible = !daggers;
      this.daggerStowedR.visible = !daggers;
      this.katanaDrawn.visible = katana;
      this.katanaHilt.visible = !katana;
      this.hammerDrawn.visible = hammer;
      this.hammerStowed.visible = !hammer;
   }

   private static float hammerGrabTick(int windup, int recover) {
      AnimEnvelopes.Phases ph = AnimEnvelopes.Phases.of(windup, recover);
      float raiseTicks = ph.raiseEnd() * (float)ph.totalTicks();
      return raiseTicks * (1.0F - (float)Math.cbrt(0.55F));
   }

   private void applyAction(byte action, float p, float tick, int windup, int recover, float age) {
      AnimEnvelopes.Phases ph = AnimEnvelopes.Phases.of(windup, recover);
      switch (action) {
         case 1:
            this.poseDaggerFlurry(ph, p, tick, windup, recover, age);
            break;
         case 4:
            this.poseIaiDash(tick, windup, windup + recover, age);
            break;
         case 9:
            this.poseHammerSlam(ph, p, tick, age);
            break;
         case 16:
            this.poseBow(p, age);
            break;
         case 18:
            this.poseStagger(age);
      }
   }

   private void poseDaggerFlurry(AnimEnvelopes.Phases ph, float p, float tick, int windup, int recover, float age) {
      float a = AnimEnvelopes.antic(ph, p);
      float tr = AnimEnvelopes.tremor(ph, p, age, 0.045F);
      int total = ph.totalTicks();
      float flurryEnd = (float)windup + Math.min(30.0F, (float)recover);
      float intoBreather = AnimEnvelopes.smooth(seg(tick, flurryEnd, flurryEnd + 5.0F));
      float flurry = seg(tick, (float)windup, (float)windup + 2.0F) * (1.0F - intoBreather);
      float breather = intoBreather * (1.0F - AnimEnvelopes.smooth(seg(tick, (float)total - 6.0F, (float)total - 1.0F)));
      this.armBladePauldron.xRot += -0.3F * a;
      this.armBladePauldron.yRot += -0.55F * a;
      this.armBladePauldron.zRot += 0.4F * a;
      this.armBladeFore.xRot += -1.25F * a;
      this.armOffUpper.xRot += -0.3F * a;
      this.armOffUpper.yRot += 0.55F * a;
      this.armOffUpper.zRot += -0.4F * a;
      this.armOffFore.xRot += -1.25F * a;
      this.pelvis.yRot += -0.22F * a;
      this.chest.yRot += -0.1F * a;
      this.chest.xRot += 0.16F * a;
      this.chest.zRot += tr;
      this.root.y += 1.8F * a;
      this.rightShin.xRot += 0.3F * a;
      this.leftShin.xRot += 0.3F * a;
      this.rightThigh.xRot += -0.15F * a;
      this.leftThigh.xRot += -0.15F * a;
      float c = age * 2.6F;
      float slashR = Mth.sin(c);
      float slashL = Mth.sin(c + (float) Math.PI);
      this.armBladePauldron.xRot += (-1.05F + 0.95F * slashR) * flurry;
      this.armBladePauldron.yRot = this.armBladePauldron.yRot + (-0.25F + 0.45F * Mth.cos(c)) * flurry;
      this.armBladeFore.xRot += (-0.55F + 0.35F * slashR) * flurry;
      this.armOffUpper.xRot += (-1.05F + 0.95F * slashL) * flurry;
      this.armOffUpper.yRot = this.armOffUpper.yRot + (0.25F - 0.45F * Mth.cos(c)) * flurry;
      this.armOffFore.xRot += (-0.55F + 0.35F * slashL) * flurry;
      this.chest.yRot = this.chest.yRot + Mth.sin(c) * 0.22F * flurry;
      this.pelvis.yRot = this.pelvis.yRot + -Mth.sin(c) * 0.07F * flurry;
      this.chest.xRot += 0.12F * flurry;
      this.rightThigh.xRot += 0.22F * flurry;
      this.leftThigh.xRot += -0.22F * flurry;
      this.rightShin.xRot += 0.18F * flurry;
      this.leftShin.xRot += 0.3F * flurry;
      this.root.y += 1.2F * flurry;
      float heave = Mth.sin(age * 0.5F) * 0.07F;
      this.armBladePauldron.xRot += 0.3F * breather;
      this.armBladePauldron.zRot += 0.55F * breather;
      this.armOffUpper.xRot += 0.3F * breather;
      this.armOffUpper.zRot += -0.55F * breather;
      this.armBladeFore.xRot += -0.2F * breather;
      this.armOffFore.xRot += -0.2F * breather;
      this.chest.xRot += (0.22F + heave) * breather;
      this.abdomen.xRot += 0.1F * breather;
      this.head.xRot += 0.32F * breather;
      this.root.y += 2.0F * breather;
      this.root.xRot += 0.06F * breather;
      this.rightShin.xRot += 0.35F * breather;
      this.leftShin.xRot += 0.35F * breather;
      this.rightThigh.xRot += -0.18F * breather;
      this.leftThigh.xRot += -0.18F * breather;
   }

   private void poseIaiDash(float tick, int windup, int total, float age) {
      if (tick < (float)windup) {
         float in = AnimEnvelopes.easeOutCubic(seg(tick, 0.0F, Math.min(6.0F, (float)windup * 0.5F)));
         this.root.y += 7.0F * in;
         this.leftThigh.xRot += -1.35F * in;
         this.leftShin.xRot += 1.35F * in;
         this.rightThigh.xRot += 0.3F * in;
         this.rightShin.xRot += 1.35F * in;
         this.chest.xRot += 0.4F * in;
         this.abdomen.xRot += 0.1F * in;
         this.pelvis.yRot += 0.2F * in;
         this.chest.yRot += 0.15F * in;
         this.head.xRot += -0.3F * in;
         this.armBladePauldron.xRot += -2.5F * in;
         this.armBladePauldron.yRot += -0.33F * in;
         this.armBladePauldron.zRot += 2.14F * in;
         this.armBladeFore.xRot += -0.96F * in;
         this.armOffUpper.xRot += 0.55F * in;
         this.sheath.xRot += 0.2F * in;
         this.cloakUpper.xRot += -0.2F * in;
         this.chest.zRot = this.chest.zRot + Mth.sin(age * 2.2F) * 0.006F * in;
      } else {
         float dashEnd = (float)windup + 10.0F;
         float uncoil = seg(tick, (float)windup, (float)windup + 2.0F);
         float arc = AnimEnvelopes.easeOutCubic(seg(tick, (float)windup, (float)windup + 4.0F));
         float lunge = AnimEnvelopes.smooth(seg(tick, (float)windup + 3.0F, dashEnd + 2.0F));
         float resheath = AnimEnvelopes.smooth(seg(tick, dashEnd + 6.0F, (float)total - 2.0F));
         float settle = AnimEnvelopes.smooth(seg(tick, (float)total - 6.0F, (float)total));
         float k = 1.0F - 0.55F * settle;
         this.root.y += 7.0F * (1.0F - uncoil) + 4.5F * lunge * (1.0F - 0.45F * resheath) * k;
         this.root.xRot += 0.42F * arc * (1.0F - 0.55F * resheath) * k;
         this.chest.xRot += 0.18F * lunge * (1.0F - 0.5F * resheath) * k;
         float trail = uncoil * (1.0F - lunge);
         this.rightThigh.xRot += 0.6F * trail;
         this.leftThigh.xRot += 0.3F * trail;
         float legW = lunge * (1.0F - 0.4F * resheath) * k;
         this.leftThigh.xRot += -1.25F * legW;
         this.leftShin.xRot += 1.6F * legW;
         this.rightThigh.xRot += 0.85F * legW;
         this.rightShin.xRot += 0.2F * legW;
         float armX = Mth.lerp(arc, -2.5F, -1.5F);
         float armYaw = Mth.lerp(arc, -0.33F, 0.95F);
         float armZ = Mth.lerp(arc, 2.14F, 0.05F);
         float foreX = Mth.lerp(arc, -0.96F, -0.1F);
         armX = Mth.lerp(resheath, armX, -2.8F);
         armYaw = Mth.lerp(resheath, armYaw, -0.78F);
         armZ = Mth.lerp(resheath, armZ, 2.38F);
         foreX = Mth.lerp(resheath, foreX, -1.02F);
         this.armBladePauldron.xRot += armX * k;
         this.armBladePauldron.yRot += armYaw * k;
         this.armBladePauldron.zRot += armZ * k;
         this.armBladeFore.xRot += foreX * k;
         float torsoYaw = Mth.lerp(arc, 0.2F, -0.35F);
         torsoYaw = Mth.lerp(resheath, torsoYaw, 0.15F);
         this.pelvis.yRot += torsoYaw * k;
         this.chest.yRot += torsoYaw * 0.5F * k;
         this.armOffUpper.xRot += 0.8F * arc * (1.0F - resheath) + 0.55F * resheath * k;
         this.armOffUpper.zRot += -0.5F * arc * (1.0F - resheath) + 0.09F * resheath * k;
         this.armOffUpper.yRot += 1.07F * resheath * k;
         this.armOffFore.xRot += -1.55F * resheath * k;
         float cloakSnap = uncoil * (1.0F - AnimEnvelopes.smooth(seg(tick, dashEnd, dashEnd + 8.0F)));
         this.cloakUpper.xRot = this.cloakUpper.xRot + (0.55F + Mth.sin(age * 1.8F) * 0.1F) * cloakSnap;
         this.cloakLower.xRot = this.cloakLower.xRot + (0.95F + Mth.sin(age * 1.8F + 0.9F) * 0.15F) * cloakSnap;
         this.cloakUpper.yRot = this.cloakUpper.yRot + Mth.sin(age * 2.3F) * 0.12F * cloakSnap;
         this.cloakLower.yRot = this.cloakLower.yRot + Mth.sin(age * 2.3F + 0.7F) * 0.18F * cloakSnap;
         this.head.xRot += -0.1F * arc * (1.0F - resheath) + 0.3F * resheath * (1.0F - settle);
      }
   }

   private void poseHammerSlam(AnimEnvelopes.Phases ph, float p, float tick, float age) {
      float a = AnimEnvelopes.antic(ph, p);
      float s = AnimEnvelopes.strike(ph, p);
      float j = AnimEnvelopes.jolt(ph, p, 2);
      float tr = AnimEnvelopes.tremor(ph, p, age, 0.022F);
      int total = ph.totalTicks();
      float reach = Mth.clamp(a / 0.45F, 0.0F, 1.0F);
      float hoist = Mth.clamp((a - 0.45F) / 0.55F, 0.0F, 1.0F);
      float offJoin = Mth.clamp((a - 0.3F) / 0.7F, 0.0F, 1.0F);
      float carry = AnimEnvelopes.smooth(seg(tick, ph.followEnd() * (float)total, (float)total - 1.0F));
      float bladeX = Mth.lerp(hoist, -2.3F * reach, -3.05F);
      float foreX = Mth.lerp(hoist, -0.95F * reach, -0.05F);
      this.armBladePauldron.xRot += bladeX - 0.5F * s + tr;
      this.armBladePauldron.yRot += 0.3F * reach * (1.0F - hoist) - 0.18F * hoist;
      this.armBladeFore.xRot += foreX - 0.25F * s;
      this.armOffUpper.xRot += -3.0F * offJoin - 0.5F * s + tr;
      this.armOffUpper.yRot += 0.18F * hoist;
      this.armOffFore.xRot += -0.1F * offJoin;
      this.chest.xRot += -0.34F * hoist + 0.52F * s + tr * 0.6F;
      this.abdomen.xRot += -0.15F * hoist + 0.3F * s;
      this.root.xRot += -0.04F * hoist + 0.16F * s + 0.12F * j;
      this.head.xRot += -0.22F * hoist + 0.25F * s;
      this.root.y += -1.5F * hoist + 2.2F * s + 3.2F * j;
      this.rightShin.xRot += 0.45F * s;
      this.leftShin.xRot += 0.45F * s;
      this.rightThigh.xRot += -0.25F * s;
      this.leftThigh.xRot += -0.25F * s;
      this.armBladePauldron.xRot += -0.95F * carry;
      this.armBladePauldron.zRot += 0.15F * carry;
      this.armBladeFore.xRot += -1.3F * carry;
      this.armOffUpper.xRot += -0.55F * carry;
      this.armOffUpper.yRot += 0.35F * carry;
      this.armOffFore.xRot += -0.4F * carry;
      this.chest.xRot += -0.06F * carry;
      this.root.y += 0.8F * carry;
   }

   private void poseBow(float p, float age) {
      float depth = smooth(seg(p, 0.0F, 0.35F)) * (1.0F - smooth(seg(p, 0.68F, 0.98F)));
      if (!(depth <= 0.0F)) {
         float breath = Mth.sin(age * 0.25F) * 0.02F * depth;
         this.chest.xRot += 0.42F * depth + breath;
         this.abdomen.xRot += 0.22F * depth;
         this.pelvis.yRot += 0.2F * depth;
         this.head.xRot += 0.55F * depth;
         this.root.xRot += 0.04F * depth;
         this.root.y += 0.8F * depth;
         this.rightThigh.xRot += 0.18F * depth;
         this.rightShin.xRot += 0.22F * depth;
         this.armBladePauldron.xRot += -2.5F * depth;
         this.armBladePauldron.yRot += -0.33F * depth;
         this.armBladePauldron.zRot += 2.14F * depth;
         this.armBladeFore.xRot += -0.96F * depth;
         this.armOffUpper.xRot += 0.65F * depth;
         this.armOffUpper.zRot += -0.3F * depth;
         this.armOffFore.xRot += -0.2F * depth;
         this.cloakUpper.xRot += -0.18F * depth;
      }
   }

   private void poseStagger(float age) {
      this.root.xRot = 0.5F;
      this.chest.xRot = 0.3F;
      this.abdomen.xRot = 0.2F;
      this.root.y += 5.0F;
      this.rightThigh.xRot++;
      this.rightShin.xRot++;
      this.leftThigh.xRot += 0.2F;
      this.armBladePauldron.xRot = 0.5F;
      this.armOffUpper.xRot = 0.5F;
      this.chest.zRot = Mth.sin(age * 1.3F) * 0.04F;
   }

   private void applyDeath(float d, float age) {
      this.root.xRot = 0.0F;
      this.chest.zRot = 0.0F;
      this.root.y = 24.0F;
      float stump = Mth.clamp(d / 20.0F, 0.0F, 1.0F);
      this.root.xRot = -0.25F * smooth(stump);
      this.root.y = this.root.y + 3.0F * smooth(stump);
      this.rightThigh.xRot = 0.9F * smooth(stump);
      this.rightShin.xRot = 0.8F * smooth(stump);
      if (d >= 20.0F) {
         float rise = Mth.clamp((d - 20.0F) / 20.0F, 0.0F, 1.0F);
         this.root.y = 24.0F + 3.0F * (1.0F - smooth(rise));
         this.root.xRot = -0.25F * (1.0F - smooth(rise));
         this.rightThigh.xRot = 0.9F * (1.0F - smooth(rise));
         this.rightShin.xRot = 0.8F * (1.0F - smooth(rise));
         this.armBladePauldron.xRot = 0.5F * smooth(rise);
      }

      if (d >= 40.0F) {
         float hang = Mth.clamp((d - 40.0F) / 50.0F, 0.0F, 1.0F);
         this.armBladePauldron.xRot = 0.5F + 0.5F * smooth(hang);
         this.armOffUpper.xRot = 0.3F * smooth(hang);
         this.chest.xRot = -0.05F * smooth(hang);
      }

      if (d >= 90.0F) {
         float bow = Mth.clamp((d - 90.0F) / 20.0F, 0.0F, 1.0F);
         this.head.xRot = 0.4F * smooth(bow);
         this.chest.xRot = -0.1F * smooth(bow);
      }

      if (d >= 110.0F) {
         float slip = Mth.clamp((d - 110.0F) / 20.0F, 0.0F, 1.0F);
         this.armOffUpper.xRot = 0.3F + 0.8F * smooth(slip);
         this.armOffUpper.zRot = -0.3F * smooth(slip);
      }
   }

   private static float seg(float tick, float from, float to) {
      if (to <= from) {
         return tick >= to ? 1.0F : 0.0F;
      } else {
         return Mth.clamp((tick - from) / (to - from), 0.0F, 1.0F);
      }
   }

   private static float smooth(float t) {
      t = Mth.clamp(t, 0.0F, 1.0F);
      return t * t * (3.0F - 2.0F * t);
   }
}
