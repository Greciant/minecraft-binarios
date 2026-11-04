package com.awakenedmod.awakened.content.brawler;

import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import net.minecraft.server.level.ServerPlayer;

final class BrawlerStamina {
   private BrawlerStamina() {
   }

   static void give(ServerPlayer player, float amount) {
      if (!(amount <= 0.0F)) {
         PowerData data = PowerService.data(player);
         float max = StaminaSystem.max(player);
         if (!(data.stamina() >= max)) {
            data.setStamina(Math.min(max, data.stamina() + amount));
            PowerService.sync(player);
         }
      }
   }

   static boolean take(ServerPlayer player, float amount) {
      PowerData data = PowerService.data(player);
      if (!StaminaSystem.canAfford(data, amount)) {
         return false;
      } else {
         if (!data.infiniteStamina()) {
            data.setStamina(Math.max(0.0F, data.stamina() - amount));
         }

         return true;
      }
   }
}
