package com.awakenedmod.awakened.client.render.archevoker;

import com.awakenedmod.awakened.content.entity.archevoker.VexplodeEntity;
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

public class VexplodeModel extends HierarchicalModel<VexplodeEntity> {
   public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("awakened", "vexplode"), "main");
   private final ModelPart root;

   public VexplodeModel(ModelPart root) {
      this.root = root;
   }

   public ModelPart root() {
      return this.root;
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      PartDefinition body = partdefinition.addOrReplaceChild(
         "body",
         CubeListBuilder.create()
            .texOffs(0, 10)
            .addBox(-1.5F, -4.0F, -1.0F, 3.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(0, 16)
            .addBox(-1.5F, -3.0F, -1.0F, 3.0F, 5.0F, 2.0F, new CubeDeformation(-0.2F)),
         PartPose.offset(0.0F, 24.0F, 0.0F)
      );
      PartDefinition head = body.addOrReplaceChild(
         "head",
         CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -5.0F, -2.5F, 5.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, -4.0F, 0.0F)
      );
      PartDefinition rightArm = body.addOrReplaceChild(
         "rightArm",
         CubeListBuilder.create().texOffs(23, 0).addBox(-1.25F, -0.5F, -1.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(-0.1F)),
         PartPose.offset(-1.75F, -3.75F, 0.0F)
      );
      PartDefinition rightItem = rightArm.addOrReplaceChild("rightItem", CubeListBuilder.create(), PartPose.offset(-0.25F, 2.75F, 0.0F));
      PartDefinition leftArm = body.addOrReplaceChild(
         "leftArm",
         CubeListBuilder.create().texOffs(23, 6).addBox(-0.75F, -0.5F, -1.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(-0.1F)),
         PartPose.offset(1.75F, -3.75F, 0.0F)
      );
      PartDefinition leftWing = body.addOrReplaceChild(
         "leftWing",
         CubeListBuilder.create().texOffs(16, 22).mirror().addBox(0.0F, 0.0F, 0.0F, 8.0F, 5.0F, 0.0F, new CubeDeformation(0.0F)).mirror(false),
         PartPose.offset(0.5F, -3.0F, 1.0F)
      );
      PartDefinition rightWing = body.addOrReplaceChild(
         "rightWing",
         CubeListBuilder.create().texOffs(16, 22).addBox(-8.0F, 0.0F, 0.0F, 8.0F, 5.0F, 0.0F, new CubeDeformation(0.0F)),
         PartPose.offset(-0.5F, -3.0F, 1.0F)
      );
      return LayerDefinition.create(meshdefinition, 32, 32);
   }

   public void setupAnim(VexplodeEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
      LegacyEvokerAnim.reset(this);
      LegacyEvokerAnim.apply(this, VexplodeAnimations.Fly, (long)(ageInTicks * 50.0F));
   }
}
