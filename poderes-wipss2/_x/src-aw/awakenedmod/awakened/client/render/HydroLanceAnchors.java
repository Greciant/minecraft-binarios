package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.hydro.HydroVisual;
import com.awakenedmod.awakened.core.AimUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class HydroLanceAnchors {
   private static final float LIFT_START = 60.0F;
   private static final float LIFT_FULL = 75.0F;
   private static final float TWIST_START = 12.0F;
   private static final float TWIST_FULL = 25.0F;
   private static final float RAMP_TICKS = 3.0F;

   private HydroLanceAnchors() {
   }

   static long chargeAge(Player p) {
      return HydroVisual.of(p).lanceAge(p.level().getGameTime());
   }

   static boolean wet(Player p) {
      return HydroVisual.of(p).lanceWet();
   }

   static float armLift(float age) {
      return keyed(age, 60.0F, 75.0F);
   }

   static float twist(float age) {
      return keyed(age, 12.0F, 25.0F);
   }

   private static float keyed(float age, float start, float full) {
      return age < 3.0F ? start * (age / 3.0F) : Mth.lerp(drawn(age), start, full);
   }

   private static float drawn(float age) {
      return Mth.clamp((age - 3.0F) / 37.0F, 0.0F, 1.0F);
   }

   static Vec3 hand(Player p, float partialTick, float age, boolean leftArm) {
      double side = leftArm ? -1.0 : 1.0;
      float yaw = Mth.rotLerp(partialTick, p.yBodyRotO, p.yBodyRot);
      return AimUtil.shoulderHand(p.getEyePosition(partialTick), (double)yaw - side * (double)twist(age), (double)(-armLift(age)), side, 0.31, 0.25, 0.7);
   }

   static Vec3 tip(Player p, float partialTick, float age, boolean leftArm, float length) {
      if (HydroHands.ownFirstPerson(p)) {
         return HydroLanceFirstPerson.worldTip(p, partialTick, age, leftArm, length);
      } else {
         Vec3 look = p.getViewVector(partialTick);
         return hand(p, partialTick, age, leftArm).add(look.scale((double)(length - 0.32F)));
      }
   }
}
