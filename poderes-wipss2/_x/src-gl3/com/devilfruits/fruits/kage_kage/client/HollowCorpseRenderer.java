package com.devilfruits.fruits.kage_kage.client;

import com.devilfruits.DevilFruits;
import com.devilfruits.fruits.kage_kage.client.vfx.KageModel;
import com.devilfruits.fruits.kage_kage.entity.HollowCorpse;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class HollowCorpseRenderer extends EntityRenderer<HollowCorpse> {
   private static final KageModel MODEL = new KageModel("hollow_corpse");
   private static final ResourceLocation TEXTURE = DevilFruits.id("textures/entity/kage_kage/hollow_corpse.png");

   public HollowCorpseRenderer(Context context) {
      super(context);
      this.shadowRadius = 0.6F;
   }

   public ResourceLocation getTextureLocation(HollowCorpse e) {
      return TEXTURE;
   }

   public void render(HollowCorpse e, float entityYaw, float partial, PoseStack ps, MultiBufferSource buffers, int light) {
      if (MODEL.ready()) {
         MODEL.pose();
         if (e.injected()) {
            MODEL.play("twitch", (float)e.twitchTicks() + partial, false);
         } else {
            MODEL.play("limp", (float)e.tickCount + partial, true);
         }

         ps.pushPose();
         ps.mulPose(Axis.YP.rotationDegrees(180.0F - e.getYRot()));
         ps.mulPose(Axis.XP.rotationDegrees(90.0F));
         ps.translate(0.0F, 0.0F, -0.125F);
         ps.scale(0.0625F, 0.0625F, 0.0625F);
         MODEL.drawSkin(ps, buffers, TEXTURE, light, OverlayTexture.NO_OVERLAY);
         ps.popPose();
         super.render(e, entityYaw, partial, ps, buffers, light);
      }
   }
}
