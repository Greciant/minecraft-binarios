package com.awakenedmod.awakened.client.graviton.dense;

import com.awakenedmod.awakened.client.AwakenedAnimations;
import com.awakenedmod.awakened.core.AwId;
import com.zigythebird.playeranimcore.animation.layered.modifier.AdjustmentModifier.PartModifier;
import com.zigythebird.playeranimcore.math.Vec3f;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class DenseWalkPose {
   private static final float VANILLA_RATE = 0.6662F;
   private static final float VANILLA_SWING = 1.4F;
   private static final float STEP_SHARE = 0.7F;
   private static final ResourceLocation CLIP = AwId.of("dense");

   private DenseWalkPose() {
   }

   public static void register() {
      AwakenedAnimations.addPoseModifier(DenseWalkPose::bone);
   }

   private static Optional<PartModifier> bone(String bone, Entity avatar, boolean firstPerson, float partialTick) {
      boolean right = bone.equals("right_leg");
      if (!right && !bone.equals("left_leg")) {
         return Optional.empty();
      } else {
         if (avatar instanceof LivingEntity living && AwakenedAnimations.isTriggered(living.getId(), CLIP)) {
            float speed = Math.min(1.0F, living.walkAnimation.speed(partialTick));
            if (speed < 0.001F) {
               return Optional.empty();
            }

            float swing = Mth.cos(living.walkAnimation.position(partialTick) * 0.6662F) * 1.4F * 0.7F * speed;
            return Optional.of(new PartModifier(new Vec3f(right ? swing : -swing, 0.0F, 0.0F), Vec3f.ZERO));
         }

         return Optional.empty();
      }
   }
}
