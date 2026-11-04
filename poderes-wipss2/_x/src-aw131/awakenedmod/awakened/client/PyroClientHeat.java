package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.content.pyrokinetic.FlameStyle;
import com.awakenedmod.awakened.content.pyrokinetic.PyroHeatState;
import net.minecraft.world.entity.Entity;

public final class PyroClientHeat {
   private PyroClientHeat() {
   }

   public static float heat(Entity player) {
      return PyroHeatState.visualHeat(player);
   }

   public static FlameStyle style(Entity player) {
      return FlameStyle.forHeat(heat(player));
   }

   public static float blueFraction(Entity player) {
      return FlameStyle.blueFraction(heat(player));
   }
}
