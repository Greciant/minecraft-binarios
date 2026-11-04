package com.devilfruits.fruits.kage_kage.client;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.model.geo.GeoPose;
import com.devilfruits.fruits.kage_kage.client.vfx.KageModel;
import com.devilfruits.fruits.kage_kage.entity.ShadowZombie;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class ShadowZombieRenderer extends EntityRenderer<ShadowZombie> {
   private static final KageModel MODEL = new KageModel("shadow_zombie");
   private static final ResourceLocation[] SKINS = new ResourceLocation[12];
   private static final float STRIDE = 1.3F;

   public ShadowZombieRenderer(Context context) {
      super(context);
      this.shadowRadius = 0.45F;
   }

   public ResourceLocation getTextureLocation(ShadowZombie e) {
      return SKINS[Math.floorMod(e.variant(), SKINS.length)];
   }

   public void render(ShadowZombie e, float entityYaw, float partial, PoseStack ps, MultiBufferSource buffers, int light) {
      if (MODEL.ready()) {
         float age = (float)e.tickCount + partial;
         GeoPose pose = MODEL.pose();
         if (e.deathTime > 0) {
            MODEL.play("collapse", ((float)e.deathTime + partial) * 20.0F / 30.0F, false);
         } else if (e.rising()) {
            MODEL.play("rise", (float)e.risingTicks() + partial, false);
         } else {
            float walk = Mth.clamp(e.anim.speed / 1.6F, 0.0F, 1.0F);
            float attack = 0.0F;
            if (e.anim.actionId == 0 && e.anim.actionAge < 12) {
               float a = (float)e.anim.actionAge + partial;
               attack = Mth.clamp(Math.min(a / 2.0F, (12.0F - a) / 3.0F), 0.0F, 1.0F);
               MODEL.play("attack", a, false, attack);
            }

            float rest = 1.0F - attack;
            float phase = Mth.lerp(partial, e.anim.prevDistance, e.anim.distance) / 1.3F * 20.0F;
            MODEL.play("walk", phase, true, walk * rest);
            MODEL.play("idle", age, true, (1.0F - walk) * rest);
            int head = MODEL.bone("head");
            if (head >= 0) {
               float bodyYaw = Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot);
               float headYaw = Mth.wrapDegrees(Mth.rotLerp(partial, e.yHeadRotO, e.yHeadRot) - bodyYaw);
               float pitch = Mth.lerp(partial, e.xRotO, e.getXRot());
               pose.addRotation(head, -pitch * (float) (Math.PI / 180.0) * 0.6F, -headYaw * (float) (Math.PI / 180.0) * 0.6F, 0.0F);
            }
         }

         float bodyYaw = Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot);
         int overlay = OverlayTexture.pack(OverlayTexture.u(0.0F), OverlayTexture.v(e.hurtTime > 0 && e.deathTime == 0));
         ps.pushPose();
         ps.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
         ps.scale(0.0625F, 0.0625F, 0.0625F);
         MODEL.drawSkin(ps, buffers, this.getTextureLocation(e), light, overlay);
         ps.popPose();
         super.render(e, entityYaw, partial, ps, buffers, light);
      }
   }

   static {
      for (int i = 0; i < SKINS.length; i++) {
         SKINS[i] = DevilFruits.id("textures/entity/kage_kage/shadow_zombie" + (i == 0 ? "" : "_" + i) + ".png");
      }
   }
}
