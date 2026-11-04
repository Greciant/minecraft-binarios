package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.entity.ArbiterEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

public class ArbiterModel extends HierarchicalModel<ArbiterEntity> {
   public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("awakened", "arbiter"), "main");
   private static final float FEET_ANCHOR = 24.0F;
   private static final float[][] STRIP_SLOTS = new float[][]{
      {-5.1F, -4.4F, 0.0F},
      {-1.7F, -4.6F, 0.0F},
      {1.7F, -4.6F, 0.0F},
      {5.1F, -4.4F, 0.0F},
      {-5.1F, 4.4F, (float) Math.PI},
      {-1.7F, 4.6F, (float) Math.PI},
      {1.7F, 4.6F, (float) Math.PI},
      {5.1F, 4.4F, (float) Math.PI},
      {-6.0F, -2.0F, (float) (Math.PI / 2)},
      {-6.0F, 2.0F, (float) (Math.PI / 2)},
      {6.0F, -2.0F, (float) (-Math.PI / 2)},
      {6.0F, 2.0F, (float) (-Math.PI / 2)}
   };
   private static final int[] STRIP_LENGTHS = new int[]{24, 20, 26, 22};
   private final ModelPart root;
   private final ModelPart torso;
   private final ModelPart sigil;
   private final ModelPart shoulders;
   private final ModelPart head;
   private final ModelPart hips;
   private final ModelPart[] strips = new ModelPart[STRIP_SLOTS.length];
   private final ModelPart armR;
   private final ModelPart armL;
   private final ModelPart foreR;
   private final ModelPart foreL;
   private final ModelPart staff;
   private final ArbiterModel.Rig scratch = new ArbiterModel.Rig();
   private static final float WRIST_Y = 11.0F;
   private static final float FORE_OFFSET_Y = 12.0F;
   private static final float ARM_OFFSET_X = 10.5F;
   private static final float ARM_OFFSET_Y = -45.0F;
   private static final float SHOULDER_OFFSET_Y = -46.0F;

   public ArbiterModel(ModelPart root) {
      this.root = root;
      this.torso = root.getChild("torso");
      this.sigil = this.torso.getChild("sigil");
      this.shoulders = root.getChild("shoulders");
      this.head = root.getChild("head");
      this.hips = root.getChild("hips");

      for (int i = 0; i < this.strips.length; i++) {
         this.strips[i] = this.hips.getChild("strip_" + i);
      }

      this.armR = root.getChild("arm_r");
      this.armL = root.getChild("arm_l");
      this.foreR = this.armR.getChild("fore_r");
      this.foreL = this.armL.getChild("fore_l");
      this.staff = this.foreR.getChild("staff");
   }

   public ModelPart root() {
      return this.root;
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      PartDefinition torso = root.addOrReplaceChild(
         "torso", CubeListBuilder.create().texOffs(0, 0).addBox(-6.0F, 0.0F, -4.0F, 12.0F, 20.0F, 8.0F), PartPose.offset(0.0F, -46.0F, 0.0F)
      );
      torso.addOrReplaceChild(
         "sigil",
         CubeListBuilder.create().texOffs(98, 0).addBox(-3.0F, -3.0F, -0.5F, 6.0F, 6.0F, 1.0F),
         PartPose.offsetAndRotation(0.0F, 6.5F, -4.2F, 0.0F, 0.0F, (float) (Math.PI / 4))
      );
      PartDefinition shoulders = root.addOrReplaceChild(
         "shoulders", CubeListBuilder.create().texOffs(40, 0).addBox(-9.0F, -3.0F, -5.0F, 18.0F, 6.0F, 10.0F), PartPose.offset(0.0F, -46.0F, 0.0F)
      );
      shoulders.addOrReplaceChild(
         "pauldron_r", CubeListBuilder.create().texOffs(96, 16).addBox(-3.0F, -1.0F, -5.0F, 6.0F, 5.0F, 10.0F), PartPose.offset(-11.0F, -1.0F, 0.0F)
      );
      shoulders.addOrReplaceChild(
         "pauldron_l", CubeListBuilder.create().texOffs(96, 16).mirror().addBox(-3.0F, -1.0F, -5.0F, 6.0F, 5.0F, 10.0F), PartPose.offset(11.0F, -1.0F, 0.0F)
      );
      PartDefinition head = root.addOrReplaceChild(
         "head", CubeListBuilder.create().texOffs(0, 28).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.offset(0.0F, -49.0F, 0.0F)
      );
      head.addOrReplaceChild("crown_band", CubeListBuilder.create().texOffs(32, 28).addBox(-4.5F, -10.0F, -4.5F, 9.0F, 2.0F, 9.0F), PartPose.ZERO);
      head.addOrReplaceChild("spike_c", spike(6), PartPose.offset(0.0F, -10.0F, -4.0F));
      head.addOrReplaceChild("spike_mr", spike(4), PartPose.offset(-2.5F, -10.0F, -3.8F));
      head.addOrReplaceChild("spike_ml", spike(4), PartPose.offset(2.5F, -10.0F, -3.8F));
      head.addOrReplaceChild("spike_or", spike(3), PartPose.offset(-4.0F, -10.0F, -1.5F));
      head.addOrReplaceChild("spike_ol", spike(3), PartPose.offset(4.0F, -10.0F, -1.5F));
      PartDefinition hips = root.addOrReplaceChild(
         "hips", CubeListBuilder.create().texOffs(36, 44).addBox(-5.0F, 0.0F, -3.0F, 10.0F, 22.0F, 6.0F), PartPose.offset(0.0F, -27.0F, 0.0F)
      );

      for (int i = 0; i < STRIP_SLOTS.length; i++) {
         int len = STRIP_LENGTHS[i % STRIP_LENGTHS.length];
         hips.addOrReplaceChild(
            "strip_" + i,
            CubeListBuilder.create().texOffs(stripU(len), 72).addBox(-1.5F, 0.0F, -0.5F, 3.0F, (float)len, 1.0F),
            PartPose.offsetAndRotation(STRIP_SLOTS[i][0], 1.0F, STRIP_SLOTS[i][1], 0.0F, STRIP_SLOTS[i][2], 0.0F)
         );
      }

      PartDefinition armR = root.addOrReplaceChild(
         "arm_r", CubeListBuilder.create().texOffs(0, 44).addBox(-2.5F, -1.0F, -2.5F, 5.0F, 14.0F, 5.0F), PartPose.offset(-10.5F, -45.0F, 0.0F)
      );
      PartDefinition foreR = armR.addOrReplaceChild(
         "fore_r", CubeListBuilder.create().texOffs(20, 44).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(0.0F, 12.0F, 0.0F)
      );
      PartDefinition armL = root.addOrReplaceChild(
         "arm_l", CubeListBuilder.create().texOffs(0, 44).mirror().addBox(-2.5F, -1.0F, -2.5F, 5.0F, 14.0F, 5.0F), PartPose.offset(10.5F, -45.0F, 0.0F)
      );
      armL.addOrReplaceChild(
         "fore_l", CubeListBuilder.create().texOffs(20, 44).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(0.0F, 12.0F, 0.0F)
      );
      PartDefinition staff = foreR.addOrReplaceChild(
         "staff", CubeListBuilder.create().texOffs(74, 28).addBox(-1.0F, -20.0F, -1.0F, 2.0F, 34.0F, 2.0F), PartPose.offset(0.0F, 10.0F, 0.0F)
      );
      staff.addOrReplaceChild("staff_cage", CubeListBuilder.create().texOffs(84, 28).addBox(-2.5F, -26.0F, -2.5F, 5.0F, 6.0F, 5.0F), PartPose.ZERO);
      staff.addOrReplaceChild("staff_orb", CubeListBuilder.create().texOffs(84, 40).addBox(-1.5F, -24.5F, -1.5F, 3.0F, 3.0F, 3.0F), PartPose.ZERO);
      staff.addOrReplaceChild("staff_ferrule", CubeListBuilder.create().texOffs(104, 28).addBox(-1.5F, 13.0F, -1.5F, 3.0F, 2.0F, 3.0F), PartPose.ZERO);
      return LayerDefinition.create(mesh, 128, 128);
   }

   private static CubeListBuilder spike(int height) {
      return CubeListBuilder.create().texOffs(68, 28).addBox(-0.5F, (float)(-height), -0.5F, 1.0F, (float)height, 1.0F);
   }

   private static int stripU(int len) {
      if (len <= 20) {
         return 0;
      } else {
         return len <= 24 ? 8 : 16;
      }
   }

   public void setupAnim(ArbiterEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
      this.pose(
         limbSwing,
         Math.min(limbSwingAmount, 1.0F),
         ageInTicks,
         netHeadYaw,
         headPitch,
         entity.getAttackAnim(Mth.frac(ageInTicks)),
         entity.getPhase(),
         entity.isKneeling(),
         entity.inRecess()
      );
   }

   private void pose(float swing, float amount, float age, float yRot, float xRot, float attackProgress, byte phase, boolean kneeling, boolean inRecess) {
      float DEG = (float) (Math.PI / 180.0);
      ArbiterModel.Rig r = this.scratch;
      rig(r, age, attackProgress, phase, kneeling, inRecess);
      this.root.y = r.rootY;
      this.root.xRot = r.rootXRot;
      this.root.zRot = r.rootZRot;
      this.armR.xRot = r.armRX;
      this.armR.zRot = r.armRZ;
      this.foreR.xRot = r.foreRX;
      this.armL.xRot = r.armLX;
      this.armL.zRot = r.armLZ;
      this.foreL.xRot = r.foreLX;
      this.staff.xRot = 0.12F;
      this.head.zRot = 0.0F;
      this.head.yRot = yRot * (float) (Math.PI / 180.0);
      this.head.xRot = xRot * (float) (Math.PI / 180.0);

      for (int i = 0; i < this.strips.length; i++) {
         float phaseOff = (float)i * 0.83F;
         float idleSway = Mth.sin(age * 0.08F + phaseOff) * 0.05F;
         float trail = Mth.cos(swing * 0.4F + phaseOff) * 0.22F * amount;
         this.strips[i].xRot = -0.06F + idleSway + trail;
         this.strips[i].zRot = Mth.sin(age * 0.06F + phaseOff * 1.3F) * 0.03F;
      }

      if (kneeling) {
         this.head.xRot += 0.55F;
         this.staff.xRot = 0.95F;

         for (int i = 0; i < this.strips.length; i++) {
            this.strips[i].xRot = -0.22F + Mth.sin(age * 0.05F + (float)i * 0.83F) * 0.03F;
         }
      }
   }

   public static void rig(ArbiterModel.Rig r, float age, float attackProgress, byte phase, boolean kneeling, boolean inRecess) {
      float rate = inRecess ? 0.5F : 1.0F;
      float bob = Mth.sin(age * 0.06F * rate) * (inRecess ? 1.7F : 1.4F);
      r.rootY = 22.5F + bob;
      r.rootZRot = Mth.cos(age * 0.035F * rate) * 0.025F;
      r.rootXRot = 0.0F;
      r.armRX = 0.0F;
      r.armLX = 0.0F;
      r.armRZ = -0.08F;
      r.armLZ = 0.08F;
      r.foreRX = -0.12F;
      r.foreLX = 0.0F;
      float drift = Mth.cos(age * 0.055F * rate) * 0.06F * (inRecess ? 0.55F : 1.0F);
      r.armRX += drift;
      r.armLX += -drift * 0.8F;
      r.armLZ = r.armLZ + Mth.sin(age * 0.05F * rate) * 0.02F;
      if (kneeling) {
         float settle = Mth.sin(age * 0.05F) * 0.5F;
         r.rootY = 30.5F + settle;
         r.rootXRot = 0.3F;
         r.armRX = 0.35F;
         r.armLX = 0.4F;
         r.foreRX = 0.2F;
         r.foreLX = 0.15F;
      } else {
         if (attackProgress > 0.0F) {
            float slam = -2.0F * Mth.sin(attackProgress * (float) Math.PI);
            r.armLX += slam;
            r.foreLX = r.foreLX + -0.4F * Mth.sin(attackProgress * (float) Math.PI);
            r.armRX = r.armRX + 0.3F * Mth.sin(attackProgress * (float) Math.PI);
         }

         if (phase == 6) {
            r.armRX += -2.9F;
            r.foreRX += -0.35F;
            r.armLX += -0.5F;
            r.rootXRot = -0.06F;
         }

         if (phase == 7) {
            r.armRX += -1.2F;
            r.armLX += -1.2F;
            r.armRZ -= 0.9F;
            r.armLZ += 0.9F;
         }

         if (phase == 2) {
            r.rootY++;
            r.armRZ -= 0.7F;
            r.armLZ += 0.7F;
         }

         if (phase == 3) {
            r.rootXRot = -0.28F;
            r.armRX += 0.7F;
            r.foreRX += -0.9F;
            r.armLX += 0.6F;
         }

         if (phase == 4) {
            r.armRX += -2.6F;
            r.foreRX += -0.3F;
            r.armLX += -2.3F;
            r.foreLX += -0.4F;
         }

         if (phase == 5 || phase == 8) {
            float tremor = Mth.sin(age * 0.6F) * 0.05F;
            r.armRX += -1.55F + tremor;
            r.foreRX += 0.1F;
            r.armLX += 0.75F;
            r.rootXRot = 0.12F;
         }
      }
   }

   public static Vector3f hand(ArbiterModel.Rig r, boolean right) {
      Vector3f p = new Vector3f(0.0F, 11.0F, 0.0F);
      p.rotateX(right ? r.foreRX : r.foreLX);
      p.add(0.0F, 12.0F, 0.0F);
      p.rotateX(right ? r.armRX : r.armLX);
      p.rotateZ(right ? r.armRZ : r.armLZ);
      p.add(right ? -10.5F : 10.5F, -45.0F, 0.0F);
      return root(r, p);
   }

   public static Vector3f shoulder(ArbiterModel.Rig r) {
      return root(r, new Vector3f(0.0F, -46.0F, 0.0F));
   }

   private static Vector3f root(ArbiterModel.Rig r, Vector3f p) {
      p.rotateX(r.rootXRot);
      p.rotateZ(r.rootZRot);
      p.add(0.0F, r.rootY, 0.0F);
      return p;
   }

   public static final class Rig {
      public float rootY;
      public float rootXRot;
      public float rootZRot;
      public float armRX;
      public float armRZ;
      public float foreRX;
      public float armLX;
      public float armLZ;
      public float foreLX;
   }
}
