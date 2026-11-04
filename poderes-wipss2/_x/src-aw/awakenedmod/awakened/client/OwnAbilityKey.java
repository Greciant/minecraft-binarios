package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.power.Power;
import com.awakenedmod.awakened.core.power.PowerRegistry;

public final class OwnAbilityKey {
   private OwnAbilityKey() {
   }

   public static int slot(String id) {
      ClientPowerState state = ClientPowerState.get();
      if (!state.awakened()) {
         return -1;
      } else {
         Power power = PowerRegistry.get(state.powerId());
         if (power == null) {
            return -1;
         } else {
            for (int slot = 0; slot < power.abilityCount(); slot++) {
               Ability a = power.ability(slot);
               if (a != null && a.id().equals(id)) {
                  return state.masteryLevel() >= a.unlockLevel() ? slot : -1;
               }
            }

            return -1;
         }
      }
   }

   public static boolean held(String id) {
      int slot = slot(id);
      return slot >= 0 && AbilityKeyInput.heldFor(slot);
   }
}
