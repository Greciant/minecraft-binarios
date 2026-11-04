package com.awakenedmod.awakened.content.limitless;

import com.awakenedmod.awakened.core.AimUtil;
import net.minecraft.world.phys.Vec3;

public final class LimitlessHands {
   private LimitlessHands() {
   }

   public static Vec3 redTip(Vec3 eye, double yaw, double armPitch, boolean firstPerson) {
      return AimUtil.shoulderHand(eye, yaw, armPitch, 1.0, firstPerson ? 0.37 : 0.31, firstPerson ? 0.37 : 0.25, 0.78);
   }

   public static Vec3 purpleHand(Vec3 eye, double yaw, double armPitch, int side, boolean firstPerson) {
      return AimUtil.shoulderHand(eye, yaw, armPitch, (double)side, firstPerson ? 0.37 : 0.31, firstPerson ? 0.37 : 0.25, 0.7);
   }

   public static Vec3 purpleMerge(Vec3 eye, double yaw, double pitch) {
      return eye.add(AimUtil.direction(yaw, pitch).scale(1.3)).subtract(0.0, 0.27, 0.0);
   }
}
