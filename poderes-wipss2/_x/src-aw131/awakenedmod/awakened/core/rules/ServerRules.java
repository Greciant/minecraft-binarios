package com.awakenedmod.awakened.core.rules;

import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

public final class ServerRules {
   private static final Builder BUILDER = new Builder();
   private static final boolean DEFAULT_NEAR_DEATH = true;
   private static final double DEFAULT_AWAKEN_CHANCE = 1.0;
   private static final double DEFAULT_RETRY_SECONDS = 30.0;
   private static final double DEFAULT_GRACE_MINUTES = 0.0;
   private static final boolean DEFAULT_FROM_PLAYER_DAMAGE = true;
   private static final boolean DEFAULT_START_AWAKENED = false;
   private static final boolean DEFAULT_LOSE_POWER = false;
   private static final double DEFAULT_MASTERY_LOSS = 0.0;
   private static final double DEFAULT_MASTERY_GAIN = 1.0;
   private static final int DEFAULT_MAX_MASTERY = 0;
   private static final double DEFAULT_PVP_DAMAGE = 1.0;
   private static final boolean DEFAULT_PVP_EFFECTS = true;
   private static final boolean DEFAULT_REALISTIC_MOVEMENT = false;
   private static final boolean DEFAULT_UNIQUE_POWERS = false;
   private static final double DEFAULT_STALE_AFTER_DAYS = 0.0;
   private static final int DEFAULT_DEFAULT_LIMIT = 1;
   private static final boolean DEFAULT_NOMINATION_REMINDER = true;
   public static final BooleanValue NEAR_DEATH = BUILDER.comment("Players awaken a power when they drop to low health.").define("awakening.nearDeath", true);
   public static final DoubleValue AWAKEN_CHANCE = BUILDER.comment("Chance that low health awakens a power.").defineInRange("awakening.chance", 1.0, 0.0, 1.0);
   public static final DoubleValue RETRY_SECONDS = BUILDER.comment("Seconds after a missed chance before the player can awaken again.")
      .defineInRange("awakening.retrySeconds", 30.0, 0.0, 86400.0);
   public static final DoubleValue GRACE_MINUTES = BUILDER.comment("Minutes a player must play before they can awaken.")
      .defineInRange("awakening.graceMinutes", 0.0, 0.0, 100000.0);
   public static final BooleanValue FROM_PLAYER_DAMAGE = BUILDER.comment("Hits from other players can awaken a player.")
      .define("awakening.fromPlayerDamage", true);
   public static final BooleanValue START_AWAKENED = BUILDER.comment("Players without a power get a random one.").define("awakening.startAwakened", false);
   public static final ConfigValue<List<? extends String>> DISABLED_POWERS = BUILDER.comment("Powers nobody can get or use, e.g. \"pyrokinetic\".")
      .defineListAllowEmpty("powers.disabled", List.of(), () -> "power_id", o -> o instanceof String);
   public static final ConfigValue<List<? extends String>> DISABLED_DIMENSIONS = BUILDER.comment(
         "Dimensions where powers don't work, e.g. \"minecraft:the_end\"."
      )
      .defineListAllowEmpty("powers.disabledDimensions", List.of(), () -> "minecraft:the_end", o -> o instanceof String);
   public static final BooleanValue UNIQUE_POWERS = BUILDER.comment("Awakenings and Catalysts prefer powers fewer players have.")
      .define("uniquePowers.enabled", false);
   public static final DoubleValue STALE_AFTER_DAYS = BUILDER.comment("Days offline before a player loses their power so others can get it. 0 = never.")
      .defineInRange("uniquePowers.staleAfterDays", 0.0, 0.0, 3650.0);
   public static final IntValue DEFAULT_LIMIT = BUILDER.comment("How many players can have the same power. 0 = no limit.")
      .defineInRange("uniquePowers.defaultLimit", 1, 0, 1000);
   public static final ConfigValue<List<? extends String>> LIMITS = BUILDER.comment("Limits for single powers, e.g. \"pyrokinetic=3\". 0 = no limit.")
      .defineListAllowEmpty("uniquePowers.limits", List.of(), () -> "powerId=1", o -> {
         if (o instanceof String s && s.contains("=")) {
            return true;
         }

         return false;
      });
   public static final BooleanValue REALISTIC_MOVEMENT = BUILDER.comment("Leaps need solid ground, and Web Swing needs something to hang from.")
      .define("gameplay.realisticMovement", false);
   public static final BooleanValue LOSE_POWER_ON_DEATH = BUILDER.comment("Players lose their power when they die.").define("death.losePower", false);
   public static final DoubleValue MASTERY_LOSS_PERCENT = BUILDER.comment("Percent of mastery lost on death.")
      .defineInRange("death.masteryLossPercent", 0.0, 0.0, 100.0);
   public static final DoubleValue MASTERY_GAIN = BUILDER.comment("Multiplier on mastery gained.").defineInRange("mastery.gainMultiplier", 1.0, 0.0, 100.0);
   public static final IntValue MAX_MASTERY = BUILDER.comment("Highest mastery level. 0 = no cap.").defineInRange("mastery.maxLevel", 0, 0, 10000);
   public static final DoubleValue PVP_DAMAGE = BUILDER.comment("Multiplier on ability damage against players.")
      .defineInRange("pvp.abilityDamageMultiplier", 1.0, 0.0, 100.0);
   public static final BooleanValue PVP_HOSTILE_EFFECTS = BUILDER.comment("Powers can hold, slow or give harmful effects to other players.")
      .define("pvp.hostileEffects", true);
   public static final BooleanValue NOMINATION_REMINDER = BUILDER.comment("Players see the Mod of the Year nomination reminder (ends November 9, 2026).")
      .define("contest.nominationReminder", true);
   public static final ModConfigSpec SPEC = BUILDER.build();

