package com.awakenedmod.awakened.client.render.limitless.voidlook;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public interface VoidCue {
   Vec3 centre();

   float radius();

   Vec3 forward();

   long seed();

   Iterable<? extends Entity> caught();

   float crackAt();
}
