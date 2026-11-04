package com.awakenedmod.awakened.core;

import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

public final class Headroom {
   private static final int SEARCH_STEPS = 4;
   public static final double NATURAL = 1.0;

   private Headroom() {
   }

   public static boolean fits(LivingEntity entity, double targetScale) {
      EntityDimensions current = entity.getDimensions(entity.getPose());
      float currentScale = Math.max(1.0E-4F, entity.getScale());
      EntityDimensions atTarget = current.scale((float)(targetScale / (double)currentScale));
      AABB box = atTarget.makeBoundingBox(entity.position()).deflate(1.0E-7);
      return entity.level().noBlockCollision(entity, box);
   }

   public static double standingCeiling(LivingEntity entity, double maxScale) {
      if (!(maxScale <= 1.0) && !fitsStanding(entity, maxScale)) {
         double lo = 1.0;
         double hi = maxScale;

         for (int i = 0; i < 4; i++) {
            double mid = (lo + hi) * 0.5;
            if (fitsStanding(entity, mid)) {
               lo = mid;
            } else {
               hi = mid;
            }
         }

         return lo;
      } else {
         return maxScale;
      }
   }

   private static boolean fitsStanding(LivingEntity entity, double targetScale) {
      EntityDimensions natural = entity.getType().getDimensions();
      AABB box = natural.scale((float)targetScale).makeBoundingBox(entity.position()).deflate(1.0E-7);
      return entity.level().noBlockCollision(entity, box);
   }
}
