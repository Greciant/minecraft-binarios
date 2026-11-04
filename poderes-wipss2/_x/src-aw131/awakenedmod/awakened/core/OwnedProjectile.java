package com.awakenedmod.awakened.core;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public interface OwnedProjectile {
   Entity shooter();

   float shotDamage();

   default boolean sendBack(Vec3 direction, double speed, Entity newOwner) {
      return false;
   }
}
