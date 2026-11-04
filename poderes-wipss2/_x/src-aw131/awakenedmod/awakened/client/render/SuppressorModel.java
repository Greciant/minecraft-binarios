package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.entity.SuppressorEntity;
import com.awakenedmod.awakened.core.AwId;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

public class SuppressorModel extends HierarchicalModel<SuppressorEntity> {
   public static final ModelLayerLocation LAYER = new ModelLayerLocation(AwId.of("suppressor"), "main");
   private static final float DEG = (float) (Math.PI / 180.0);
   private static final float ROOT_Y = 14.0F;
   private final ModelPart root;
   private final ModelPart core;
   private final ModelPart eyeLens;
   private final ModelPart ring;
   private final ModelPart emitter;
   private final ModelPart prong0;
   private final ModelPart prong1;
   private final ModelPart prong2;

   public SuppressorModel(ModelPart root) {
      this.root = root;
      this.core = root.getChild("core");
      this.eyeLens = this.core.getChild("eye_lens");
      this.ring = root.getChild("ring");
      this.emitter = root.getChild("emitter");
      this.prong0 = this.emitter.getChild("prong0");
      this.prong1 = this.emitter.getChild("prong1");
      this.prong2 = this.emitter.getChild("prong2");
   }

   public ModelPart root() {
      return this.root;
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      PartDefinition core = root.addOrReplaceChild(
         "core",
         CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 4.0F, 8.0F).texOffs(0, 16).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 4.0F, 8.0F),
         PartPose.offset(0.0F, -2.0F, 0.0F)
      );
      core.addOrReplaceChild(
         "eye_lens", CubeListBuilder.create().texOffs(0, 32).addBox(-3.0F, -3.0F, -4.5F, 6.0F, 6.0F, 1.0F), PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition ring = root.addOrReplaceChild(
         "ring",
         CubeListBuilder.create()
            .texOffs(28, 0)
            .addBox(-5.0F, -0.5F, 5.0F, 10.0F, 1.0F, 2.0F)
            .texOffs(28, 4)
            .addBox(5.0F, -0.5F, -5.0F, 2.0F, 1.0F, 10.0F)
            .texOffs(28, 8)
            .addBox(-7.0F, -0.5F, -5.0F, 2.0F, 1.0F, 10.0F),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition emitter = root.addOrReplaceChild("emitter", CubeListBuilder.create(), PartPose.offset(0.0F, 6.0F, 0.0F));
      addProng(emitter, "prong0", 0.0F);
      addProng(emitter, "prong1", 120.0F);
      addProng(emitter, "prong2", 240.0F);
      return LayerDefinition.create(mesh, 64, 64);
   }

   private static void addProng(PartDefinition emitter, String name, float yawDeg) {
      float yaw = yawDeg * (float) (Math.PI / 180.0);
      emitter.addOrReplaceChild(
         name,
         CubeListBuilder.create().texOffs(44, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F),
         PartPose.offsetAndRotation(0.0F, 0.0F, -2.0F, 0.43633232F, yaw, 0.0F)
      );
   }

   public void setupAnim(SuppressorEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
      float partialTick = Mth.frac(ageInTicks);
      float sidewaysSpeed = entity.getSidewaysSpeed(partialTick);
      float attackProgress = entity.getAttackProgress(partialTick);
      this.root.y = 14.0F + Mth.cos(ageInTicks * 0.18F) * 1.2F;
      this.root.zRot = Mth.clamp(sidewaysSpeed, -0.3F, 0.3F);
      this.core.yRot = netHeadYaw * (float) (Math.PI / 180.0);
      this.core.xRot = headPitch * (float) (Math.PI / 180.0);
      this.ring.yRot = ageInTicks * 0.12F + attackProgress * 6.0F;
      float pull = attackProgress * 1.5F;
      this.prong0.z = -pull;
      this.prong1.z = -pull;
      this.prong2.z = -pull;
      this.emitter.y = 6.0F - attackProgress * 1.0F;
   }
}
