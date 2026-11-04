package com.awakenedmod.awakened.content.reaper;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;

public final class ReaperEvents {
   private static final float MARK_DAMAGE_MULT = 1.25F;

   private ReaperEvents() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(ReaperEvents::onIncoming);
      gameBus.addListener(ReaperEvents::onRespawn);
   }

   private static void onIncoming(LivingIncomingDamageEvent e) {
      if (DeathMarkControl.isMarked(e.getEntity())) {
         e.setAmount(e.getAmount() * 1.25F);
      }
   }

   private static void onRespawn(PlayerRespawnEvent e) {
      if (e.getEntity() instanceof ServerPlayer player) {
         ReaperSouls.armRespawnImmunity(player);
      }
   }
}
