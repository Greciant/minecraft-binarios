package com.awakenedmod.awakened.content.brawler;

import com.awakenedmod.awakened.core.power.Ability;
import net.minecraft.server.level.ServerPlayer;

abstract class BrawlerAbility extends Ability {
   protected BrawlerAbility(String id, float staminaCost, int unlockLevel) {
      super(id, staminaCost, unlockLevel);
   }

   @Override
   public final boolean activate(ServerPlayer player) {
      boolean happened = this.press(player);
      if (happened && this.endsGuard()) {
         BrawlerGuard.end(player);
      }

      return happened;
   }

   protected boolean endsGuard() {
      return true;
   }

   protected abstract boolean press(ServerPlayer var1);

   protected final void whiff(ServerPlayer player) {
      this.whiff(player, this.effectiveCost(player));
   }

   protected final void whiff(ServerPlayer player, float paid) {
      BrawlerFx.whiff(player.serverLevel(), player);
      BrawlerStamina.give(player, paid * 0.65F);
   }
}
