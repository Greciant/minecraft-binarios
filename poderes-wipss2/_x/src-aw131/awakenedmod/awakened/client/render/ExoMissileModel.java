package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public final class ExoMissileModel {
   public static final ModelLayerLocation LAYER = new ModelLayerLocation(AwId.of("exo_missile"), "main");
   public static final String BONE = "RightMissile";
   public static final float PIVOT_X = 0.5F;
   public static final float PIVOT_Y = 24.0F;
   public static final float PIVOT_Z = 0.0F;

   private ExoMissileModel() {
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      PartDefinition RightMissile = partdefinition.addOrReplaceChild(
         "RightMissile",
         CubeListBuilder.create()
            .texOffs(10, 5)
            .addBox(-1.324F, -0.449F, -1.351F, 2.0F, 4.0F, 2.0F, new CubeDeformation(-0.66F))
            .texOffs(0, 11)
            .addBox(-1.3375F, 2.1125F, -1.3375F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.75F))
            .texOffs(0, 0)
            .addBox(-1.325F, -0.9998F, -2.0542F, 2.0F, 3.0F, 3.0F, new CubeDeformation(-0.955F))
            .texOffs(0, 6)
            .addBox(-1.65F, -1.0105F, -1.3075F, 3.0F, 3.0F, 2.0F, new CubeDeformation(-0.95F))
            .texOffs(10, 0)
            .addBox(-2.0F, -1.0105F, -1.3075F, 3.0F, 3.0F, 2.0F, new CubeDeformation(-0.951F)),
         PartPose.offsetAndRotation(0.5F, 24.0F, 0.0F, -1.5708F, 0.0F, 0.0F)
      );
      return LayerDefinition.create(meshdefinition, 32, 32);
   }
}
