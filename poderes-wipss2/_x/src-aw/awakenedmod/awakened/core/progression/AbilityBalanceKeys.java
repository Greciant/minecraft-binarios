package com.awakenedmod.awakened.core.progression;

import com.awakenedmod.awakened.Awakened;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.power.Power;
import com.awakenedmod.awakened.core.power.PowerRegistry;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

public final class AbilityBalanceKeys {
   private AbilityBalanceKeys() {
   }

   public static void logAll() {
      StringBuilder sb = new StringBuilder("Ability balance-override keys (abilities.damageOverrides, abilities.staminaOverrides):\n");
      keys().forEach((key, powerId) -> sb.append("  ").append(key).append("  (").append(powerId).append(")\n"));
      Awakened.LOGGER.info(sb.toString());
   }

   public static Map<String, String> keys() {
      TreeMap<String, String> keyToPower = new TreeMap<>();

      for (Power power : PowerRegistry.all()) {
         for (Ability ability : power.abilities()) {
            keyToPower.putIfAbsent(ability.balanceKey(), power.id());
         }

         for (String key : power.extraBalanceKeys()) {
            keyToPower.putIfAbsent(key, power.id());
         }
      }

      for (Item item : BuiltInRegistries.ITEM) {
         if (item instanceof BalanceKeyed) {
            BalanceKeyed keyed = (BalanceKeyed)item;
            String itemId = BuiltInRegistries.ITEM.getKey(item).getPath();

            for (String key : keyed.balanceKeys()) {
               keyToPower.putIfAbsent(key, itemId);
            }
         }
      }

      return keyToPower;
   }
}
