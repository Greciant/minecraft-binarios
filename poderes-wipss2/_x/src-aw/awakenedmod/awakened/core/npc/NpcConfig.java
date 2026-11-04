package com.awakenedmod.awakened.core.npc;

import com.awakenedmod.awakened.core.CatalystConfig;
import com.awakenedmod.awakened.core.ConfigOverrides;
import com.awakenedmod.awakened.core.rules.ServerRules;
import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

public final class NpcConfig {
   private static final Builder BUILDER = new Builder();
   private static final boolean DEFAULT_ENABLED = true;
   private static final int DEFAULT_INTERVAL = 1200;
   private static final double DEFAULT_CHANCE = 0.12;
   private static final int DEFAULT_RADIUS = 96;
   private static final int DEFAULT_MIN_DISTANCE = 48;
   private static final int DEFAULT_MAX_DISTANCE = 80;
   private static final int DEFAULT_MAX_MASTERY = 15;
   public static final BooleanValue ENABLED = BUILDER.comment("Powered NPCs spawn on their own.").define("npcs.enabled", true);
   public static final IntValue INTERVAL_TICKS = BUILDER.comment("Ticks between spawn rolls for each player (20 = 1 second).")
      .defineInRange("npcs.intervalTicks", 1200, 20, 72000);
   public static final DoubleValue CHANCE = BUILDER.comment("Chance a roll spawns an NPC.").defineInRange("npcs.chance", 0.12, 0.0, 1.0);
   public static final IntValue RADIUS = BUILDER.comment("No roll while a powered NPC is within this many blocks.").defineInRange("npcs.radius", 96, 0, 512);
   public static final IntValue MIN_DISTANCE = BUILDER.comment("Closest an NPC spawns to the player, in blocks.")
      .defineInRange("npcs.minDistance", 48, 16, 128);
   public static final IntValue MAX_DISTANCE = BUILDER.comment("Farthest an NPC spawns from the player, in blocks.")
      .defineInRange("npcs.maxDistance", 80, 16, 128);
   public static final IntValue MAX_MASTERY = BUILDER.comment("Highest mastery an NPC can have.").defineInRange("npcs.maxMastery", 15, 3, 100);
   public static final ConfigValue<List<? extends String>> KIT_WEIGHTS = BUILDER.comment(
         "Spawn weights per NPC, e.g. \"pyrokinetic_npc=2\". Unlisted = 1, 0 = never."
      )
      .defineListAllowEmpty("npcs.kitWeights", List.of(), () -> "pyrokinetic_npc=1", o -> {
         if (o instanceof String s && s.contains("=")) {
            return true;
         }

         return false;
      });
   private static final double DEFAULT_CATALYST_CHANCE = 0.02;
   private static final double DEFAULT_CATALYST_PER_MASTERY = 0.01;
   private static final double DEFAULT_CATALYST_MAX = 0.12;
   private static final double DEFAULT_REWARD_MULTIPLIER = 1.0;
   public static final DoubleValue CATALYST_CHANCE = BUILDER.comment("Chance a mastery 3 NPC drops a Catalyst.")
      .defineInRange("npcs.rewards.catalystChance", 0.02, 0.0, 1.0);
   public static final DoubleValue CATALYST_PER_MASTERY = BUILDER.comment("Extra Catalyst chance per mastery level above 3.")
      .defineInRange("npcs.rewards.catalystChancePerMastery", 0.01, 0.0, 1.0);
   public static final DoubleValue CATALYST_MAX = BUILDER.comment("Highest Catalyst chance.").defineInRange("npcs.rewards.catalystChanceMax", 0.12, 0.0, 1.0);
   public static final DoubleValue MASTERY_MULTIPLIER = BUILDER.comment("Multiplier on the bonus mastery for killing an NPC.")
      .defineInRange("npcs.rewards.masteryMultiplier", 1.0, 0.0, 100.0);
   public static final DoubleValue XP_MULTIPLIER = BUILDER.comment("Multiplier on the bonus character XP for killing an NPC.")
      .defineInRange("npcs.rewards.xpMultiplier", 1.0, 0.0, 100.0);
   public static final ModConfigSpec SPEC = BUILDER.build();
   private static final ConfigOverrides<Double> WEIGHTS = new ConfigOverrides<>("npcs.kitWeights", KIT_WEIGHTS::get, Double::parseDouble);

   private NpcConfig() {
   }

   public static boolean enabled() {
      return orDefault(ENABLED, true);
   }

   public static int intervalTicks() {
      return orDefault(INTERVAL_TICKS, 1200);
   }

   public static double chance() {
      return orDefault(CHANCE, 0.12);
   }

   public static int radius() {
      return orDefault(RADIUS, 96);
   }

   public static int minDistance() {
      return orDefault(MIN_DISTANCE, 48);
   }

   public static int maxDistance() {
      return Math.max(minDistance(), orDefault(MAX_DISTANCE, 80));
   }

   public static int maxMastery() {
      return orDefault(MAX_MASTERY, 15);
   }

   public static double catalystChance(int mastery) {
      double base = orDefault(CATALYST_CHANCE, 0.02);
      double step = orDefault(CATALYST_PER_MASTERY, 0.01);
      double max = orDefault(CATALYST_MAX, 0.12);
      return Math.min(max, base + step * (double)Math.max(0, mastery - 3)) * CatalystConfig.dropMultiplier();
   }

   public static double masteryMultiplier() {
      return orDefault(MASTERY_MULTIPLIER, 1.0);
   }

   public static double xpMultiplier() {
      return orDefault(XP_MULTIPLIER, 1.0);
   }

   public static double weight(NpcKit kit) {
      if (ServerRules.powerDisabled(kit.powerId())) {
         return 0.0;
      } else {
         double fallback = kit.spawnWeight();
         return Math.max(0.0, SPEC.isLoaded() ? WEIGHTS.get(kit.id(), fallback) : fallback);
      }
   }

   private static <T> T orDefault(ConfigValue<T> value, T fallback) {
      return (T)(SPEC.isLoaded() ? value.get() : fallback);
   }
}
