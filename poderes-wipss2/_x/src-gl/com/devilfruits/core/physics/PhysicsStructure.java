package com.devilfruits.core.physics;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public interface PhysicsStructure {
   UUID id();

   Level level();

   boolean isValid();

   double mass();

   Vec3 toWorld(Vec3 var1);

   Vec3 toLocal(Vec3 var1);

   Vec3 dirToWorld(Vec3 var1);

   Vec3 dirToLocal(Vec3 var1);

   Vec3 velocityAt(Vec3 var1);

   @Nullable
   AABB localBounds();

   AABB worldBounds();

   @Nullable
   Vec3 centerOfMassWorld();

   void applyImpulse(Vec3 var1, Vec3 var2);

   default void addVelocity(Vec3 linear, Vec3 spin) {
   }
}
