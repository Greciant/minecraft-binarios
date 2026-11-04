package com.awakenedmod.awakened.core.progression;

import com.awakenedmod.awakened.core.ConfigOverrides;
import java.util.stream.Stream;

public final class AbilityBalance {
   private static final ConfigOverrides<Float> DAMAGE = new ConfigOverrides<>(
      "abilities.damageOverrides", BalanceConfig::damageOverrides, AbilityBalance::multiplier, AbilityBalance::keys
   );
   private static final ConfigOverrides<Float> STAMINA = new ConfigOverrides<>(
      "abilities.staminaOverrides", BalanceConfig::staminaOverrides, AbilityBalance::multiplier, AbilityBalance::keys
   );

   private AbilityBalance() {
   }

   private static Float multiplier(String text) {
      float value = Float.parseFloat(text);
      return value >= 0.0F && Float.isFinite(value) ? value : null;
   }

   private static Stream<String> keys() {
      return AbilityBalanceKeys.keys().keySet().stream();
   }

   public static float damageMultiplier(String abilityKey) {
      return DAMAGE.get(abilityKey, 1.0F);
   }

   public static float staminaMultiplier(String abilityKey) {
      return STAMINA.get(abilityKey, 1.0F);
   }
}
