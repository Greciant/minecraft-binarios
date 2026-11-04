package com.awakenedmod.awakened.client.render.limitless.purple;

import com.awakenedmod.awakened.content.limitless.purple.PurpleStrength;
import net.minecraft.world.phys.Vec3;

public interface PurpleCue {
   Vec3 left();

   Vec3 right();

   Vec3 merge();

   Vec3 fwd();

   long seed();

   default PurpleStrength strength() {
      return PurpleStrength.NORMAL;
   }

   default int gone() {
      return this.strength().gone();
   }

   default float presence() {
      return 1.0F;
   }

   default float overcharge() {
      return 0.0F;
   }

   default float pullIn() {
      return 0.0F;
   }

   default float motion(float age) {
      return age;
   }

   default float chargeAge(float age) {
      return age;
   }
}
