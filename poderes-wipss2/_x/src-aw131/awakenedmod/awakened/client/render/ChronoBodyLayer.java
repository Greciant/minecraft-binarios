package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.client.chrono.ChronoBodies;
import com.awakenedmod.awakened.core.Lab;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

public final class ChronoBodyLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
   public static final Lab.Value DEVELOP_ALPHA = Lab.CLIENT.def("chrono.body.develop_alpha", 0.8, 0.0, 1.0, 0.01);
   public static final int SEPIA = 15784360;
   private final LivingEntityRenderer<T, M> renderer;

   public ChronoBodyLayer(LivingEntityRenderer<T, M> renderer) {
      super(renderer);
      this.renderer = renderer;
   }

   public static void attach(LivingEntityRenderer<?, ?> renderer) {
      renderer.addLayer(new ChronoBodyLayer((LivingEntityRenderer<T, M>)renderer));
   }

   public void render(
      PoseStack poseStack,
      MultiBufferSource buffers,
      int packedLight,
      T entity,
      float limbSwing,
      float limbSwingAmount,
      float partialTick,
      float ageInTicks,
      float netHeadYaw,
      float headPitch
   ) {
      ChronoBodies.Look look = ChronoBodies.look(entity);
      if (look != null && !entity.isInvisible()) {
         if (look.mode() != ChronoBodies.Mode.NEGATIVE || NegativeLayers.available()) {
            M model = (M)this.getParentModel();
            if (look.mode() == ChronoBodies.Mode.NEGATIVE) {
               model.renderToBuffer(poseStack, buffers.getBuffer(NegativeLayers.model()), packedLight, OverlayTexture.NO_OVERLAY, negativeColor(look));
            } else {
               ResourceLocation texture = this.renderer.getTextureLocation(entity);
               model.renderToBuffer(
                  poseStack, buffers.getBuffer(RenderType.entityTranslucent(texture)), packedLight, OverlayTexture.NO_OVERLAY, developColor(look)
               );
            }
         }
      }
   }

   private static int negativeColor(ChronoBodies.Look look) {
      return -1;
   }

   private static int developColor(ChronoBodies.Look look) {
      int a = Mth.clamp(Math.round(look.amount() * DEVELOP_ALPHA.getF() * 255.0F), 0, 255);
      return a << 24 | 15784360;
   }
}
