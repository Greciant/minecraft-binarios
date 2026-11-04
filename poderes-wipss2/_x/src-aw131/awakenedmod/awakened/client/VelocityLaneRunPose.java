package com.awakenedmod.awakened.client;

import com.zigythebird.playeranimcore.animation.layered.modifier.AdjustmentModifier.PartModifier;
import com.zigythebird.playeranimcore.math.Vec3f;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

public final class VelocityLaneRunPose {
   private static final float ARM_RAISE_DEG = 55.0F;
   private static final float ARM_SWING_DEG = 40.0F;
   private static final float ARM_TUCK_DEG = 6.0F;

   private VelocityLaneRunPose() {
   }

   public static Optional<PartModifier> bone(String bone, Entity avatar, boolean firstPerson, float partialTick) {
      if (VelocityLaneTransitClient.active() && avatar == Minecraft.getInstance().player) {
         float ramp = VelocityLaneTransitClient.ramp(partialTick);
         float swing = 40.0F * Mth.sin(VelocityLaneTransitClient.cadence(partialTick)) * ramp;
         float raise = 55.0F * ramp;

         return switch (bone) {
            case "right_arm" -> Optional.of(new PartModifier(new Vec3f(rad(-(raise + swing)), 0.0F, rad(-6.0F * ramp)), Vec3f.ZERO));
            case "left_arm" -> Optional.of(new PartModifier(new Vec3f(rad(-(raise - swing)), 0.0F, rad(6.0F * ramp)), Vec3f.ZERO));
            default -> Optional.empty();
         };
      } else {
         return Optional.empty();
      }
   }

   private static float rad(float deg) {
      return (float)Math.toRadians((double)deg);
   }
}
