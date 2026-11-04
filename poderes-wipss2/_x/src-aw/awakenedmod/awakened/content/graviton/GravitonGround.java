package com.awakenedmod.awakened.content.graviton;

import com.awakenedmod.awakened.core.Fx;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

final class GravitonGround {
   private GravitonGround() {
   }

   static Fx.Floor floor(ServerLevel level, Vec3 feet) {
      return Fx.floorUnder(level, feet.x, feet.y - 0.05, feet.z, 3);
   }

   static void floorKick(ServerLevel level, Vec3 feet, int count) {
      Fx.Floor floor = floor(level, feet);
      if (floor != null) {
         Fx.debrisFan(
            level, new BlockParticleOption(ParticleTypes.BLOCK, floor.state()), new Vec3(feet.x, floor.topY() + 0.1, feet.z), count, 10.0, 35.0, 0.12, 0.28
         );
      }
   }
}
