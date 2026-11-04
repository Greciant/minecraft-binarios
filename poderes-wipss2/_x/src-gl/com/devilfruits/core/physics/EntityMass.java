package com.devilfruits.core.physics;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.world.entity.Entity;

public final class EntityMass {
   public static final double DEFAULT_KG = 70.0;
   private static final List<EntityMassProvider> PROVIDERS = new CopyOnWriteArrayList<>();

   public static void register(EntityMassProvider provider) {
      PROVIDERS.add(provider);
   }

   public static double provided(Entity entity) {
      for (EntityMassProvider p : PROVIDERS) {
         double kg = p.massKg(entity);
         if (!Double.isNaN(kg)) {
            return kg;
         }
      }

      return Double.NaN;
   }

   public static double kg(Entity entity) {
      double kg = provided(entity);
      return Double.isNaN(kg) ? 70.0 * hitboxRatio(entity) : kg;
   }

   public static boolean immovable(Entity entity) {
      for (EntityMassProvider p : PROVIDERS) {
         if (p.immovable(entity)) {
            return true;
         }
      }

      return false;
   }

   public static double hitboxRatio(Entity entity) {
      double volume = (double)(entity.getBbWidth() * entity.getBbWidth() * entity.getBbHeight());
      return Math.max(0.5, Math.min(40.0, volume / 0.648));
   }

   private EntityMass() {
   }
}
