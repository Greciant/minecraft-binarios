package com.devilfruits.core.config;

import com.devilfruits.core.gacha.CasketLootModifier;
import com.devilfruits.core.gacha.GachaTier;
import com.devilfruits.core.logia.DamageCategory;
import com.devilfruits.core.logia.HitRule;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.EnumValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

public final class CoreConfig {
   public static final ModConfigSpec SPEC;
   public static final BooleanValue ONE_PER_WORLD;
   public static final BooleanValue LOSE_ON_DEATH;
   public static final EnumValue<CoreConfig.ReincarnationMode> REINCARNATION_MODE;
   public static final IntValue REINCARNATION_RADIUS;
   public static final BooleanValue KEEP_MASTERY_ON_LOSS;
   public static final BooleanValue SECOND_FRUIT_KILLS;
   public static final DoubleValue LOOT_CHANCE_MULTIPLIER;
   public static final BooleanValue CASKET_LOOT;
   public static final ConfigValue<List<? extends String>> CASKET_TABLE_CHANCES;
   private static final Map<GachaTier, IntValue> GACHA_TIER_WEIGHTS = new EnumMap<>(GachaTier.class);
   public static final DoubleValue CASKET_OPEN_SECONDS;
   public static final DoubleValue CASKET_REVEAL_SECONDS;
   public static final BooleanValue CASKET_KEY_CONSUMED;
   public static final EnumValue<CoreConfig.AnnounceFrom> CASKET_ANNOUNCE_FROM;
   public static final BooleanValue CASKET_CINEMATIC;
   public static final BooleanValue WATER_WEAKNESS;
   public static final DoubleValue SINK_SPEED;
   public static final BooleanValue SEASTONE_WEAKNESS;
   public static final BooleanValue REVERT_ON_DEATH;
   public static final BooleanValue REVERT_IN_WATER;
   public static final BooleanValue REVERT_ON_DEPLETION;
   public static final IntValue COMBAT_SECONDS;
   public static final BooleanValue CREATIVE_FREE_ENERGY;
   public static final DoubleValue XP_MULTIPLIER;
   public static final DoubleValue XP_PER_HIT;
   public static final ConfigValue<List<? extends Integer>> XP_CURVE;
   public static final DoubleValue DAMAGE_PER_LEVEL;
   public static final DoubleValue RANGE_PER_LEVEL;
   public static final DoubleValue COOLDOWN_REDUCTION_PER_LEVEL;
   public static final BooleanValue BLOCK_DESTRUCTION;
   public static final BooleanValue RESPECT_MOB_GRIEFING;
   public static final EnumValue<CoreConfig.DebrisMode> DEBRIS_MODE;
   public static final IntValue RESTORE_DELAY_SECONDS;
   public static final IntValue MAX_BLOCKS_PER_CAST;
   public static final IntValue MAX_DEBRIS_ENTITIES_PER_CAST;
   public static final DoubleValue MAX_BLOCK_HARDNESS;
   public static final BooleanValue DROP_BLOCKS;
   public static final BooleanValue AFFECT_PLAYERS;
   public static final DoubleValue GLOBAL_DAMAGE_MULTIPLIER;
   public static final BooleanValue LOGIA_INTANGIBILITY;
   private static final Map<DamageCategory, EnumValue<HitRule>> LOGIA_RULES = new EnumMap<>(DamageCategory.class);
   public static final DoubleValue LOGIA_PARTIAL_FACTOR;
   public static final DoubleValue LOGIA_NEGATE_ENERGY;
   public static final DoubleValue LOGIA_NEGATE_ENERGY_PER_DAMAGE;
   public static final BooleanValue LOGIA_SEASTONE_WEAPONS_BYPASS;
   public static final BooleanValue LOGIA_ARMAMENT_BYPASS;
   public static final ConfigValue<List<? extends String>> LOGIA_BYPASS_DAMAGE_TYPES;
   public static final IntValue LOGIA_REFORM_TICKS;
   public static final DoubleValue LOGIA_DASH_RANGE;
   public static final IntValue LOGIA_DASH_COOLDOWN;
   public static final DoubleValue LOGIA_DASH_ENERGY;
   public static final DoubleValue LOGIA_DASH_SPEED;

   public static int xpForLevel(int level) {
      List<? extends Integer> curve = (List<? extends Integer>)XP_CURVE.get();
      if (curve.isEmpty()) {
         return 100 * level;
      } else {
         int index = Math.max(0, Math.min(curve.size() - 1, level - 1));
         return Math.max(1, curve.get(index));
      }
   }

