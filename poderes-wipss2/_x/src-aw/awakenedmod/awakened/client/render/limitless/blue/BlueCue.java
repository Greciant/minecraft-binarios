package com.awakenedmod.awakened.client.render.limitless.blue;

import com.awakenedmod.awakened.content.limitless.blue.BlueEnd;
import net.minecraft.world.phys.Vec3;

public interface BlueCue {
   Vec3 orb();

   long seed();

   default float release() {
      return 40.0F;
   }

   default BlueEnd ending() {
      return BlueEnd.FADE;
   }
}
