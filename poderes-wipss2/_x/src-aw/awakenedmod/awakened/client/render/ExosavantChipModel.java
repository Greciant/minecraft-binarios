package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public final class ExosavantChipModel {
   public static final ModelLayerLocation LAYER = new ModelLayerLocation(AwId.of("exosavant_chip"), "main");
   public static final String CHIP = "Chip";

   private ExosavantChipModel() {
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      PartDefinition Chip = partdefinition.addOrReplaceChild(
         "Chip",
         CubeListBuilder.create()
            .texOffs(0, 6)
            .addBox(-3.0F, 1.0F, -2.25F, 6.0F, 5.0F, 3.0F, new CubeDeformation(-0.75F))
            .texOffs(14, 14)
            .addBox(-2.0F, 4.0F, -2.25F, 4.0F, 3.0F, 3.0F, new CubeDeformation(-0.76F))
            .texOffs(0, 14)
            .addBox(-2.0F, 1.0F, -2.5F, 4.0F, 5.0F, 3.0F, new CubeDeformation(-0.76F))
            .texOffs(18, 6)
            .addBox(-2.0F, 0.0F, -2.25F, 4.0F, 3.0F, 3.0F, new CubeDeformation(-0.76F))
            .texOffs(0, 0)
            .addBox(-4.0F, 2.0F, -2.25F, 8.0F, 3.0F, 3.0F, new CubeDeformation(-0.76F)),
         PartPose.offset(0.0F, 0.0F, -1.0F)
      );
      return LayerDefinition.create(meshdefinition, 32, 32);
   }
}
