package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

public final class PyroBeamClock {
   private static final ClipClock CLOCK = new ClipClock(AwId.of("flame_beam"));

   private PyroBeamClock() {
   }

   public static long age(Entity entity) {
      return CLOCK.age(entity);
   }

   public static float ramp(Entity entity) {
      long age = CLOCK.age(entity);
      return age < 0L ? 0.0F : Mth.clamp((float)(age + 1L) / 60.0F, 0.0F, 1.0F);
   }
}
