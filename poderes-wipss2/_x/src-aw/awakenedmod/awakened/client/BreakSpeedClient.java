package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.core.PowerSuppression;
import com.awakenedmod.awakened.core.power.Power;
import com.awakenedmod.awakened.core.power.PowerRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.BreakSpeed;

public final class BreakSpeedClient {
   private BreakSpeedClient() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(BreakSpeedClient::onBreakSpeed);
   }

   private static void onBreakSpeed(BreakSpeed event) {
      Player player = event.getEntity();
      if (player != null && player == Minecraft.getInstance().player) {
         ClientPowerState state = ClientPowerState.get();
         if (state.awakened()) {
            Power power = PowerRegistry.get(state.powerId());
            if (power != null && !PowerSuppression.isSuppressed(player)) {
               float modified = power.modifyBreakSpeed(player, event.getNewSpeed());
               if (modified != event.getNewSpeed()) {
                  event.setNewSpeed(modified);
               }
            }
         }
      }
   }
}
