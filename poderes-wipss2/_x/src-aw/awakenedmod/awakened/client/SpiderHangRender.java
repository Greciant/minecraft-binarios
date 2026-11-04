package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.data.SpiderVisual;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderPlayerEvent.Post;
import net.neoforged.neoforge.client.event.RenderPlayerEvent.Pre;
import org.joml.Quaternionf;

public final class SpiderHangRender {
   private static final float PIVOT_Y = 0.9F;
   static final float GLIDE_DROP = 0.6F;
   private static boolean pushed;

   private SpiderHangRender() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(SpiderHangRender::onPre);
      gameBus.addListener(SpiderHangRender::onPost);
   }

   private static void applyFlip(PoseStack poseStack, float yaw) {
      poseStack.pushPose();
      poseStack.translate(0.0F, 0.9F, 0.0F);
      poseStack.mulPose(new Quaternionf().rotationAxis((float) Math.PI, -((float)Math.sin((double)yaw)), 0.0F, (float)Math.cos((double)yaw)));
      poseStack.translate(0.0F, -0.9F, 0.0F);
      pushed = true;
   }

   private static void applyGlide(PoseStack poseStack, float yaw, float angle) {
      poseStack.pushPose();
      poseStack.translate(0.0F, 0.29999995F, 0.0F);
      poseStack.mulPose(new Quaternionf().rotationAxis(angle, (float)Math.cos((double)yaw), 0.0F, (float)Math.sin((double)yaw)));
      poseStack.translate(0.0F, -0.9F, 0.0F);
      pushed = true;
   }

   private static boolean isHang(Player p) {
      if (p == Minecraft.getInstance().player) {
         return SpiderClientState.hanging();
      } else {
         SpiderVisual v = (SpiderVisual)p.getData(AwakenedAttachments.SPIDER_VISUAL);
         return v.tether() == 1 && v.hanging();
      }
   }

   private static boolean isGlide(Player p) {
      return p == Minecraft.getInstance().player ? SpiderSwingController.gliding() : ((SpiderVisual)p.getData(AwakenedAttachments.SPIDER_VISUAL)).gliding();
   }

   private static float bodyYaw(Player p, float pt) {
      return (float)Math.toRadians((double)Mth.rotLerp(pt, p.yBodyRotO, p.yBodyRot));
   }

   private static float glideAngle(Player p, float pt) {
      float lookPitch = p.getViewXRot(pt);
      return (float)SpiderSwingController.glidePitchRadians(lookPitch);
   }

   private static void onPre(Pre event) {
      pushed = false;
      Player p = event.getEntity();
      float pt = event.getPartialTick();
      if (isHang(p)) {
         applyFlip(event.getPoseStack(), bodyYaw(p, pt));
      } else if (isGlide(p)) {
         applyGlide(event.getPoseStack(), bodyYaw(p, pt), glideAngle(p, pt));
      }
   }

   private static void onPost(Post event) {
      if (pushed) {
         event.getPoseStack().popPose();
         pushed = false;
      }
   }
}
