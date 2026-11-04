package com.awakenedmod.awakened.client;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

public final class PyroJetClock {
   private static final ClipClock CLOCK = new ClipClock(AwakenedAnimations::isJetting);

   private PyroJetClock() {
   }

   public static boolean lit(Entity entity) {
      return CLOCK.age(entity) >= 0L;
   }

   public static float throttle(Entity entity) {
      long age = CLOCK.age(entity);
      return age < 0L ? 0.0F : Mth.clamp((float)(age + 1L) / 15.0F, 0.0F, 1.0F);
   }
}
