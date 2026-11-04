package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public final class GauntletModel {
   public static final ModelLayerLocation LAYER = new ModelLayerLocation(AwId.of("web_gauntlets"), "main");
   public static final String CUFF = "cuff";

   private GauntletModel() {
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      PartDefinition cuff = partdefinition.addOrReplaceChild(
         "cuff",
         CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(-2.0F, 6.0F, -2.0F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.4F))
            .texOffs(0, 8)
            .addBox(-1.0F, 6.6F, -3.1F, 2.0F, 2.0F, 1.2F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      return LayerDefinition.create(meshdefinition, 64, 32);
   }
}
