package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.client.ClientPowerState;
import com.awakenedmod.awakened.client.ExoSuitAnim;
import com.awakenedmod.awakened.client.ExoSuitVisibility;
import com.awakenedmod.awakened.content.exosavant.ExoPiece;
import com.awakenedmod.awakened.content.exosavant.ExoSkin;
import com.awakenedmod.awakened.content.exosavant.ExoSkins;
import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.data.ExoVisual;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import org.joml.Quaternionf;

public final class ExoFirstPersonArm {
   private static ModelPart root;
   private static ModelPart rightArm;
   private static ModelPart rightGauntlet;
   private static ModelPart undersuitRightArm;
   private static ModelPart undersuitLeftArm;
   private static final float[] ARM_PIVOT = new float[]{-5.0F, 2.0F, 0.0F};
   private static final float[] SEAT_RIGHT_ARM = new float[]{5.0F, 10.0F, 0.0F};
   private static final float[] SEAT_RIGHT_GAUNTLET = new float[]{5.0F, -2.0F, 0.0F};
   private static final float FIT_OFFSET_X = 0.0F;
   private static final float FIT_OFFSET_Y = 0.0F;
   private static final float FIT_OFFSET_Z = 0.0F;
   private static final float FIT_SCALE = 1.0F;
   private static final float FIT_PITCH_DEG = 0.0F;
   private static final float FIT_YAW_DEG = 0.0F;
   private static final float FIT_ROLL_DEG = 0.0F;
   private static final float AIM_X = 0.36F;
   private static final float AIM_Y = -0.38F;
   private static final float AIM_Z = -0.1F;
   private static final float AIM_PITCH_DEG = -87.0F;
   private static final float AIM_YAW_DEG = -5.0F;
   private static final float AIM_ROLL_DEG = 0.0F;
   private static final float AIM_LOWERED_DEG = 38.0F;
   private static final float AIM_DROP = 0.16F;
   private static final float RECOIL_BACK = 0.13F;
   private static final float RECOIL_PITCH_DEG = 11.0F;
   private static final double RECOIL_SECONDS = 0.2;
   private static final float FIRING_EPS = 0.001F;
   private static long lastFiredNanos = Long.MIN_VALUE;
   private static boolean prevDeploy;

