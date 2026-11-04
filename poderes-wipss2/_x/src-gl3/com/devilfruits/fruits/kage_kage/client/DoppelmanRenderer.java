package com.devilfruits.fruits.kage_kage.client;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.model.geo.GeoPose;
import com.devilfruits.fruits.kage_kage.client.vfx.KageModel;
import com.devilfruits.fruits.kage_kage.client.vfx.RimConsumer;
import com.devilfruits.fruits.kage_kage.entity.Doppelman;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class DoppelmanRenderer extends EntityRenderer<Doppelman> {
   private static final KageModel MODEL = new KageModel("doppelman", "body", "head", "right_arm", "left_arm", "right_leg", "left_leg").silhouetteRim();
   private static final ResourceLocation TEXTURE = DevilFruits.id("textures/entity/kage_kage/doppelman.png");
   private static final float STRIDE = 1.7F;
   private final RimConsumer rim = new RimConsumer();

   public DoppelmanRenderer(Context context) {
      super(context);
      this.shadowRadius = 0.4F;
   }

   public ResourceLocation getTextureLocation(Doppelman e) {
      return TEXTURE;
   }

   public void render(Doppelman e, float entityYaw, float partial, PoseStack ps, MultiBufferSource buffers, int light) {
      if (MODEL.ready()) {
         float age = (float)e.tickCount + partial;
         GeoPose pose = MODEL.pose();
         float st = (float)e.stateTicks() + partial;
         if (e.deathTime > 0) {
            MODEL.play("sink", ((float)e.deathTime + partial) * 20.0F / 30.0F, false);
         } else if (e.state() == 1) {
            MODEL.play("rise", st, false);
         } else if (e.state() == 2) {
            MODEL.play("sink", st, false);
         } else {
            float walk = Mth.clamp(e.anim.speed / 2.2F, 0.0F, 1.0F);
            float attack = 0.0F;
            if (e.anim.actionId == 0 && e.anim.actionAge < 12) {
               float a = (float)e.anim.actionAge + partial;
               attack = Mth.clamp(Math.min(a / 2.0F, (12.0F - a) / 3.0F), 0.0F, 1.0F);
               MODEL.play("attack", a, false, attack);
            }

            float rest = 1.0F - attack;
            float phase = Mth.lerp(partial, e.anim.prevDistance, e.anim.distance) / 1.7F * 20.0F;
            MODEL.play("walk", phase, true, walk * rest);
            MODEL.play("idle", age, true, (1.0F - walk) * rest);
            int head = MODEL.bone("head");
            if (head >= 0) {
               float bodyYaw = Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot);
               float headYaw = Mth.wrapDegrees(Mth.rotLerp(partial, e.yHeadRotO, e.yHeadRot) - bodyYaw);
               float pitch = Mth.lerp(partial, e.xRotO, e.getXRot());
               pose.addRotation(head, -pitch * (float) (Math.PI / 180.0) * 0.8F, -headYaw * (float) (Math.PI / 180.0) * 0.8F, 0.0F);
            }
         }

         float bodyYaw = Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot);
         int overlay = OverlayTexture.pack(OverlayTexture.u(0.0F), OverlayTexture.v(e.hurtTime > 0 && e.deathTime == 0));
         ps.pushPose();
         ps.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
         ps.scale(0.0625F, 0.0625F, 0.0625F);
         MODEL.draw(false, ps, buffers, light, overlay, 1.0F, 1.0F, 0.0F, this.rim);
         float glow = e.deathTime > 0 ? Math.max(0.0F, 1.0F - (float)e.deathTime / 20.0F) : 1.0F;
         MODEL.draw(true, ps, buffers, light, overlay, glow, 1.0F, 0.028F, this.rim);
         ps.popPose();
         super.render(e, entityYaw, partial, ps, buffers, light);
      }
   }
}
