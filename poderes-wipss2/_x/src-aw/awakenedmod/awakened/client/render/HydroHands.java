package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.client.AwakenedAnimations;
import com.awakenedmod.awakened.core.AimUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class HydroHands {
   private static final double FP_WIDTH = 0.37;
   private static final double FP_DROP = 0.37;
   private static final double FP_REACH = 0.7;

   private HydroHands() {
   }

   static boolean ownFirstPerson(Entity e) {
      Minecraft mc = Minecraft.getInstance();
      return e != null && mc.getCameraEntity() == e && mc.options.getCameraType().isFirstPerson();
   }

   static boolean mainArmLeft(Player p) {
      return p.getMainArm() == HumanoidArm.LEFT;
   }

   static Vec3 hand(Player p, float partialTick, float liftDeg, boolean leftArm) {
      if (!ownFirstPerson(p)) {
         return thirdPerson(p, partialTick, liftDeg, leftArm);
      } else {
         float armPitch = AwakenedAnimations.firstPersonArmPitch(p.getViewXRot(partialTick)) - liftDeg;
         return AimUtil.shoulderHand(p.getEyePosition(partialTick), (double)p.getViewYRot(partialTick), (double)armPitch, leftArm ? -1.0 : 1.0, 0.37, 0.37, 0.7);
      }
   }

   static Vec3 thirdPerson(Player p, float partialTick, float liftDeg, boolean leftArm) {
      float yaw = Mth.rotLerp(partialTick, p.yBodyRotO, p.yBodyRot);
      return AimUtil.shoulderHand(p.getEyePosition(partialTick), (double)yaw, (double)(-liftDeg), leftArm ? -1.0 : 1.0, 0.31, 0.25, 0.7);
   }
}
