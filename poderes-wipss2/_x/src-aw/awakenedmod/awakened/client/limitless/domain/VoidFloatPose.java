package com.awakenedmod.awakened.client.limitless.domain;

import com.awakenedmod.awakened.client.AwakenedAnimations;
import com.awakenedmod.awakened.core.AwId;
import com.zigythebird.playeranimcore.animation.layered.modifier.AdjustmentModifier.PartModifier;
import com.zigythebird.playeranimcore.math.Vec3f;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

final class VoidFloatPose {
   private static final ResourceLocation FLOAT = AwId.of("limitless_void_float");
   private static final float WALK_RATE = 0.6662F;
   private static final float FULL_SPEED = 0.35F;
   private static final float EASE = 0.15F;
   private static final float LEAN = 12.0F;
   private static final float BANK = 6.0F;
   private static final float ARM_TRAIL = 14.0F;
   private static final float LEG_TRAIL = 10.0F;
   private static final float LEG_SWAY = 6.0F;
   private static final float ARM_OUT = 7.0F;
   private static final float ARM_SWAY = 5.0F;
   private static final float SWAY_RATE = 0.045F;
   private static final int STALE_TICKS = 2;
   private static final Map<Integer, VoidFloatPose.Drift> DRIFT = new HashMap<>();
   private static int clientTick;

   private VoidFloatPose() {
   }

   static void tick(Minecraft mc) {
      clientTick++;
      if (mc.level == null) {
         DRIFT.clear();
      } else {
         DRIFT.values().removeIf(d -> clientTick - d.seen > 2);
      }
   }

   static Optional<PartModifier> bone(String bone, Entity avatar, boolean firstPerson, float partialTick) {
      if (!firstPerson && avatar instanceof LivingEntity living && AwakenedAnimations.isTriggered(avatar.getId(), FLOAT)) {
         VoidFloatPose.Drift d = drift(living);
         float t = (float)living.tickCount + partialTick;

         return switch (bone) {
            case "body" -> rot(deg(12.0F * d.forward), 0.0F, -deg(6.0F * d.side));
            case "head" -> rot(-deg(12.0F * d.forward), 0.0F, 0.0F);
            case "right_arm" -> rot(
            unwalk(living, partialTick, true) + deg(14.0F * Math.max(0.0F, d.forward) + 5.0F * Mth.sin(t * 0.045F)),
            0.0F,
            deg(7.0F + 3.0F * Mth.sin(t * 0.045F * 0.8F + 1.0F))
         );
            case "left_arm" -> rot(
            unwalk(living, partialTick, false) + deg(14.0F * Math.max(0.0F, d.forward) + 5.0F * Mth.sin(t * 0.045F + 2.2F)),
            0.0F,
            -deg(7.0F + 3.0F * Mth.sin(t * 0.045F * 0.8F + 3.1F))
         );
            case "right_leg", "left_leg" -> rot(deg(10.0F * d.forward), 0.0F, -deg(6.0F * d.side));
            default -> Optional.empty();
         };
      }

      return Optional.empty();
   }

   private static float unwalk(LivingEntity living, float partialTick, boolean right) {
      float swing = Mth.cos(living.walkAnimation.position(partialTick) * 0.6662F) * Math.min(1.0F, living.walkAnimation.speed(partialTick));
      return right ? swing : -swing;
   }

   private static VoidFloatPose.Drift drift(LivingEntity living) {
      VoidFloatPose.Drift d = DRIFT.computeIfAbsent(living.getId(), k -> new VoidFloatPose.Drift());
      d.seen = clientTick;
      if (living.tickCount != d.tick) {
         d.tick = living.tickCount;
         float yaw = living.yBodyRot * (float) (Math.PI / 180.0);
         double dx = living.getX() - living.xo;
         double dz = living.getZ() - living.zo;
         float fwd = (float)(dx * (double)(-Mth.sin(yaw)) + dz * (double)Mth.cos(yaw));
         float side = (float)(dx * (double)(-Mth.cos(yaw)) + dz * (double)(-Mth.sin(yaw)));
         d.forward = d.forward + (Mth.clamp(fwd / 0.35F, -1.0F, 1.0F) - d.forward) * 0.15F;
         d.side = d.side + (Mth.clamp(side / 0.35F, -1.0F, 1.0F) - d.side) * 0.15F;
      }

      return d;
   }

   private static Optional<PartModifier> rot(float x, float y, float z) {
      return Optional.of(new PartModifier(new Vec3f(x, y, z), Vec3f.ZERO));
   }

   private static float deg(float degrees) {
      return degrees * (float) (Math.PI / 180.0);
   }

   private static final class Drift {
      float forward;
      float side;
      int tick = Integer.MIN_VALUE;
      int seen;
   }
}
