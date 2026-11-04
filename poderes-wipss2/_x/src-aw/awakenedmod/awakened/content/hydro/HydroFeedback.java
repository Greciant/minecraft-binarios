package com.awakenedmod.awakened.content.hydro;

import com.awakenedmod.awakened.core.power.AbilityFeedback;
import net.minecraft.server.level.ServerPlayer;

final class HydroFeedback {
   private HydroFeedback() {
   }

   static void tired(ServerPlayer player) {
      AbilityFeedback.fail(player, "awakened.hydrokinetic.tired");
   }

   static void aim(ServerPlayer player) {
      AbilityFeedback.fail(player, "awakened.hydrokinetic.geyser_aim");
   }
}
