package com.awakenedmod.awakened.content.hydro;

import com.awakenedmod.awakened.core.Fx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public final class WaveRiderFx {
   private static final double WAKE_MAX_GAP = 2.5;
   private static final int RING_EVERY = 6;

   private WaveRiderFx() {
   }

   static void wake(ServerLevel level, Vec3 feet, double surfaceY, Vec3 fwd, float rush, int tick, boolean wet) {
      boolean near = feet.y - surfaceY <= 2.5;
      Vec3 behind = new Vec3(feet.x - fwd.x * 0.9, surfaceY, feet.z - fwd.z * 0.9);
      int spray = 1 + Math.round(4.0F * rush);
      Vec3 lip = feet.add(fwd.scale(0.7)).add(0.0, -0.3, 0.0);
      Fx.burst(level, ParticleTypes.SPLASH, lip, spray, 0.35, 0.05 + 0.1 * (double)rush);
      if (near) {
         Fx.burst(level, ParticleTypes.SPLASH, behind.add(0.0, 0.2, 0.0), spray, 0.5, 0.08);
      } else {
         Fx.burst(level, ParticleTypes.FALLING_WATER, feet.subtract(fwd.scale(0.9)).add(0.0, -0.4, 0.0), 3, 0.5, 0.0);
         if (tick % 6 == 0) {
            HydroFx.impactRing(level, behind, new Vec3(0.0, 1.0, 0.0), 1.2 + 0.6 * (double)rush, wet);
         }
      }
   }
}