   public static int gachaTierWeight(GachaTier tier) {
      return (Integer)GACHA_TIER_WEIGHTS.get(tier).get();
   }

   public static boolean announces(GachaTier tier) {
      CoreConfig.AnnounceFrom from = (CoreConfig.AnnounceFrom)CASKET_ANNOUNCE_FROM.get();
      return from != CoreConfig.AnnounceFrom.NONE && tier.atLeast(GachaTier.valueOf(from.name()));
   }

   private static HitRule defaultRule(DamageCategory category) {
      return switch (category) {
         case MELEE, PROJECTILE, EXPLOSION, FALL, ENVIRONMENT -> HitRule.PASS;
         case FIRE, LIGHTNING, FREEZE, ABILITY -> HitRule.PARTIAL;
         case MAGIC, WATER, ALWAYS, OTHER -> HitRule.CONNECT;
      };
   }

   public static HitRule logiaRule(DamageCategory category) {
      EnumValue<HitRule> value = LOGIA_RULES.get(category);
      return value == null ? HitRule.CONNECT : (HitRule)value.get();
   }

   private CoreConfig() {
   }

   static {
      Builder b = new Builder();
      b.comment("Devil Fruit rules (anime-faithful defaults).").push("fruit");
      ONE_PER_WORLD = b.comment("Each Devil Fruit exists only once per world: a casket cannot roll it while it is held, carried or eaten.")
         .define("onePerWorld", true);
      LOSE_ON_DEATH = b.comment("When a user dies the power is lost and the fruit reincarnates. If false the power survives death.")
         .define("loseOnDeath", true);
      REINCARNATION_MODE = b.comment(
            "Where a reincarnated fruit appears: NEARBY_CHEST (random container near the death spot, falls back to DROP), DROP (at the death spot) or LOOT_POOL (free again: a casket may roll it)."
         )
         .defineEnum("reincarnationMode", CoreConfig.ReincarnationMode.NEARBY_CHEST);
      REINCARNATION_RADIUS = b.comment("Search radius in blocks for NEARBY_CHEST reincarnation.").defineInRange("reincarnationRadius", 64, 8, 256);
      KEEP_MASTERY_ON_LOSS = b.comment("Keep mastery level/XP when the power is lost (so re-eating the same fruit restores it).")
         .define("keepMasteryOnLoss", false);
      SECOND_FRUIT_KILLS = b.comment("Eating a second Devil Fruit (any item in #devilfruits:devil_fruits) kills the eater.").define("secondFruitKills", true);
      LOOT_CHANCE_MULTIPLIER = b.comment("Multiplier applied to the chances in data/devilfruits/loot_modifiers/*.json.")
         .defineInRange("lootChanceMultiplier", 1.0, 0.0, 100.0);
      b.pop();
      b.comment(
            new String[]{
               "Devil Fruit Caskets: dungeon chests hold caskets, a Golden Key in the off hand opens one and only then a fruit is rolled.",
               "A fruit's rarity is its own gacha tier (each fruit file: [fruit] gachaTier), never the chest it came from."
            }
         )
         .push("gacha");
      CASKET_LOOT = b.comment("Dungeon chests may contain Devil Fruit Caskets.").define("casketLoot", true);
      CASKET_TABLE_CHANCES = b.comment(
            new String[]{
               "Chance of a casket per chest, \"<loot table>=<chance>\" (x lootChanceMultiplier). Common dungeons are low,",
               "rare dungeons higher; archaeology tables replace the brushed item. Tables not listed use the chance in",
               "data/devilfruits/loot_modifiers/casket_in_*.json."
            }
         )
         .defineListAllowEmpty("tableChances", CasketLootModifier.defaultChanceEntries(), () -> "minecraft:chests/simple_dungeon=0.04", o -> {
            if (o instanceof String s && CasketLootModifier.parseEntry(s) != null) {
               return true;
            }

            return false;
         });
      b.comment("Weights of the tiers when a casket opens; tiers without a free fruit are skipped and the rest renormalised.").push("tierWeights");

      for (GachaTier tier : GachaTier.values()) {
         GACHA_TIER_WEIGHTS.put(tier, b.defineInRange(tier.name().toLowerCase(Locale.ROOT), tier.defaultWeight(), 0, 10000));
      }

      b.pop();
      CASKET_OPEN_SECONDS = b.comment("Seconds the use key is held to unlock a casket.").defineInRange("openSeconds", 2.5, 0.5, 10.0);
      CASKET_REVEAL_SECONDS = b.comment("Seconds of the reveal before the fruit lands in the inventory.").defineInRange("revealSeconds", 3.0, 0.5, 10.0);
      CASKET_KEY_CONSUMED = b.comment("Opening a casket uses up the Golden Key.").define("keyConsumed", true);
      CASKET_ANNOUNCE_FROM = b.comment("From which tier on everybody reads \"X opened a casket and found Y\" in chat (NONE: never).")
         .defineEnum("announceFromTier", CoreConfig.AnnounceFrom.LEGENDARY);
      CASKET_CINEMATIC = b.comment(
            new String[]{
               "The reveal plays as a cinematic (light, rings, camera; each client's VFX quality still applies).",
               "Off: every client plays the simple vanilla-particle reveal."
            }
         )
         .define("cinematic", true);
      b.pop();
      b.comment("Devil Fruit weaknesses.").push("weakness");
      WATER_WEAKNESS = b.comment("Submerged deeper than one block: Slowness, Weakness, sinking and no abilities.").define("waterWeakness", true);
      SINK_SPEED = b.comment("Downward speed (blocks/tick) forced on a submerged user.").defineInRange("sinkSpeed", 0.08, 0.0, 1.0);
      SEASTONE_WEAKNESS = b.comment("Holding seastone (#devilfruits:seastone items) or standing on seastone blocks suppresses the powers like deep water.")
         .define("seastoneWeakness", true);
      b.pop();
      b.comment("Forms (Zoan transformations and other alternate forms).").push("forms");
      REVERT_ON_DEATH = b.comment("Return to the base form on death.").define("revertOnDeath", true);
      REVERT_IN_WATER = b.comment("Return to the base form when the powers are suppressed (deep water, seastone).").define("revertInWater", true);
      REVERT_ON_DEPLETION = b.comment("Return to the base form when a form's energy drain empties the energy bar.").define("revertOnDepletion", true);
      b.pop();
      b.comment("Energy (amounts and regeneration are set per fruit).").push("energy");
      COMBAT_SECONDS = b.comment("Seconds after dealing/taking damage or casting during which the user counts as in combat.")
         .defineInRange("combatSeconds", 6, 0, 600);
      CREATIVE_FREE_ENERGY = b.comment("Creative-mode players do not consume energy (cooldowns still apply).").define("creativeFreeEnergy", true);
      b.pop();
      b.comment("Mastery (levels 1-10).").push("mastery");
      XP_MULTIPLIER = b.comment("Multiplier for all mastery XP gains (each fruit has its own multiplier on top).")
         .defineInRange("xpMultiplier", 1.0, 0.0, 100.0);
      XP_PER_HIT = b.comment("Mastery XP per entity hit by an ability.").defineInRange("xpPerHit", 2.0, 0.0, 1000.0);
      XP_CURVE = b.comment("XP needed to go from level N to N+1 (9 entries for levels 1->10).")
         .defineList("xpCurve", List.of(100, 180, 300, 450, 650, 900, 1200, 1600, 2100), () -> 100, o -> {
            if (o instanceof Integer i && i > 0) {
               return true;
            }

            return false;
         });
      DAMAGE_PER_LEVEL = b.comment("Damage bonus per mastery level above 1 (0.12 = +12%).").defineInRange("damagePerLevel", 0.12, 0.0, 10.0);
      RANGE_PER_LEVEL = b.comment("Radius/reach bonus per mastery level above 1.").defineInRange("rangePerLevel", 0.06, 0.0, 10.0);
      COOLDOWN_REDUCTION_PER_LEVEL = b.comment("Cooldown reduction per mastery level above 1 (0.03 = -3%).")
         .defineInRange("cooldownReductionPerLevel", 0.03, 0.0, 0.1);
      b.pop();
      b.comment("Terrain interaction.").push("world");
      BLOCK_DESTRUCTION = b.comment("Abilities may tear out / shatter blocks.").define("blockDestruction", true);
      RESPECT_MOB_GRIEFING = b.comment("Also require the mobGriefing game rule for block destruction.").define("respectMobGriefing", true);
      DEBRIS_MODE = b.comment("TEMPORARY: torn-out blocks fly as debris and the terrain heals after restoreDelaySeconds. PERMANENT: blocks are destroyed.")
         .defineEnum("debrisMode", CoreConfig.DebrisMode.TEMPORARY);
      RESTORE_DELAY_SECONDS = b.comment("Seconds before TEMPORARY terrain damage heals (staggered per block).")
         .defineInRange("restoreDelaySeconds", 12, 1, 3600);
      MAX_BLOCKS_PER_CAST = b.comment("Upper bound of blocks affected by a single cast.").defineInRange("maxBlocksPerCast", 160, 0, 4096);
      MAX_DEBRIS_ENTITIES_PER_CAST = b.comment("Upper bound of flying debris entities spawned by a single cast (the rest only produces particles).")
         .defineInRange("maxDebrisEntitiesPerCast", 40, 0, 512);
      MAX_BLOCK_HARDNESS = b.comment(
            "Blocks harder than this are never affected by block changes that have no rule of their own (stone = 1.5, obsidian = 50). Block-breaking techniques ignore it: they break everything but #devilfruits:resists_techniques."
         )
         .defineInRange("maxBlockHardness", 6.0, 0.0, 100.0);
      DROP_BLOCKS = b.comment("PERMANENT mode only: destroyed blocks drop as items.").define("dropBlocks", false);
      b.pop();
      b.comment("Combat.").push("combat");
      AFFECT_PLAYERS = b.comment("Abilities can hurt other players (still subject to the server PvP setting and teams).").define("affectPlayers", true);
      GLOBAL_DAMAGE_MULTIPLIER = b.comment("Multiplier for all ability damage.").defineInRange("globalDamageMultiplier", 1.0, 0.0, 100.0);
      b.pop();
      b.comment("Logia fruits: the body is the element. A fruit's own file can override rules per element and tunes its counters.").push("logia");
      LOGIA_INTANGIBILITY = b.comment("Logia bodies let hits pass through (off = Logia users are hit like anybody).").define("intangibility", true);
      LOGIA_PARTIAL_FACTOR = b.comment("Damage factor of hits a Logia body only softens (rule PARTIAL).").defineInRange("partialFactor", 0.5, 0.0, 1.0);
      LOGIA_NEGATE_ENERGY = b.comment(
            new String[]{
               "Optional balancing: energy each negated hit costs. 0 (default) = intangibility is free, like in the story;",
               "above 0 an exhausted user is hit."
            }
         )
         .defineInRange("energyPerNegatedHit", 0.0, 0.0, 1000.0);
      LOGIA_NEGATE_ENERGY_PER_DAMAGE = b.comment("Optional balancing: extra energy per point of damage a negated hit would have dealt (0 = free).")
         .defineInRange("energyPerNegatedDamage", 0.0, 0.0, 100.0);
      LOGIA_SEASTONE_WEAPONS_BYPASS = b.comment("Weapons in #devilfruits:seastone_weapon hit a Logia body.").define("seastoneWeaponsBypass", true);
      LOGIA_ARMAMENT_BYPASS = b.comment("Attackers under the Armament Coating effect hit a Logia body.").define("armamentBypass", true);
      LOGIA_BYPASS_DAMAGE_TYPES = b.comment("Damage type ids that always hit a Logia body (besides the tag #devilfruits:logia_bypass).")
         .defineListAllowEmpty("bypassDamageTypes", List.of(), () -> "minecraft:generic", o -> {
            if (o instanceof String s && s.contains(":")) {
               return true;
            }

            return false;
         });
      LOGIA_REFORM_TICKS = b.comment("Ticks an elemental body takes to reform.").defineInRange("reformTicks", 8, 0, 100);
      LOGIA_DASH_RANGE = b.comment("Elemental dash: longest relocation in blocks.").defineInRange("dashRange", 8.0, 1.0, 64.0);
      LOGIA_DASH_COOLDOWN = b.comment("Elemental dash: cooldown in ticks.").defineInRange("dashCooldownTicks", 40, 0, 1200);
      LOGIA_DASH_ENERGY = b.comment("Elemental dash: energy cost.").defineInRange("dashEnergy", 10.0, 0.0, 1000.0);
      LOGIA_DASH_SPEED = b.comment("Elemental dash: average speed of the flowing body in blocks per tick (the body really travels the way).")
         .defineInRange("dashSpeed", 1.0, 0.3, 4.0);
      b.comment(
            new String[]{
               "What a Logia body does with each kind of harm: PASS (goes through), PARTIAL (softened), CONNECT (hits).",
               "Damage that bypasses invulnerability, the void, the border, starvation, suffocation, cramming and drowning always connect."
            }
         )
         .push("rules");

      for (DamageCategory category : DamageCategory.values()) {
         if (category != DamageCategory.ALWAYS) {
            LOGIA_RULES.put(category, b.defineEnum(category.configName(), defaultRule(category)));
         }
      }

      b.pop();
      b.pop();
      SPEC = b.build();
   }

   public static enum AnnounceFrom {
      NONE,
      LEGENDARY,
      RARE,
      UNCOMMON,
      COMMON;
   }

   public static enum DebrisMode {
      TEMPORARY,
      PERMANENT;
   }

   public static enum ReincarnationMode {
      NEARBY_CHEST,
      DROP,
      LOOT_POOL;
   }
}
