package com.awakenedmod.awakened.client.render.limitless.red;

import javax.annotation.Nullable;
import net.minecraft.world.phys.Vec3;

public interface RedCue {
   Vec3 fireFrom();

   Vec3 fwd();

   @Nullable
   Vec3 burst();

   int hitTick();

   double floorY();

   long seed();

   default Vec3 burstPoint() {
      Vec3 b = this.burst();
      return b != null ? b : this.fireFrom().add(this.fwd().scale(16.0));
   }

   default int burstTick() {
      return this.burst() != null ? this.hitTick() : 14 + (int)Math.ceil(8.88888888888889);
   }
}
