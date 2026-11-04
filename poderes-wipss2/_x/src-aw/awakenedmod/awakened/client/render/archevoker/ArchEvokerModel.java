package com.awakenedmod.awakened.client.render.archevoker;

import com.awakenedmod.awakened.content.entity.archevoker.ArchEvokerAction;
import com.awakenedmod.awakened.content.entity.archevoker.ArchEvokerActor;
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
import net.minecraft.world.entity.Mob;

public class ArchEvokerModel<E extends Mob> extends HierarchicalModel<E> {
   public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("awakened", "archevoker"), "main");
   private static final float DEG = (float) (Math.PI / 180.0);
   private static final float FANGS_LEN = 1.0F;
   private final ModelPart root;
   private final ModelPart body;
   private final ModelPart head;
   private final ModelPart leftArm;
   private final ModelPart staff;

   public ArchEvokerModel(ModelPart root) {
      this.root = root;
      this.body = root.getChild("body");
      this.head = this.body.getChild("Head");
      this.leftArm = this.body.getChild("LeftArm");
      this.staff = this.leftArm.getChild("Staff");
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
            .texOffs(28, 37)
            .addBox(-4.0F, 0.0F, -3.0F, 8.0F, 12.0F, 6.0F, new CubeDeformation(0.0F))
            .texOffs(0, 19)
            .addBox(-4.0F, 0.0F, -3.0F, 8.0F, 18.0F, 6.0F, new CubeDeformation(0.5F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition Head = body.addOrReplaceChild(
         "Head",
         CubeListBuilder.create()
            .texOffs(28, 19)
            .addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F, new CubeDeformation(0.0F))
            .texOffs(0, 43)
            .addBox(4.0F, -10.0F, -5.0F, 1.0F, 10.0F, 9.0F, new CubeDeformation(0.0F))
            .texOffs(0, 43)
            .addBox(-5.0F, -10.0F, -5.0F, 1.0F, 10.0F, 9.0F, new CubeDeformation(0.0F))
            .texOffs(52, 55)
            .addBox(-4.0F, -10.0F, 4.0F, 8.0F, 10.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(38, 0)
            .addBox(-4.0F, -11.0F, -5.0F, 8.0F, 1.0F, 9.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition nose = Head.addOrReplaceChild(
         "nose",
         CubeListBuilder.create().texOffs(38, 10).addBox(-1.0F, -1.0F, -6.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, -2.0F, 0.0F)
      );
      PartDefinition LeftArm = body.addOrReplaceChild(
         "LeftArm",
         CubeListBuilder.create().texOffs(36, 55).mirror().addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false),
         PartPose.offset(5.0F, 2.0F, 0.0F)
      );
      PartDefinition Staff = LeftArm.addOrReplaceChild(
         "Staff",
         CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(0.0F, -4.0F, -10.0F, 1.0F, 1.0F, 18.0F, new CubeDeformation(0.0F))
            .texOffs(14, 14)
            .addBox(0.0F, -4.0F, -19.0F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
            .texOffs(17, 17)
            .addBox(0.0F, -8.0F, -16.0F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(13, 13)
            .addBox(0.0F, -8.0F, -15.0F, 1.0F, 1.0F, 5.0F, new CubeDeformation(0.0F))
            .texOffs(17, 17)
            .addBox(0.0F, -8.0F, -10.0F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(0, 0)
            .addBox(-1.0F, -6.0F, -14.0F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)),
         PartPose.offset(1.0F, 12.0F, 0.0F)
      );
      PartDefinition arms = body.addOrReplaceChild("arms", CubeListBuilder.create(), PartPose.offset(0.0F, 2.0F, 0.0F));
      PartDefinition leg0 = body.addOrReplaceChild(
         "leg0",
         CubeListBuilder.create().texOffs(20, 55).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)),
         PartPose.offset(-2.0F, 12.0F, 0.0F)
      );
      PartDefinition leg1 = body.addOrReplaceChild(
         "leg1",
         CubeListBuilder.create().texOffs(20, 55).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false),
         PartPose.offset(2.0F, 12.0F, 0.0F)
      );
      PartDefinition RightArm = body.addOrReplaceChild(
         "RightArm",
         CubeListBuilder.create().texOffs(36, 55).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)),
         PartPose.offset(-5.0F, 2.0F, 0.0F)
      );
      return LayerDefinition.create(meshdefinition, 128, 128);
   }

   public void setupAnim(E entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
      LegacyEvokerAnim.reset(this);
      this.head.yRot = netHeadYaw * (float) (Math.PI / 180.0);
      this.head.xRot = headPitch * (float) (Math.PI / 180.0);
      byte a = ((ArchEvokerActor)entity).getAction();
      long ageMs = (long)(ageInTicks * 50.0F);
      if (a == 0) {
         if (limbSwingAmount > 0.05F) {
            LegacyEvokerAnim.apply(this, ArchEvokerAnimations.Float, ageMs);
         } else {
            LegacyEvokerAnim.apply(this, ArchEvokerAnimations.Idle, ageMs);
         }
      } else if (ArchEvokerAction.isSummonCast(a)) {
         LegacyEvokerAnim.apply(this, ArchEvokerAnimations.Summon, ageMs);
      } else {
         long t = (long)(Math.min(1.0F, ((ArchEvokerActor)entity).getActionProgress()) * 1.0F * 1000.0F);
         LegacyEvokerAnim.apply(this, ArchEvokerAnimations.Fangs, t);
      }
   }
}