   private ServerRules() {
   }

   public static boolean nearDeath() {
      return orDefault(NEAR_DEATH, true);
   }

   public static double awakenChance() {
      return orDefault(AWAKEN_CHANCE, 1.0);
   }

   public static long retryTicks() {
      return Math.round(orDefault(RETRY_SECONDS, 30.0) * 20.0);
   }

   public static double graceMinutes() {
      return orDefault(GRACE_MINUTES, 0.0);
   }

   public static boolean fromPlayerDamage() {
      return orDefault(FROM_PLAYER_DAMAGE, true);
   }

   public static boolean startAwakened() {
      return orDefault(START_AWAKENED, false);
   }

   public static boolean powerDisabled(String powerId) {
      return SPEC.isLoaded() && ((List)DISABLED_POWERS.get()).contains(powerId);
   }

   public static boolean dimensionDisabled(String dimensionId) {
      return SPEC.isLoaded() && ((List)DISABLED_DIMENSIONS.get()).contains(dimensionId);
   }

   public static boolean anyDimensionDisabled() {
      return SPEC.isLoaded() && !((List)DISABLED_DIMENSIONS.get()).isEmpty();
   }

   public static boolean uniquePowers() {
      return orDefault(UNIQUE_POWERS, false);
   }

   public static double staleAfterDays() {
      return orDefault(STALE_AFTER_DAYS, 0.0);
   }

   public static int defaultLimit() {
      return orDefault(DEFAULT_LIMIT, 1);
   }

   public static List<? extends String> limits() {
      return SPEC.isLoaded() ? (List)LIMITS.get() : List.of();
   }

   public static boolean realisticMovement() {
      return orDefault(REALISTIC_MOVEMENT, false);
   }

   public static boolean losePowerOnDeath() {
      return orDefault(LOSE_POWER_ON_DEATH, false);
   }

   public static double masteryLossPercent() {
      return orDefault(MASTERY_LOSS_PERCENT, 0.0);
   }

   public static double masteryGain() {
      return orDefault(MASTERY_GAIN, 1.0);
   }

   public static int maxMastery() {
      int cap = orDefault(MAX_MASTERY, 0);
      return cap <= 0 ? Integer.MAX_VALUE : cap;
   }

   public static double pvpDamage() {
      return orDefault(PVP_DAMAGE, 1.0);
   }

   public static boolean pvpHostileEffects() {
      return orDefault(PVP_HOSTILE_EFFECTS, true);
   }

   public static boolean nominationReminder() {
      return orDefault(NOMINATION_REMINDER, true);
   }

   private static <T> T orDefault(ConfigValue<T> value, T fallback) {
      return (T)(SPEC.isLoaded() ? value.get() : fallback);
   }
}
