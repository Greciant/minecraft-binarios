package com.devilfruits.fruits.kilo_kilo.client.vfx;

import com.devilfruits.client.vfx.fx.VfxWorld;
import net.minecraft.world.level.Level;

public final class KiloLook {
   private static final int STONE_DUST = 9076851;

   public static int earth(Level level, double x, double groundY, double z) {
      int floor = VfxWorld.groundColour(level, x, groundY - 0.5, z);
      int r = ((floor >> 16 & 0xFF) * 45 + 7590) / 100;
      int g = ((floor >> 8 & 0xFF) * 45 + 7040) / 100;
      int b = ((floor & 0xFF) * 45 + 6325) / 100;
      return VfxWorld.shade(r << 16 | g << 8 | b, VfxWorld.light(level, x, groundY + 0.5, z));
   }

   private KiloLook() {
   }
}
