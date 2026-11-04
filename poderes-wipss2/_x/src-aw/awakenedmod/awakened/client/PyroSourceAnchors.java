package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.core.AimUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class PyroSourceAnchors {
   private static final float FP_WIDTH = 0.37F;
   private static final float FP_DROP = 0.37F;
   private static final float FP_REACH = 0.7F;
   private static final float TP_WIDTH = 0.31F;
   private static final float TP_DROP = 0.25F;
   private static final float TP_REACH = 0.7F;
   private static final double HIP_HEIGHT = 0.75;
   private static final double EYE_OVER_HIP = 0.87F;
   private static final double LEG_WIDTH = 0.11875;
   private static final double LEG_REACH = 0.75;
   private static final double FP_CRUISE_HAND_BACK = 0.55;
   private static final double FP_CRUISE_HAND_OUT = 0.28;
   private static final double FP_CRUISE_HAND_DROP = 0.15;
   private static final double FP_CRUISE_FOOT_BACK = 1.15;
   private static final double FP_CRUISE_FOOT_OUT = 0.12;
   private static final double FP_CRUISE_FOOT_DROP = 0.25;
   private static final double FP_HOVER_HAND_OUT = 0.4;
   private static final double FP_HOVER_HAND_DROP = 0.6;
   private static final double FP_HOVER_HAND_BACK = 0.15;
   private static final double FP_HOVER_FOOT_OUT = 0.13;
   private static final double FP_HOVER_FOOT_DROP = 1.2;

   private PyroSourceAnchors() {
   }

   public static PyroSourceAnchors.Nozzles beamPalms(LivingEntity p, float partialTick, Vec3 eye, boolean firstPerson) {
      float yaw = p.getViewYRot(partialTick);
      float look = p.getViewXRot(partialTick);
      float armPitch = firstPerson ? AwakenedAnimations.firstPersonArmPitch(look) : AwakenedAnimations.aimedChannelArmPitch(p, look, p.isCrouching());
      float width = firstPerson ? 0.37F : 0.31F;
      float drop = firstPerson ? 0.37F : 0.25F;
      float reach = firstPerson ? 0.7F : 0.7F;
      return new PyroSourceAnchors.Nozzles(
         AimUtil.shoulderHand(eye, (double)yaw, (double)armPitch, -1.0, (double)width, (double)drop, (double)reach),
         AimUtil.shoulderHand(eye, (double)yaw, (double)armPitch, 1.0, (double)width, (double)drop, (double)reach)
      );
   }

   public static PyroSourceAnchors.Nozzles jetAnchors(LivingEntity p, float partialTick, Vec3 eye, boolean firstPerson) {
      if (firstPerson) {
         return jetAnchorsFirstPerson(p, partialTick, eye);
      } else {
         float yaw = p.getViewYRot(partialTick);
         double body = Math.toRadians((double)AwakenedAnimations.jetBodyPitch(p));
         double width = 0.31F;
         double drop = 0.25;
         double reach = 0.7F;
         Vec3 forward = AimUtil.direction((double)yaw, 0.0);
         Vec3 right = AimUtil.horizontalRight((double)yaw);
         double up = 0.87F - drop;
         Vec3 hip = eye.add(0.0, 0.75 - (double)p.getEyeHeight() + (double)AwakenedAnimations.jetBodySink(p), 0.0);
         Vec3 shoulderMid = hip.add(0.0, up * Math.cos(body), 0.0).add(forward.scale(up * Math.sin(body)));
         double armLook = (double)AwakenedAnimations.jetArmPitch(p) + 90.0;
         double splay = Math.toRadians((double)AwakenedAnimations.jetArmSplay(p));
         Vec3 alongArm = AimUtil.direction((double)yaw, armLook).scale(reach * Math.cos(splay));
         double out = width + reach * Math.sin(splay);
         double legLook = (double)AwakenedAnimations.jetLegPitch(p) + 90.0;
         double legSplay = Math.toRadians((double)AwakenedAnimations.jetLegSplay(p));
         Vec3 alongLeg = AimUtil.direction((double)yaw, legLook).scale(0.75 * Math.cos(legSplay));
         double legOut = 0.11875 + 0.75 * Math.sin(legSplay);
         return new PyroSourceAnchors.Nozzles(
            shoulderMid.add(right.scale(-out)).add(alongArm),
            shoulderMid.add(right.scale(out)).add(alongArm),
            hip.add(right.scale(-legOut)).add(alongLeg),
            hip.add(right.scale(legOut)).add(alongLeg)
         );
      }
   }

   private static PyroSourceAnchors.Nozzles jetAnchorsFirstPerson(LivingEntity p, float partialTick, Vec3 eye) {
      float yaw = p.getViewYRot(partialTick);
      Vec3 right = AimUtil.horizontalRight((double)yaw);
      double c = (double)AwakenedAnimations.jetCruise(p);
      Vec3 flatBack = AimUtil.direction((double)yaw, 0.0).scale(-0.15);
      Vec3 hoverHand = eye.add(flatBack).add(0.0, -0.6, 0.0);
      Vec3 hoverFoot = eye.add(0.0, -1.2, 0.0);
      if (c <= 0.0) {
         return new PyroSourceAnchors.Nozzles(
            hoverHand.add(right.scale(-0.4)), hoverHand.add(right.scale(0.4)), hoverFoot.add(right.scale(-0.13)), hoverFoot.add(right.scale(0.13))
         );
      } else {
         Vec3 look = p.getViewVector(partialTick);
         Vec3 back = AwakenedAnimations.jetExhaustAim(p, look);
         Vec3 screenUp = right.cross(look);
         Vec3 cruiseHand = eye.add(back.scale(0.55)).add(screenUp.scale(-0.15));
         Vec3 cruiseFoot = eye.add(back.scale(1.15)).add(screenUp.scale(-0.25));
         double handOut = Mth.lerp(c, 0.4, 0.28);
         double footOut = Mth.lerp(c, 0.13, 0.12);
         Vec3 hand = hoverHand.lerp(cruiseHand, c);
         Vec3 foot = hoverFoot.lerp(cruiseFoot, c);
         return new PyroSourceAnchors.Nozzles(
            hand.add(right.scale(-handOut)), hand.add(right.scale(handOut)), foot.add(right.scale(-footOut)), foot.add(right.scale(footOut))
         );
      }
   }

   public static Vec3 beamEnd(Level level, LivingEntity p, Vec3 eye, Vec3 aimDir) {
      Vec3 reach = eye.add(aimDir.scale(20.0));
      BlockHitResult blocked = level.clip(new ClipContext(eye, reach, Block.COLLIDER, Fluid.NONE, p));
      return blocked.getType() == Type.BLOCK ? blocked.getLocation() : reach;
   }

   public static Vec3 fireballFpAnchor(Player p, float partialTick, boolean leftArm) {
      return AimUtil.shoulderHand(
         p.getEyePosition(partialTick),
         (double)p.getViewYRot(partialTick),
         (double)AwakenedAnimations.firstPersonArmPitch(p.getViewXRot(partialTick)),
         leftArm ? -1.0 : 1.0,
         0.34,
         0.46,
         0.6
      );
   }

   public static record Nozzles(Vec3 left, Vec3 right, Vec3 leftFoot, Vec3 rightFoot) {
      public Nozzles(Vec3 left, Vec3 right) {
         this(left, right, null, null);
      }

      public boolean feet() {
         return this.leftFoot != null;
      }

      public int count() {
         return this.feet() ? 4 : 2;
      }

      public Vec3 point(int slot) {
         return switch (slot) {
            case 0 -> this.left;
            case 1 -> this.right;
            case 2 -> this.leftFoot;
            default -> this.rightFoot;
         };
      }
   }
}
