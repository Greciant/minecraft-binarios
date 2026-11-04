package com.devilfruits.client.seastone;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public final class SeastoneArmorGeometry {
   public static LayerDefinition helmet() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      PartDefinition head = root.addOrReplaceChild(
         "head",
         CubeListBuilder.create()
            .texOffs(0, 0)
            .mirror(false)
            .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(1.0F))
            .texOffs(0, 16)
            .mirror(false)
            .addBox(-4.0F, -6.0F, -4.0F, 8.0F, 1.0F, 1.0F, new CubeDeformation(1.3F))
            .texOffs(0, 20)
            .mirror(false)
            .addBox(-3.0F, -8.0F, -3.0F, 6.0F, 1.0F, 6.0F, new CubeDeformation(1.25F))
            .texOffs(24, 16)
            .mirror(false)
            .addBox(-4.0F, -2.0F, 3.0F, 8.0F, 2.0F, 1.0F, new CubeDeformation(1.2F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition hat = root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
      PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
      PartDefinition rightarm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5.0F, 2.0F, 0.0F));
      PartDefinition leftarm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5.0F, 2.0F, 0.0F));
      PartDefinition rightleg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12.0F, 0.0F));
      PartDefinition leftleg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12.0F, 0.0F));
      return LayerDefinition.create(mesh, 64, 32);
   }

   public static LayerDefinition chest() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
      PartDefinition hat = root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
      PartDefinition body = root.addOrReplaceChild(
         "body",
         CubeListBuilder.create()
            .texOffs(16, 16)
            .mirror(false)
            .addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(1.01F))
            .texOffs(0, 0)
            .mirror(false)
            .addBox(-3.0F, 1.0F, -2.0F, 6.0F, 6.0F, 1.0F, new CubeDeformation(1.35F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition rightarm = root.addOrReplaceChild(
         "right_arm",
         CubeListBuilder.create()
            .texOffs(40, 16)
            .mirror(false)
            .addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(1.0F))
            .texOffs(0, 44)
            .mirror(false)
            .addBox(-3.5F, 5.0F, -2.5F, 5.0F, 5.0F, 5.0F, new CubeDeformation(0.2F)),
         PartPose.offset(-5.0F, 2.0F, 0.0F)
      );
      rightarm.addOrReplaceChild(
         "pauldron",
         CubeListBuilder.create()
            .texOffs(0, 32)
            .mirror(false)
            .addBox(-4.5F, -3.5F, -3.0F, 6.0F, 3.0F, 6.0F, new CubeDeformation(0.35F))
            .texOffs(24, 32)
            .mirror(false)
            .addBox(-4.5F, -1.0F, -3.0F, 3.0F, 3.0F, 6.0F, new CubeDeformation(0.3F)),
         PartPose.rotation(0.0F, 0.0F, -0.17F)
      );
      PartDefinition leftarm = root.addOrReplaceChild(
         "left_arm",
         CubeListBuilder.create()
            .texOffs(40, 16)
            .mirror(true)
            .addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(1.0F))
            .texOffs(0, 44)
            .mirror(true)
            .addBox(-1.5F, 5.0F, -2.5F, 5.0F, 5.0F, 5.0F, new CubeDeformation(0.2F)),
         PartPose.offset(5.0F, 2.0F, 0.0F)
      );
      leftarm.addOrReplaceChild(
         "pauldron",
         CubeListBuilder.create()
            .texOffs(0, 32)
            .mirror(true)
            .addBox(-1.5F, -3.5F, -3.0F, 6.0F, 3.0F, 6.0F, new CubeDeformation(0.35F))
            .texOffs(24, 32)
            .mirror(true)
            .addBox(1.5F, -1.0F, -3.0F, 3.0F, 3.0F, 6.0F, new CubeDeformation(0.3F)),
         PartPose.rotation(0.0F, 0.0F, 0.17F)
      );
      PartDefinition rightleg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12.0F, 0.0F));
      PartDefinition leftleg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12.0F, 0.0F));
      return LayerDefinition.create(mesh, 64, 64);
   }

   public static LayerDefinition legs() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
      PartDefinition hat = root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
      PartDefinition body = root.addOrReplaceChild(
         "body",
         CubeListBuilder.create().texOffs(16, 16).mirror(false).addBox(-4.0F, 7.0F, -2.0F, 8.0F, 5.0F, 4.0F, new CubeDeformation(0.65F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      PartDefinition rightarm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5.0F, 2.0F, 0.0F));
      PartDefinition leftarm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5.0F, 2.0F, 0.0F));
      PartDefinition rightleg = root.addOrReplaceChild(
         "right_leg",
         CubeListBuilder.create()
            .texOffs(0, 16)
            .mirror(false)
            .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 7.0F, 4.0F, new CubeDeformation(0.55F))
            .texOffs(0, 0)
            .mirror(false)
            .addBox(-2.0F, 6.0F, -3.0F, 4.0F, 2.0F, 1.0F, new CubeDeformation(0.45F)),
         PartPose.offset(-1.9F, 12.0F, 0.0F)
      );
      PartDefinition leftleg = root.addOrReplaceChild(
         "left_leg",
         CubeListBuilder.create()
            .texOffs(0, 16)
            .mirror(true)
            .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 7.0F, 4.0F, new CubeDeformation(0.55F))
            .texOffs(0, 0)
            .mirror(true)
            .addBox(-2.0F, 6.0F, -3.0F, 4.0F, 2.0F, 1.0F, new CubeDeformation(0.45F)),
         PartPose.offset(1.9F, 12.0F, 0.0F)
      );
      return LayerDefinition.create(mesh, 64, 32);
   }

   public static LayerDefinition boots() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
      PartDefinition hat = root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
      PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
      PartDefinition rightarm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5.0F, 2.0F, 0.0F));
      PartDefinition leftarm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5.0F, 2.0F, 0.0F));
      PartDefinition rightleg = root.addOrReplaceChild(
         "right_leg",
         CubeListBuilder.create()
            .texOffs(0, 16)
            .mirror(false)
            .addBox(-2.0F, 6.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.95F))
            .texOffs(16, 16)
            .mirror(false)
            .addBox(-2.0F, 6.0F, -2.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(1.2F))
            .texOffs(0, 0)
            .mirror(false)
            .addBox(-2.0F, 10.0F, -3.0F, 4.0F, 2.0F, 1.0F, new CubeDeformation(0.75F)),
         PartPose.offset(-1.9F, 12.0F, 0.0F)
      );
      PartDefinition leftleg = root.addOrReplaceChild(
         "left_leg",
         CubeListBuilder.create()
            .texOffs(0, 16)
            .mirror(true)
            .addBox(-2.0F, 6.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.95F))
            .texOffs(16, 16)
            .mirror(true)
            .addBox(-2.0F, 6.0F, -2.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(1.2F))
            .texOffs(0, 0)
            .mirror(true)
            .addBox(-2.0F, 10.0F, -3.0F, 4.0F, 2.0F, 1.0F, new CubeDeformation(0.75F)),
         PartPose.offset(1.9F, 12.0F, 0.0F)
      );
      return LayerDefinition.create(mesh, 64, 32);
   }

   private SeastoneArmorGeometry() {
   }
}
