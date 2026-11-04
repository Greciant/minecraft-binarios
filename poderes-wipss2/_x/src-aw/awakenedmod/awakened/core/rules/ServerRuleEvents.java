package com.awakenedmod.awakened.core.rules;

import com.awakenedmod.awakened.core.NearDeathAwaken;
import net.neoforged.bus.api.IEventBus;

public final class ServerRuleEvents {
   private ServerRuleEvents() {
   }

   public static void register(IEventBus gameBus) {
      PowerAvailability.register();
      AwakeningRules.register(gameBus);
      DeathRules.register(gameBus);
      PvpRules.register(gameBus);
      gameBus.addListener(e -> NearDeathAwaken.migrateWorldFlag(e.getServer()));
   }
}