   private ExoFirstPersonArm() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(ExoFirstPersonArm::onRenderArm);
      gameBus.addListener(ExoFirstPersonArm::onRenderHand);
   }

   private static void onRenderArm(RenderArmEvent event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.player.getMainHandItem().isEmpty() && event.getArm() == mc.player.getMainArm()) {
         ExoVisual vis = activeVisual();
         if (vis != null) {
            event.setCanceled(true);
            float phase = advancePhase();
            if (!(phase > 0.001F)) {
               float side = event.getArm() == HumanoidArm.LEFT ? -1.0F : 1.0F;
               PoseStack ps = event.getPoseStack();
               renderIdleOverlay(event.getMultiBufferSource(), ps, side, event.getPackedLight());
            }
         }
      }
   }

   private static void onRenderHand(RenderHandEvent event) {
      if (event.getHand() == InteractionHand.MAIN_HAND) {
         Minecraft mc = Minecraft.getInstance();
         ExoVisual vis = activeVisual();
         if (vis != null) {
            float phase = advancePhase();
            if (!(phase <= 0.001F)) {
               float side = mc.player.getMainArm() == HumanoidArm.LEFT ? -1.0F : 1.0F;
               PoseStack ps = event.getPoseStack();
               renderFiringArm(event.getMultiBufferSource(), ps, phase, side, event.getPackedLight());
               event.setCanceled(true);
            }
         }
      }
   }

   private static ExoVisual activeVisual() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || !mc.options.getCameraType().isFirstPerson()) {
         return null;
      } else if (!"exosavant".equals(ClientPowerState.get().powerId())) {
         return null;
      } else {
         ExoVisual vis = (ExoVisual)mc.player.getData(AwakenedAttachments.EXO_VISUAL);
         int mask = ExoSuitVisibility.visibleMask(mc.player, vis.builtMask());
         if (vis.suited() && (mask & ExoPiece.GAUNTLETS.bit()) != 0) {
            ensureBaked();
            return vis;
         } else {
            return null;
         }
      }
   }

   private static float advancePhase() {
      Minecraft mc = Minecraft.getInstance();
      ExoVisual vis = (ExoVisual)mc.player.getData(AwakenedAttachments.EXO_VISUAL);
      float phase = ExoSuitAnim.update(mc.player.getId(), vis.flying(), vis.barrageActive(), vis.handCannonDeploy(), vis.visorOpen()).handCannon();
      boolean deploy = vis.handCannonDeploy();
      if (deploy && !prevDeploy) {
         lastFiredNanos = System.nanoTime();
      }

      prevDeploy = deploy;
      return phase;
   }

   private static ExoSkin skin() {
      Minecraft mc = Minecraft.getInstance();
      return mc.player == null ? ExoSkin.DEFAULT : ExoSkins.worn(mc.player);
   }

   private static void renderIdleOverlay(MultiBufferSource sink, PoseStack ps, float side, int light) {
      sampleCannonClip(0.0F);
      ps.pushPose();
      poseUndersuit(ps, side);
      undersuitArm(side).render(ps, sink.getBuffer(RenderType.entityCutoutNoCull(ExosavantSuitLayer.UNDERSUIT_TEXTURE)), light, OverlayTexture.NO_OVERLAY);
      ps.popPose();
      drawGroup(sink, rightArm, SEAT_RIGHT_ARM, side, ps, light);
      drawGroup(sink, rightGauntlet, SEAT_RIGHT_GAUNTLET, side, ps, light);
   }

   private static void renderFiringArm(MultiBufferSource sink, PoseStack ps, float phase, float side, int light) {
      sampleCannonClip(phase);
      ps.pushPose();
      poseFiringSpace(ps, phase, side);
      drawFiringGroup(sink, rightArm, SEAT_RIGHT_ARM, side, ps, light);
      drawFiringGroup(sink, rightGauntlet, SEAT_RIGHT_GAUNTLET, side, ps, light);
      ps.popPose();
   }

   private static void drawGroup(MultiBufferSource sink, ModelPart group, float[] seat, float side, PoseStack ps, int light) {
      ps.pushPose();
      poseArm(ps, seat, side);
      renderGroup(sink, group, ps, light);
      ps.popPose();
   }

   private static void drawFiringGroup(MultiBufferSource sink, ModelPart group, float[] seat, float side, PoseStack ps, int light) {
      ps.pushPose();
      poseFiringSeat(ps, seat, side);
      renderGroup(sink, group, ps, light);
      ps.popPose();
   }

   private static void renderGroup(MultiBufferSource sink, ModelPart group, PoseStack ps, int light) {
      ExoSkin skin = skin();
      VertexConsumer steel = sink.getBuffer(RenderType.entityCutoutNoCull(skin.suit()));
      group.render(ps, steel, light, OverlayTexture.NO_OVERLAY);
      VertexConsumer glow = sink.getBuffer(RenderType.entityTranslucentEmissive(skin.emissive()));
      group.render(ps, glow, light, OverlayTexture.NO_OVERLAY);
   }

   private static void sampleCannonClip(float phase) {
      root.getAllParts().forEach(ModelPart::resetPose);
      float t = Mth.clamp(phase, 0.0F, 2.0F);
      if (t > 0.0F) {
         if (t <= 1.0F) {
            TechArmorAnimation.apply(root, TechArmorAnimation.HandCannonOpen, (long)(t * TechArmorAnimation.HandCannonOpen.lengthInSeconds() * 1000.0F));
         } else {
            TechArmorAnimation.apply(
               root, TechArmorAnimation.HandCannonClose, (long)((t - 1.0F) * TechArmorAnimation.HandCannonClose.lengthInSeconds() * 1000.0F)
            );
         }
      }

      ExosavantSuitLayer.unflipAnimationDelta(root);
   }

   private static void poseArm(PoseStack ps, float[] seat, float side) {
      ps.translate(0.0F, 0.0F, 0.0F);
      ps.mulPose(new Quaternionf().rotationZYX(rad(0.0F), rad(0.0F), rad(0.0F)));
      ps.scale(1.0F, 1.0F, 1.0F);
      ps.translate(side * ARM_PIVOT[0] / 16.0F, ARM_PIVOT[1] / 16.0F, ARM_PIVOT[2] / 16.0F);
      ps.translate(side * seat[0] / 16.0F, seat[1] / 16.0F, seat[2] / 16.0F);
      ps.scale(1.0F, -1.0F, 1.0F);
   }

   private static void poseUndersuit(PoseStack ps, float side) {
      ps.translate(0.0F, 0.0F, 0.0F);
      ps.mulPose(new Quaternionf().rotationZYX(rad(0.0F), rad(0.0F), rad(0.0F)));
      ps.scale(1.0F, 1.0F, 1.0F);
      ps.translate(side * ARM_PIVOT[0] / 16.0F, ARM_PIVOT[1] / 16.0F, ARM_PIVOT[2] / 16.0F);
   }

   private static ModelPart undersuitArm(float side) {
      return side < 0.0F ? undersuitLeftArm : undersuitRightArm;
   }

   private static void poseFiringSpace(PoseStack ps, float phase, float side) {
      float deploy = phase <= 1.0F ? phase : 2.0F - phase;
      float raise = Mth.clamp(deploy, 0.0F, 1.0F);
      float recoil = recoilEnvelope();
      ps.translate(side * 0.36F, -0.38F - 0.16F * (1.0F - raise), -0.1F + 0.13F * recoil);
      ps.mulPose(Axis.XP.rotationDegrees(11.0F * recoil));
      ps.scale(-1.0F, -1.0F, 1.0F);
      ps.mulPose(new Quaternionf().rotationZYX(side * rad(0.0F), side * rad(-5.0F), rad(-87.0F + 38.0F * (1.0F - raise))));
   }

   private static void poseFiringSeat(PoseStack ps, float[] seat, float side) {
      ps.translate(side * seat[0] / 16.0F, seat[1] / 16.0F, seat[2] / 16.0F);
      ps.scale(1.0F, -1.0F, 1.0F);
   }

   private static float recoilEnvelope() {
      if (lastFiredNanos == Long.MIN_VALUE) {
         return 0.0F;
      } else {
         double age = (double)(System.nanoTime() - lastFiredNanos) / 1.0E9;
         if (!(age < 0.0) && !(age >= 0.2)) {
            float t = (float)(age / 0.2);
            return (1.0F - t) * (1.0F - t);
         } else {
            return 0.0F;
         }
      }
   }

   private static void ensureBaked() {
      if (root == null) {
         root = ExosavantSuitModel.createBodyLayer().bakeRoot();
         SuitModelUtil.applyThrusterUnderside(root);
         rightArm = root.getChild("Waist").getChild("RightArm");
         rightGauntlet = root.getChild("Gauntlets").getChild("RightGauntlet");
         ModelPart underRoot = ExosavantUndersuitModel.createBodyLayer().bakeRoot();
         undersuitRightArm = underRoot.getChild("right_arm");
         undersuitLeftArm = underRoot.getChild("left_arm");
      }
   }

   private static float rad(float deg) {
      return (float)Math.toRadians((double)deg);
   }
}
