package com.awakenedmod.awakened.mixin;

import com.awakenedmod.awakened.client.render.FirstPersonPassLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LivingEntityRenderer.class})
public abstract class ExoFirstPersonSuitMixin {
   @Shadow
   @Final
   protected List layers;

   @Inject(
      method = {"render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V"
      )}
   )
   private void awakened$reSubmitFirstPersonLayers(
      LivingEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci
   ) {
      if (entity instanceof AbstractClientPlayer player && player == Minecraft.getInstance().player && FirstPersonMode.isFirstPersonPass()) {
         for (Object layer : this.layers) {
            if (layer instanceof FirstPersonPassLayer) {
               FirstPersonPassLayer fp = (FirstPersonPassLayer)layer;
               if (layer instanceof RenderLayer) {
                  RenderLayer rl = (RenderLayer)layer;
                  if (fp.showsInFirstPerson(player)) {
                     rl.render(poseStack, buffer, packedLight, player, 0.0F, 0.0F, partialTicks, 0.0F, 0.0F, 0.0F);
                  }
               }
            }
         }

         return;
      }
   }
}
