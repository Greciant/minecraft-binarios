package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.core.AwId;
import com.zigythebird.playeranimcore.animation.layered.modifier.AdjustmentModifier.PartModifier;
import com.zigythebird.playeranimcore.math.Vec3f;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.IEventBus;

public final class NukeChargeShake {
   private static final ResourceLocation CLIP = AwId.of("nuke");
   private static final ClipClock CLOCK = new ClipClock(CLIP);
   private static final float WINDUP_TICKS = 50.0F;
   private static final float BONE_SHAKE = 0.9F;
   private static final float FIRST_PERSON_SHARE = 0.55F;

   private NukeChargeShake() {
   }

   public static void register(IEventBus gameBus) {
      AwakenedAnimations.addPoseModifier(NukeChargeShake::bone);
   }

   private static float strength(Entity body, float partialTick) {
      long age = CLOCK.age(body);
      if (age < 0L) {
         return 0.0F;
      } else {
         float f = Mth.clamp(((float)age + partialTick) / 50.0F, 0.0F, 1.0F);
         return f * f;
      }
   }

   private static float jitter(float t, float phase) {
      return 0.6F * Mth.sin(t * 2.7F + phase) + 0.4F * Mth.sin(t * 6.1F + phase * 1.7F);
   }

   private static Optional<PartModifier> bone(String bone, Entity avatar, boolean firstPerson, float partialTick) {
      if (!"right_item".equals(bone)) {
         return Optional.empty();
      } else {
         float s = strength(avatar, partialTick);
         if (s <= 0.0F) {
            return Optional.empty();
         } else {
            float t = (float)avatar.tickCount + partialTick;
            float a = 0.9F * s * (firstPerson ? 0.55F : 1.0F);
            return Optional.of(new PartModifier(Vec3f.ZERO, new Vec3f(a * jitter(t, 0.0F), a * jitter(t, 2.1F), a * jitter(t, 4.3F))));
         }
      }
   }
}
