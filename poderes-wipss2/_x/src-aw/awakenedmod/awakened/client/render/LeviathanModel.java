package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.entity.LeviathanEntity;
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

public class LeviathanModel extends HierarchicalModel<LeviathanEntity> {
   public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("awakened", "leviathan"), "main");
   private static final float FEET_ANCHOR = 24.0F;
   private final ModelPart root;
   private final ModelPart pelvis;
   private final ModelPart hullLower;
   private final ModelPart hullUpper;
   private final ModelPart coreSeam;
   private final ModelPart sensorHead;
   private final ModelPart cannonR;
   private final ModelPart cannonL;
   private final ModelPart leftThigh;
   private final ModelPart rightThigh;
   private final ModelPart leftShin;
   private final ModelPart rightShin;

   public LeviathanModel(ModelPart root) {
      this.root = root;
      this.pelvis = root.getChild("pelvis");
      this.hullLower = root.getChild("hull_lower");
      this.hullUpper = root.getChild("hull_upper");
      this.coreSeam = this.hullLower.getChild("core_seam");
      this.sensorHead = this.hullUpper.getChild("sensor_head");
      this.cannonR = root.getChild("cannon_r");
      this.cannonL = root.getChild("cannon_l");
      this.rightThigh = root.getChild("right_thigh");
      this.leftThigh = root.getChild("left_thigh");
      this.rightShin = this.rightThigh.getChild("right_shin");
      this.leftShin = this.leftThigh.getChild("left_shin");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      PartDefinition pelvis = partdefinition.addOrReplaceChild(
         "pelvis",
         CubeListBuilder.create().texOffs(0, 0).addBox(-15.0F, -10.0F, -8.0F, 30.0F, 10.0F, 16.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, -24.0F, 0.0F)
      );
      PartDefinition hull_lower = partdefinition.addOrReplaceChild(
         "hull_lower",
         CubeListBuilder.create().texOffs(0, 30).addBox(-19.0F, -18.0F, -11.0F, 38.0F, 18.0F, 22.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, -34.0F, 0.0F)
      );
      PartDefinition core_seam = hull_lower.addOrReplaceChild(
         "core_seam",
         CubeListBuilder.create().texOffs(120, 0).addBox(-4.0F, -16.0F, -12.0F, 8.0F, 14.0F, 2.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition hull_upper = partdefinition.addOrReplaceChild(
         "hull_upper",
         CubeListBuilder.create().texOffs(0, 72).addBox(-22.0F, -16.0F, -12.0F, 44.0F, 16.0F, 24.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, -52.0F, 0.0F)
      );
      PartDefinition sensor_head = hull_upper.addOrReplaceChild(
         "sensor_head",
         CubeListBuilder.create().texOffs(0, 114).addBox(-7.0F, -10.0F, -8.0F, 14.0F, 10.0F, 8.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, -16.0F, -3.0F)
      );
      PartDefinition visor = sensor_head.addOrReplaceChild(
         "visor",
         CubeListBuilder.create().texOffs(60, 114).addBox(-6.0F, -7.0F, -9.0F, 12.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition cannon_r = partdefinition.addOrReplaceChild(
         "cannon_r",
         CubeListBuilder.create()
            .texOffs(120, 30)
            .addBox(-5.0F, -2.0F, -5.0F, 10.0F, 10.0F, 10.0F, new CubeDeformation(0.0F))
            .texOffs(120, 52)
            .addBox(-4.0F, -26.0F, -4.0F, 8.0F, 24.0F, 8.0F, new CubeDeformation(0.0F))
            .texOffs(160, 30)
            .addBox(-5.0F, -30.0F, -5.0F, 10.0F, 5.0F, 10.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(-12.0F, -60.0F, 8.0F, -0.5F, 0.0F, 0.0F)
      );
      PartDefinition cannon_l = partdefinition.addOrReplaceChild(
         "cannon_l",
         CubeListBuilder.create()
            .texOffs(120, 30)
            .mirror()
            .addBox(-5.0F, -2.0F, -5.0F, 10.0F, 10.0F, 10.0F, new CubeDeformation(0.0F))
            .mirror(false)
            .texOffs(120, 52)
            .mirror()
            .addBox(-4.0F, -26.0F, -4.0F, 8.0F, 24.0F, 8.0F, new CubeDeformation(0.0F))
            .mirror(false)
            .texOffs(160, 30)
            .mirror()
            .addBox(-5.0F, -30.0F, -5.0F, 10.0F, 5.0F, 10.0F, new CubeDeformation(0.0F))
            .mirror(false),
         PartPose.offsetAndRotation(12.0F, -60.0F, 8.0F, -0.5F, 0.0F, 0.0F)
      );
      PartDefinition right_thigh = partdefinition.addOrReplaceChild(
         "right_thigh",
         CubeListBuilder.create().texOffs(180, 60).addBox(-6.0F, 0.0F, -6.0F, 12.0F, 12.0F, 12.0F, new CubeDeformation(0.0F)),
         PartPose.offset(-10.0F, -24.0F, 0.0F)
      );
      PartDefinition right_shin = right_thigh.addOrReplaceChild(
         "right_shin",
         CubeListBuilder.create().texOffs(180, 90).addBox(-7.0F, 0.0F, -7.0F, 14.0F, 12.0F, 14.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 12.0F, 0.0F)
      );
      PartDefinition left_thigh = partdefinition.addOrReplaceChild(
         "left_thigh",
         CubeListBuilder.create().texOffs(180, 60).mirror().addBox(-6.0F, 0.0F, -6.0F, 12.0F, 12.0F, 12.0F, new CubeDeformation(0.0F)).mirror(false),
         PartPose.offset(10.0F, -24.0F, 0.0F)
      );
      PartDefinition left_shin = left_thigh.addOrReplaceChild(
         "left_shin",
         CubeListBuilder.create().texOffs(180, 90).mirror().addBox(-7.0F, 0.0F, -7.0F, 14.0F, 12.0F, 14.0F, new CubeDeformation(0.0F)).mirror(false),
         PartPose.offset(0.0F, 12.0F, 0.0F)
      );
      return LayerDefinition.create(meshdefinition, 256, 256);
   }

   private static CubeListBuilder cannonBarrel(boolean mirror) {
      CubeListBuilder b = CubeListBuilder.create();
      if (mirror) {
         b.mirror();
      }

      return b.texOffs(120, 30)
         .addBox(-5.0F, -2.0F, -5.0F, 10.0F, 10.0F, 10.0F)
         .texOffs(120, 52)
         .addBox(-4.0F, -26.0F, -4.0F, 8.0F, 24.0F, 8.0F)
         .texOffs(160, 30)
         .addBox(-5.0F, -30.0F, -5.0F, 10.0F, 5.0F, 10.0F);
   }

   public static float smoothedPhaseProgress(float progress, byte phase, byte escalation, byte cannonR, byte cannonL, float partialTick) {
      int alive = (cannonR < 3 ? 1 : 0) + (cannonL < 3 ? 1 : 0);

      int total = switch (phase) {
         case 1 -> LeviathanEntity.salvoWindupTicks(escalation) + 12;
         case 2 -> LeviathanEntity.beamWindupTicks(escalation) + LeviathanEntity.beamSweepTicks(alive);
         case 3 -> 30;
         default -> 0;
      };
      return total > 0 ? Math.min(1.0F, progress + partialTick / (float)total) : progress;
   }

   public ModelPart root() {
      return this.root;
   }

   public void setupAnim(LeviathanEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
      float amount = Math.min(limbSwingAmount, 1.0F);
      float attackProgress = entity.getAttackAnim(Mth.frac(ageInTicks));
      byte phase = entity.getPhase();
      byte escalation = entity.getEscalation();
      byte cannonRState = entity.getCannonState(0);
      byte cannonLState = entity.getCannonState(1);
      float p = smoothedPhaseProgress(entity.getPhaseProgress(), phase, escalation, cannonRState, cannonLState, Mth.frac(ageInTicks));
      float death = entity.deathTime > 0 ? (float)entity.deathTime + Mth.frac(ageInTicks) : 0.0F;
      boolean deadR = cannonRState >= 3;
      boolean deadL = cannonLState >= 3;
      float rootX = 0.0F;
      float rootZ = Mth.cos(ageInTicks * 0.04F) * 0.03F;
      float rootYOff = 0.0F;
      float dip = 0.0F;
      float hullUpperZ = 0.0F;
      float sensorX = 0.0F;
      float thighSplay = 0.0F;
      this.hullUpper.y = -52.0F + Mth.cos(ageInTicks * 0.06F) * 0.6F;
      this.leftThigh.xRot = Mth.cos(limbSwing * 0.5F) * 0.5F * amount;
      this.rightThigh.xRot = Mth.cos(limbSwing * 0.5F + (float) Math.PI) * 0.5F * amount;
      this.leftShin.xRot = Math.max(0.0F, Mth.cos(limbSwing * 0.5F + 1.0F)) * 0.3F * amount;
      this.rightShin.xRot = Math.max(0.0F, Mth.cos(limbSwing * 0.5F + (float) Math.PI + 1.0F)) * 0.3F * amount;
      this.sensorHead.yRot = netHeadYaw * (float) (Math.PI / 180.0) * 0.4F;
      float cannonRest = -0.5F;
      float sway = Mth.sin(ageInTicks * 0.05F) * 0.03F;
      float cRX = cannonRest;
      float cLX = cannonRest;
      float cRZ = sway;
      float cLZ = -sway;
      float cRY = 0.0F;
      float cLY = 0.0F;
      if (phase == 1) {
         AnimEnvelopes.Phases ph = AnimEnvelopes.Phases.of(LeviathanEntity.salvoWindupTicks(escalation), 12);
         float a = AnimEnvelopes.antic(ph, p);
         float s = AnimEnvelopes.strike(ph, p);
         float j = AnimEnvelopes.jolt(ph, p, 2);
         float tr = AnimEnvelopes.tremor(ph, p, ageInTicks, 0.025F);
         float x = cannonRest - 1.1F * a + 0.2F * s - 0.45F * j;
         if (!deadR) {
            cRX = x;
            cRZ = sway + tr;
         }

         if (!deadL) {
            cLX = x;
            cLZ -= tr;
         }

         rootX += 0.04F * a + 0.05F * j;
         this.hullUpper.y += 1.2F * j;
      } else if (phase == 2) {
         int alive = (deadR ? 0 : 1) + (deadL ? 0 : 1);
         AnimEnvelopes.Phases phx = AnimEnvelopes.Phases.of(LeviathanEntity.beamWindupTicks(escalation), LeviathanEntity.beamSweepTicks(alive));
         float ax = AnimEnvelopes.antic(phx, p);
         float sx = AnimEnvelopes.strike(phx, p);
         float trx = AnimEnvelopes.tremor(phx, p, ageInTicks, 0.02F);
         boolean sweeping = p >= phx.hitFrac();
         float aim = sweeping ? 1.0F : Math.min(1.0F, ax + sx);
         float xx = cannonRest + 0.7F * aim + 0.1F * sx;
         float yTraverse = 0.0F;
         float shudder = 0.0F;
         if (sweeping) {
            float ps = (p - phx.hitFrac()) / Math.max(1.0E-4F, 1.0F - phx.hitFrac());
            yTraverse = (ps - 0.5F) * (float)Math.toRadians(LeviathanEntity.beamArcDegrees(alive));
            shudder = Mth.sin(ageInTicks * 1.4F) * 0.03F;
         }

         if (!deadR) {
            cRX = xx;
            cRZ = sway + (sweeping ? shudder : trx);
            cRY = yTraverse;
         }

         if (!deadL) {
            cLX = xx;
            cLZ += sweeping ? shudder : -trx;
            cLY = yTraverse;
         }

         sensorX += 0.12F * ax;
         rootX -= 0.03F * sx;
      } else if (phase == 3) {
         AnimEnvelopes.Phases phxx = AnimEnvelopes.Phases.of(20, 10);
         float axx = AnimEnvelopes.antic(phxx, p);
         float sxx = AnimEnvelopes.strike(phxx, p);
         float jx = AnimEnvelopes.jolt(phxx, p, 2);
         dip = 8.0F * axx - 1.5F * sxx;
         hullUpperZ += AnimEnvelopes.tremor(phxx, p, ageInTicks, 0.02F);
         rootX += 0.05F * axx - 0.08F * sxx;
         rootYOff += 2.0F * jx;
         if (!deadR) {
            cRX = cannonRest - 0.2F * jx;
         }

         if (!deadL) {
            cLX = cannonRest - 0.2F * jx;
         }
      }

      if (cannonRState == 2) {
         cRX += 0.3F + Mth.sin(ageInTicks * 1.7F) * 0.09F;
         cRZ += 0.12F;
      }

      if (cannonLState == 2) {
         cLX += 0.3F + Mth.sin(ageInTicks * 1.7F) * 0.09F;
         cLZ -= 0.12F;
      }

      if (deadR) {
         cRX = 1.2F;
         cRZ = -0.5F;
         cRY = 0.0F;
      }

      if (deadL) {
         cLX = 1.2F;
         cLZ = 0.5F;
         cLY = 0.0F;
      }

      if (attackProgress > 0.0F) {
         rootX -= 0.18F * Mth.sin(attackProgress * (float) Math.PI);
      }

      if (escalation >= 3) {
         rootX -= 0.12F;
         if (!deadR) {
            cRZ += Mth.sin(ageInTicks * 0.4F) * 0.06F;
         }

         if (!deadL) {
            cLZ -= Mth.sin(ageInTicks * 0.4F) * 0.06F;
         }
      }

      if (death > 0.0F || phase == 4) {
         float d = Math.max(0.0F, death);
         this.hullUpper.y = -52.0F;
         rootX = 0.0F;
         hullUpperZ = 0.0F;
         dip = 0.0F;
         float in = Math.min(1.0F, d / 20.0F);
         sensorX = 0.5F * in;
         float droop = Mth.lerp(in, cannonRest, 0.2F);
         float kickR = d >= 20.0F ? Math.max(0.0F, 1.0F - (d - 20.0F) / 15.0F) : 0.0F;
         float kickL = d >= 45.0F ? Math.max(0.0F, 1.0F - (d - 45.0F) / 15.0F) : 0.0F;
         cRX = Mth.lerp(kickR, deadR ? 0.9F : droop, -1.2F);
         cLX = Mth.lerp(kickL, deadL ? 0.9F : droop, -1.2F);
         cRZ = deadR ? -0.35F : 0.0F;
         cLZ = deadL ? 0.35F : 0.0F;
         cRY = 0.0F;
         cLY = 0.0F;
         float list = Mth.clamp((d - 70.0F) / 20.0F, 0.0F, 1.0F);
         rootZ = 0.18F * list;
         rootYOff += 6.0F * list;
         thighSplay = 0.15F * list;
      }

      this.root.y = 24.0F + rootYOff;
      this.root.xRot = rootX;
      this.root.zRot = rootZ;
      this.pelvis.y = -24.0F + dip;
      this.hullLower.y = -34.0F + dip;
      this.hullUpper.y += dip;
      this.hullUpper.zRot = hullUpperZ;
      this.sensorHead.xRot = sensorX;
      this.leftThigh.zRot = thighSplay;
      this.rightThigh.zRot = -thighSplay;
      this.cannonR.xRot = cRX;
      this.cannonR.yRot = cRY;
      this.cannonR.zRot = cRZ;
      this.cannonL.xRot = cLX;
      this.cannonL.yRot = cLY;
      this.cannonL.zRot = cLZ;
   }
}
