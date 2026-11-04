package com.awakenedmod.awakened.content.brawler;

import com.awakenedmod.awakened.core.progression.AbilityScaling;
import net.minecraft.server.level.ServerPlayer;

public final class FullCounterAbility extends BrawlerAbility {
   public FullCounterAbility() {
      super("brawler_full_counter", 16.0F, 10);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.NONE;
   }

   @Override
   protected boolean endsGuard() {
      return false;
   }

   @Override
   protected boolean press(ServerPlayer player) {
      BrawlerGuard.begin(player);
      return true;
   }

   @Override
   public String activationAnimation(ServerPlayer player) {
      return null;
   }

   @Override
   public boolean isActive(ServerPlayer player) {
      return BrawlerGuard.up(player);
   }

   @Override
   public void serverTick(ServerPlayer player) {
      BrawlerGuard.up(player);
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      BrawlerGuard.clearPlayer(player);
      super.clearPlayer(player);
   }
}
