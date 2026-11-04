package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.client.render.PyroFlameLayer;
import net.minecraft.world.entity.Entity;

public final class PyroBackdraftClock {
   private static final ClipClock CLOCK = new ClipClock(PyroFlameLayer.BACKDRAFT);

   private PyroBackdraftClock() {
   }

   public static float progress(Entity entity) {
      long age = CLOCK.age(entity);
      return age < 0L ? -1.0F : (float)age / 43.0F;
   }
}
