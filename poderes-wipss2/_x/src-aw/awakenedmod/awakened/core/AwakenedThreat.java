package com.awakenedmod.awakened.core;

import net.minecraft.server.level.ServerPlayer;

public interface AwakenedThreat {
   double bonusMastery();

   double bonusXp();

   default boolean isBoss() {
      return false;
   }

   default void onCreditedKill(ServerPlayer player) {
   }
}
