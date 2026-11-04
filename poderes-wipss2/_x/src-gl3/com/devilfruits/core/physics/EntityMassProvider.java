package com.devilfruits.core.physics;

import net.minecraft.world.entity.Entity;

public interface EntityMassProvider {
   double massKg(Entity var1);

   default boolean immovable(Entity entity) {
      return false;
   }
}
