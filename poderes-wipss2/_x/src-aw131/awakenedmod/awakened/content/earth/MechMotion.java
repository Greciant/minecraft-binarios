package com.awakenedmod.awakened.content.earth;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

public final class MechMotion {
   private static final double SPEED_MULT = 1.0;
   private static final double GRAVITY = 0.09;
   private static final double DRAG = 0.98;
   private static final double JUMP_VELOCITY = 0.42;
   private static final double LEAP_VELOCITY = 0.72;
   private static final double LEAP_FORWARD = 0.55;
   public static final int LEAP_COOLDOWN = 50;

   private MechMotion() {
   }

   public static void step(StoneMechEntity mech, float yaw, float pitch, float forward, float strafe, boolean jump, boolean sprint, boolean leap) {
      mech.setYRot(yaw);
      mech.setYBodyRot(yaw);
      mech.setYHeadRot(yaw);
      mech.setXRot(pitch);
      float yawRad = yaw * (float) (Math.PI / 180.0);
      double sin = (double)Mth.sin(yawRad);
      double cos = (double)Mth.cos(yawRad);
      Vec3 wish = new Vec3((double)strafe * cos - (double)forward * sin, 0.0, (double)forward * cos + (double)strafe * sin);
      if (wish.lengthSqr() > 1.0E-4) {
         wish = wish.normalize();
      }

      mech.setSprinting(sprint && forward > 0.0F);
      double speed = mech.getAttributeValue(Attributes.MOVEMENT_SPEED) * 1.0;
      Vec3 horizontal = wish.scale(speed);
      boolean onGround = mech.onGround();
      double vy = mech.getDeltaMovement().y;
      if (onGround) {
         if (leap) {
            vy = 0.72;
            horizontal = horizontal.add(-sin * 0.55, 0.0, cos * 0.55);
         } else {
            vy = jump ? 0.42 : 0.0;
         }
      } else {
         vy = (vy - 0.09) * 0.98;
      }

      mech.setDeltaMovement(horizontal.x, vy, horizontal.z);
      mech.move(MoverType.SELF, mech.getDeltaMovement());
      if (mech.level().isClientSide()) {
         mech.calculateEntityAnimation(false);
      }
   }
}
