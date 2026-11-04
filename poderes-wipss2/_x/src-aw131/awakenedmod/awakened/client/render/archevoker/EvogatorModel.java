package com.awakenedmod.awakened.client.render.archevoker;

import com.awakenedmod.awakened.content.entity.archevoker.EvogatorEntity;
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

public class EvogatorModel extends HierarchicalModel<EvogatorEntity> {
   public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("awakened", "evogator"), "main");
   private final ModelPart root;

   public EvogatorModel(ModelPart root) {
      this.root = root;
   }

   public ModelPart root() {
      return this.root;
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      PartDefinition TheCreature = partdefinition.addOrReplaceChild("The creature", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
      PartDefinition FrontLeftLeg = TheCreature.addOrReplaceChild(
         "Front left leg",
         CubeListBuilder.create().texOffs(5, 5).addBox(-4.0F, 0.0F, -2.0F, 4.0F, 9.0F, 5.0F, new CubeDeformation(0.0F)),
         PartPose.offset(-5.0F, -9.0F, -3.0F)
      );
      PartDefinition Head = TheCreature.addOrReplaceChild(
         "Head", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, -11.0F, 7.0F, 1.5708F, 0.0F, -1.5708F)
      );
      PartDefinition upper_jaw = Head.addOrReplaceChild(
         "upper_jaw",
         CubeListBuilder.create().texOffs(40, 0).addBox(-1.5F, 1.0F, -4.0F, 4.0F, 14.0F, 8.0F, new CubeDeformation(0.01F)),
         PartPose.offsetAndRotation(0.0F, -11.0F, 0.0F, 0.0F, 3.1416F, -2.618F)
      );
      PartDefinition lower_jaw = Head.addOrReplaceChild(
         "lower_jaw",
         CubeListBuilder.create().texOffs(40, 0).addBox(-1.5F, 1.0F, -4.0F, 4.0F, 14.0F, 8.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(0.0F, -11.0F, 0.0F, 0.0F, 0.0F, 2.618F)
      );
      PartDefinition FrontRightLeg = TheCreature.addOrReplaceChild(
         "Front right leg",
         CubeListBuilder.create().texOffs(5, 5).addBox(0.0F, 0.0F, -2.0F, 4.0F, 9.0F, 5.0F, new CubeDeformation(0.0F)),
         PartPose.offset(5.0F, -9.0F, -3.0F)
      );
      PartDefinition HindLeftLeg = TheCreature.addOrReplaceChild(
         "Hind left leg",
         CubeListBuilder.create().texOffs(5, 5).addBox(-4.0F, -1.0F, -2.0F, 4.0F, 9.0F, 5.0F, new CubeDeformation(0.0F)),
         PartPose.offset(-5.0F, -8.0F, 13.0F)
      );
      PartDefinition HindRightLeg = TheCreature.addOrReplaceChild(
         "Hind right leg",
         CubeListBuilder.create().texOffs(5, 5).addBox(1.0F, -1.0F, -2.0F, 4.0F, 9.0F, 5.0F, new CubeDeformation(0.0F)),
         PartPose.offset(3.0F, -8.0F, 13.0F)
      );
      PartDefinition Body = partdefinition.addOrReplaceChild("Body", CubeListBuilder.create(), PartPose.offset(0.0F, 12.0F, 5.0F));
      PartDefinition bone = Body.addOrReplaceChild("bone", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
      PartDefinition base_r1 = bone.addOrReplaceChild(
         "base_r1",
         CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -12.0F, -5.0F, 10.0F, 12.0F, 10.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(0.0F, 1.0F, 2.0F, 1.5708F, 0.0F, -1.5708F)
      );
      PartDefinition bone2 = bone.addOrReplaceChild(
         "bone2",
         CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -3.0F, 0.0F, 9.0F, 9.0F, 10.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 0.0F, 2.0F)
      );
      PartDefinition bone3 = bone2.addOrReplaceChild("bone3", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 10.0F));
      PartDefinition base_r2 = bone3.addOrReplaceChild(
         "base_r2",
         CubeListBuilder.create().texOffs(2, 2).addBox(-5.0F, -7.0F, -5.0F, 8.0F, 6.0F, 7.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 5.0F, 5.0F)
      );
      PartDefinition bone4 = bone3.addOrReplaceChild("bone4", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 7.0F));
      PartDefinition base_r3 = bone4.addOrReplaceChild(
         "base_r3",
         CubeListBuilder.create().texOffs(4, 3).addBox(-4.0F, -10.0F, -4.0F, 6.0F, 4.0F, 6.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 9.0F, 4.0F)
      );
      PartDefinition bone5 = bone4.addOrReplaceChild("bone5", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 6.0F));
      PartDefinition base_r4 = bone5.addOrReplaceChild(
         "base_r4",
         CubeListBuilder.create().texOffs(5, 3).addBox(-3.0F, -9.0F, -4.0F, 4.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 9.0F, 4.0F)
      );
      PartDefinition bone6 = bone5.addOrReplaceChild("bone6", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 7.0F));
      PartDefinition base_r5 = bone6.addOrReplaceChild(
         "base_r5",
         CubeListBuilder.create().texOffs(6, 3).addBox(-2.0F, -9.0F, -4.0F, 2.0F, 1.0F, 6.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 9.0F, 3.0F)
      );
      return LayerDefinition.create(meshdefinition, 64, 32);
   }

   public void setupAnim(EvogatorEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
      LegacyEvokerAnim.reset(this);
      if (limbSwingAmount > 0.05F) {
         this.animateWalk(EvogatorAnimations.Running, limbSwing, limbSwingAmount, 2.0F, 1.0F);
      } else {
         LegacyEvokerAnim.apply(this, EvogatorAnimations.Idle, (long)(ageInTicks * 50.0F));
      }
   }
}
