package com.awakenedmod.awakened.client.render.limitless.rct;

import net.minecraft.world.phys.Vec3;

public interface RctCue {
   Vec3 feet();

   float height();

   float presence();

   boolean ownView();

   long seed();
}
