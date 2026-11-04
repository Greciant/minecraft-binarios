package com.awakenedmod.awakened.client;

import net.minecraft.client.particle.Particle;

public final class FrozenAir {
   private FrozenAir() {
   }

   public static boolean beginTick(Particle particle) {
      FrozenAirParticle slow = (FrozenAirParticle)particle;
      if (!TimelessFieldClient.active()) {
         return slow.awakened$beginTick(1);
      } else {
         int divisor = TimelessFieldClient.divisorAt(slow.awakened$trueX(), slow.awakened$trueY(), slow.awakened$trueZ());
         return slow.awakened$beginTick(divisor);
      }
   }

   public static void endTick(Particle particle) {
      ((FrozenAirParticle)particle).awakened$endTick();
   }
}
