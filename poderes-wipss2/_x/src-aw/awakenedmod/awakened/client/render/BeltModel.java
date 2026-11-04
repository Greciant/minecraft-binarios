package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public final class BeltModel {
   public static final ModelLayerLocation LAYER = new ModelLayerLocation(AwId.of("relic_belt"), "main");
   public static final int GEM_COUNT = 3;
   public static final String BAND = "band";

   private BeltModel() {
   }

   public static String gem(int i) {
      return "gem" + i;
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      root.addOrReplaceChild(
         "band", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, 9.0F, -2.0F, 8.0F, 3.0F, 4.0F, new CubeDeformation(0.35F)), PartPose.ZERO
      );
      float[] gemX = new float[]{-3.0F, -1.0F, 1.0F};

      for (int i = 0; i < 3; i++) {
         root.addOrReplaceChild(
            gem(i), CubeListBuilder.create().texOffs(40, 16).addBox(gemX[i], 9.6F, -3.0F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.ZERO
         );
      }

      return LayerDefinition.create(mesh, 64, 32);
   }
}
