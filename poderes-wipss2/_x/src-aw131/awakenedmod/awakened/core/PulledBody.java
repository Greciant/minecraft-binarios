package com.awakenedmod.awakened.core;

import net.minecraft.world.entity.player.Player;

public final class PulledBody {
   private PulledBody() {
   }

   public static boolean carried(Player player) {
      return player.isPassenger() || player.isFallFlying() || player.getAbilities().flying;
   }

   public static boolean mustLetGo(Player player) {
      return carried(player) || player.isInLava() || player.isSpectator();
   }
}
