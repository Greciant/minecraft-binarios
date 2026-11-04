package com.devilfruits.fruits.gura_gura;

import com.devilfruits.fruits.gura_gura.ability.KabutowariAbility;
import com.devilfruits.fruits.gura_gura.ability.QuakeAuraAbility;
import com.devilfruits.fruits.gura_gura.ability.QuakeInfusionAbility;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem;

final class GuraEvents {
   static void register(IEventBus gameBus) {
      gameBus.addListener(GuraEvents::onIncomingDamage);
      gameBus.addListener(GuraEvents::onFall);
      gameBus.addListener(GuraEvents::onRightClickItem);
   }

   private static void onIncomingDamage(LivingIncomingDamageEvent event) {
      if (!event.getEntity().level().isClientSide()) {
         QuakeAuraAbility.onIncomingDamage(event);
      }
   }

   private static void onFall(LivingFallEvent event) {
      KabutowariAbility.onFall(event);
   }

   private static void onRightClickItem(RightClickItem event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         QuakeInfusionAbility.onRightClickItem(player, event);
      }
   }

   private GuraEvents() {
   }
}
