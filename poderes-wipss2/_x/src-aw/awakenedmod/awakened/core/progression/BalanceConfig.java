package com.awakenedmod.awakened.core.progression;

import com.awakenedmod.awakened.Config;
import com.awakenedmod.awakened.core.AwakenedConfigs;
import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;

public final class BalanceConfig {
   private static final Builder BUILDER = new Builder();
   private static final double DEFAULT_STAMINA_MAX = 100.0;
   private static final double DEFAULT_REGEN_PER_SECOND = 11.0;
   private static final double DEFAULT_REGEN_DELAY_SECONDS = 1.5;
   private static final double DEFAULT_MULTIPLIER = 1.0;
   private static final boolean DEFAULT_ADDITIVE_STACKING = true;
   public static final DoubleValue STAMINA_MAX = BUILDER.comment("Base maximum stamina.").defineInRange("stamina.max", 100.0, 1.0, 10000.0);
   public static final DoubleValue STAMINA_REGEN_PER_SECOND = BUILDER.comment("Base stamina regained per second.")
      .defineInRange("stamina.regenPerSecond", 11.0, 0.0, 10000.0);
   public static final DoubleValue STAMINA_REGEN_DELAY_SECONDS = BUILDER.comment("Seconds after using stamina before it starts to come back.")
      .defineInRange("stamina.regenDelaySeconds", 1.5, 0.0, 60.0);
   public static final DoubleValue ABILITY_DAMAGE_MULTIPLIER = BUILDER.comment("Multiplier on ability damage, range, radius, speed and duration.")
      .defineInRange("abilities.damageMultiplier", 1.0, 0.0, 100.0);
   public static final DoubleValue ABILITY_STAMINA_MULTIPLIER = BUILDER.comment("Multiplier on ability stamina costs.")
      .defineInRange("abilities.staminaMultiplier", 1.0, 0.0, 100.0);
   public static final BooleanValue ADDITIVE_DAMAGE_STACKING = BUILDER.comment("true = damage bonuses add up. false = they multiply.")
      .define("abilities.additiveDamageStacking", true);
   public static final ConfigValue<List<? extends String>> ABILITY_DAMAGE_OVERRIDES = BUILDER.comment(
         "Damage multipliers for single abilities, e.g. \"WebShotAbility=1.5\". The startup log lists every ability key."
      )
      .defineListAllowEmpty("abilities.damageOverrides", List.of(), () -> "abilityKey=1.0", o -> {
         if (o instanceof String s && s.contains("=")) {
            return true;
         }

         return false;
      });
   public static final ConfigValue<List<? extends String>> ABILITY_STAMINA_OVERRIDES = BUILDER.comment(
         "Stamina cost multipliers for single abilities, in the same format."
      )
      .defineListAllowEmpty("abilities.staminaOverrides", List.of(), () -> "abilityKey=1.0", o -> {
         if (o instanceof String s && s.contains("=")) {
            return true;
         }

         return false;
      });
   public static final ModConfigSpec SPEC = BUILDER.build();

   private BalanceConfig() {
   }

   public static void register() {
      AwakenedConfigs.moved(STAMINA_MAX, Config.OLD_STAMINA_MAX);
      AwakenedConfigs.moved(STAMINA_REGEN_PER_SECOND, Config.OLD_STAMINA_REGEN_PER_SECOND);
      AwakenedConfigs.moved(STAMINA_REGEN_DELAY_SECONDS, Config.OLD_STAMINA_REGEN_DELAY_SECONDS);
      AwakenedConfigs.moved(ABILITY_DAMAGE_MULTIPLIER, Config.OLD_ABILITY_DAMAGE_MULTIPLIER);
      AwakenedConfigs.moved(ABILITY_STAMINA_MULTIPLIER, Config.OLD_ABILITY_STAMINA_MULTIPLIER);
      AwakenedConfigs.moved(ADDITIVE_DAMAGE_STACKING, Config.OLD_ADDITIVE_DAMAGE_STACKING);
      AwakenedConfigs.moved(ABILITY_DAMAGE_OVERRIDES, Config.OLD_ABILITY_DAMAGE_OVERRIDES);
      AwakenedConfigs.moved(ABILITY_STAMINA_OVERRIDES, Config.OLD_ABILITY_STAMINA_OVERRIDES);
   }

   public static double staminaMax() {
      return orDefault(STAMINA_MAX, 100.0);
   }

   public static double regenPerSecond() {
      return orDefault(STAMINA_REGEN_PER_SECOND, 11.0);
   }

   public static double regenDelaySeconds() {
      return orDefault(STAMINA_REGEN_DELAY_SECONDS, 1.5);
   }

   public static float damageMultiplier() {
      return (float)orDefault(ABILITY_DAMAGE_MULTIPLIER, 1.0).doubleValue();
   }

   public static float staminaMultiplier() {
      return (float)orDefault(ABILITY_STAMINA_MULTIPLIER, 1.0).doubleValue();
   }

   public static boolean additiveDamageStacking() {
      return orDefault(ADDITIVE_DAMAGE_STACKING, true);
   }

   public static List<? extends String> damageOverrides() {
      return SPEC.isLoaded() ? (List)ABILITY_DAMAGE_OVERRIDES.get() : List.of();
   }

   public static List<? extends String> staminaOverrides() {
      return SPEC.isLoaded() ? (List)ABILITY_STAMINA_OVERRIDES.get() : List.of();
   }

   private static <T> T orDefault(ConfigValue<T> value, T fallback) {
      return (T)(SPEC.isLoaded() ? value.get() : fallback);
   }
}
