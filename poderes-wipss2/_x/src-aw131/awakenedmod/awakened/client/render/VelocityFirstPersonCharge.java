package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.client.AwakenedAnimations;
import com.awakenedmod.awakened.content.velocity.VelocityPalette;
import com.awakenedmod.awakened.content.velocity.VelocityVisualState;
import com.awakenedmod.awakened.core.data.VelocityVisual;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;

public final class VelocityFirstPersonCharge {
   private static final float[] ARM_PIVOT = new float[]{-5.0F, 2.0F, 0.0F};
   private static final float FP_SCALE = 0.75F;
   private static final float ITEM_X = 0.56F;
   private static final float ITEM_Y = -0.52F;
   private static final float ITEM_Z = -0.72F;
   private static final float ITEM_EQUIP_DROP = -0.6F;
   private static final float FIST_X = 0.0F;
   private static final float FIST_Y = 0.04F;
   private static final float FIST_Z = 0.04F;
   private static final float HELD_PITCH_DEG = -80.0F;
   private static final float HELD_YAW_DEG = -8.0F;

   private VelocityFirstPersonCharge() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(VelocityFirstPersonCharge::onRenderArm);
      gameBus.addListener(VelocityFirstPersonCharge::onRenderHand);
   }

   private static void onRenderArm(RenderArmEvent event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.player.getMainHandItem().isEmpty() && event.getArm() == mc.player.getMainArm()) {
         VelocityVisual vis = charging();
         if (vis != null) {
            float side = event.getArm() == HumanoidArm.LEFT ? -1.0F : 1.0F;
            PoseStack ps = event.getPoseStack();
            ps.pushPose();
            ps.translate(side * ARM_PIVOT[0] / 16.0F, ARM_PIVOT[1] / 16.0F, ARM_PIVOT[2] / 16.0F);
            VelocityChargeStatic.palm(ps, side);
            ps.scale(0.75F, 0.75F, 0.75F);
            VelocityChargeStatic.draw(ps, new VfxDraw.Sink(event.getMultiBufferSource()), palette(), vis.chargeFraction(), age(), seed());
            ps.popPose();
         }
      }
   }

   private static void onRenderHand(RenderHandEvent event) {
      if (event.getHand() == InteractionHand.MAIN_HAND) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null && !mc.player.getMainHandItem().isEmpty()) {
            VelocityVisual vis = charging();
            if (vis != null) {
               float side = mc.player.getMainArm() == HumanoidArm.LEFT ? -1.0F : 1.0F;
               PoseStack ps = event.getPoseStack();
               ps.pushPose();
               poseHeldFist(ps, side, event.getEquipProgress(), event.getSwingProgress());
               ps.scale(0.75F, 0.75F, 0.75F);
               VelocityChargeStatic.draw(ps, new VfxDraw.Sink(event.getMultiBufferSource()), palette(), vis.chargeFraction(), age(), seed());
               ps.popPose();
            }
         }
      }
   }

   private static void poseHeldFist(PoseStack ps, float side, float equip, float swing) {
      ps.translate(side * 0.56F, -0.52F + equip * -0.6F, -0.72F);
      if (swing > 0.0F) {
         float root = Mth.sqrt(swing);
         ps.translate(side * -0.4F * Mth.sin(root * (float) Math.PI), 0.2F * Mth.sin(root * (float) (Math.PI * 2)), -0.2F * Mth.sin(swing * (float) Math.PI));
         float snap = Mth.sin(swing * swing * (float) Math.PI);
         ps.mulPose(Axis.YP.rotationDegrees(side * (45.0F + snap * -20.0F)));
         float arc = Mth.sin(root * (float) Math.PI);
         ps.mulPose(Axis.ZP.rotationDegrees(side * arc * -20.0F));
         ps.mulPose(Axis.XP.rotationDegrees(arc * -80.0F));
         ps.mulPose(Axis.YP.rotationDegrees(side * -45.0F));
      }

      ps.translate(side * 0.0F, 0.04F, 0.04F);
      ps.mulPose(Axis.YP.rotationDegrees(side * -8.0F));
      ps.mulPose(Axis.XP.rotationDegrees(-80.0F));
   }

   private static VelocityVisual charging() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && !AwakenedAnimations.libraryOwnsFirstPerson()) {
         VelocityVisual vis = VelocityVisualState.of(mc.player);
         return vis.charging() ? vis : null;
      } else {
         return null;
      }
   }

   private static VelocityPalette palette() {
      return VelocityPalette.of(Minecraft.getInstance().player);
   }

   private static float age() {
      return (float)Minecraft.getInstance().player.tickCount;
   }

   private static int seed() {
      return Minecraft.getInstance().player.getId();
   }
}
