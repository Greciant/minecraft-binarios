package com.devilfruits.fruits.uo_uo_seiryu.client.render;

import com.devilfruits.DevilFruits;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuClientConfig;
import com.devilfruits.fruits.uo_uo_seiryu.UoUoSeiryu;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;

public class SeiryuHumanLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
   public static final ModelLayerLocation LAYER = new ModelLayerLocation(UoUoSeiryu.id("human_overlay"), "main");
   private static final ResourceLocation TEXTURE = DevilFruits.id("textures/entity/uo_uo_seiryu/human_overlay.png");
   private final ModelPart horns;
   private final ModelPart eyes;
   private final ModelPart belt;
   private final ModelPart rightBand;
   private final ModelPart leftBand;
   private final boolean slim;

   public SeiryuHumanLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, ModelPart root, boolean slim) {
      super(parent);
      this.horns = root.getChild("horns");
      this.eyes = root.getChild("eyes");
      this.belt = root.getChild("belt");
      this.rightBand = root.getChild("right_band");
      this.leftBand = root.getChild("left_band");
      this.slim = slim;
   }

   public static LayerDefinition createLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      root.addOrReplaceChild(
         "horns",
         CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(-3.5F, -10.5F, -2.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.1F))
            .texOffs(0, 0)
            .mirror()
            .addBox(2.5F, -10.5F, -2.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.1F)),
         PartPose.ZERO
      );
      root.addOrReplaceChild("eyes", CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, -4.0F, -4.08F, 8.0F, 1.0F, 0.0F), PartPose.ZERO);
      root.addOrReplaceChild(
         "belt",
         CubeListBuilder.create()
            .texOffs(0, 8)
            .addBox(-4.0F, 10.0F, -2.0F, 8.0F, 2.0F, 4.0F, new CubeDeformation(0.45F))
            .texOffs(24, 8)
            .addBox(-3.0F, 11.5F, -2.6F, 2.0F, 4.0F, 0.0F)
            .texOffs(24, 8)
            .mirror()
            .addBox(1.0F, 11.5F, -2.6F, 2.0F, 4.0F, 0.0F),
         PartPose.ZERO
      );
      root.addOrReplaceChild("right_band", band(-3.0F), PartPose.ZERO);
      root.addOrReplaceChild("left_band", band(-1.0F), PartPose.ZERO);
      return LayerDefinition.create(mesh, 64, 32);
   }

   private static CubeListBuilder band(float x) {
      CubeDeformation d = new CubeDeformation(0.4F);
      return CubeListBuilder.create()
         .texOffs(0, 16)
         .addBox(x, 7.0F, -2.0F, 4.0F, 2.0F, 4.0F, d)
         .texOffs(4, 0)
         .addBox(x + 1.5F, 7.5F, -3.2F, 1.0F, 1.0F, 1.0F)
         .texOffs(4, 0)
         .addBox(x + 1.5F, 7.5F, 2.2F, 1.0F, 1.0F, 1.0F)
         .texOffs(4, 0)
         .addBox(x - 1.2F, 7.5F, -0.5F, 1.0F, 1.0F, 1.0F)
         .texOffs(4, 0)
         .addBox(x + 4.2F, 7.5F, -0.5F, 1.0F, 1.0F, 1.0F);
   }

   public void render(
      PoseStack poseStack,
      MultiBufferSource buffers,
      int packedLight,
      AbstractClientPlayer player,
      float limbSwing,
      float limbSwingAmount,
      float partialTick,
      float ageInTicks,
      float netHeadYaw,
      float headPitch
   ) {
      if (!player.isInvisible() && UoUoSeiryu.isIn(player, "human") && (Boolean)SeiryuClientConfig.HUMAN_OVERLAY.get()) {
         PlayerModel<AbstractClientPlayer> model = (PlayerModel<AbstractClientPlayer>)this.getParentModel();
         VertexConsumer cutout = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
         int overlay = OverlayTexture.NO_OVERLAY;
         if (player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            poseStack.pushPose();
            model.head.translateAndRotate(poseStack);
            this.horns.render(poseStack, cutout, packedLight, overlay);
            poseStack.popPose();
         }

         if (player.getItemBySlot(EquipmentSlot.LEGS).isEmpty()) {
            poseStack.pushPose();
            model.body.translateAndRotate(poseStack);
            this.belt.render(poseStack, cutout, packedLight, overlay);
            poseStack.popPose();
         }

         poseStack.pushPose();
         model.rightArm.translateAndRotate(poseStack);
         if (this.slim) {
            poseStack.translate(0.03125F, 0.0F, 0.0F);
            poseStack.scale(0.8F, 1.0F, 1.0F);
         }

         this.rightBand.render(poseStack, cutout, packedLight, overlay);
         poseStack.popPose();
         poseStack.pushPose();
         model.leftArm.translateAndRotate(poseStack);
         if (this.slim) {
            poseStack.translate(-0.03125F, 0.0F, 0.0F);
            poseStack.scale(0.8F, 1.0F, 1.0F);
         }

         this.leftBand.render(poseStack, cutout, packedLight, overlay);
         poseStack.popPose();
         if ((Boolean)SeiryuClientConfig.HUMAN_EYE_GLOW.get() && player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            poseStack.pushPose();
            model.head.translateAndRotate(poseStack);
            this.eyes.render(poseStack, buffers.getBuffer(RenderType.eyes(TEXTURE)), 15728880, overlay);
            poseStack.popPose();
         }
      }
   }
}
