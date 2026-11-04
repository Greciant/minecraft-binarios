package com.devilfruits.core.haki;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

public final class HakiConfig {
   public static final ModConfigSpec SPEC;
   public static final DoubleValue XP_BASE;
   public static final DoubleValue XP_EXPONENT;
   public static final DoubleValue XP_PER_HEALTH;
   public static final DoubleValue XP_PER_FIST_HIT;
   public static final DoubleValue XP_COATED_MULTIPLIER;
   public static final DoubleValue XP_LOGIA_VICTIM;
   public static final DoubleValue XP_HAKI_VICTIM;
   public static final DoubleValue XP_BOSS;
   public static final DoubleValue XP_MIN_VICTIM_HEALTH;
   public static final IntValue XP_SAME_TYPE_WINDOW;
   public static final DoubleValue XP_SAME_TYPE_FALLOFF;
   public static final BooleanValue XP_FROM_SPAWNERS;
   public static final DoubleValue XP_DAILY_SOFT_CAP;
   public static final DoubleValue XP_OVER_CAP;
   public static final DoubleValue SPIRIT_BASE;
   public static final DoubleValue SPIRIT_PER_LEVEL;
   public static final DoubleValue DRAIN_PER_SECOND;
   public static final DoubleValue ACTIVATION_COST;
   public static final DoubleValue REGEN_PER_SECOND;
   public static final IntValue REGEN_DELAY;
   public static final DoubleValue DRAIN_NOVICE;
   public static final DoubleValue DRAIN_ADEPT;
   public static final DoubleValue DRAIN_EXPERT;
   public static final DoubleValue DRAIN_MASTER;
   public static final DoubleValue ATTACK_PER_LEVEL;
   public static final DoubleValue DEFENSE_PER_LEVEL;
   public static final DoubleValue LOGIA_FRACTION_BASE;
   public static final DoubleValue LOGIA_FRACTION_PER_LEVEL;
   public static final DoubleValue LOGIA_ELEMENT_REDUCTION;
   public static final DoubleValue ABILITY_MULTIPLIER;
   public static final DoubleValue CLASH_BONUS;
   public static final DoubleValue CLASH_PENALTY;
   public static final DoubleValue CLASH_JITTER;
   public static final IntValue RYUO_CHARGE_TICKS;
   public static final DoubleValue RYUO_COST;
   public static final DoubleValue RYUO_MULTIPLIER;
   public static final DoubleValue RYUO_INTERNAL;
   public static final DoubleValue RYUO_INTERNAL_MASTER;
   public static final BooleanValue SCROLL_LOOT;
   public static final DoubleValue SCROLL_LOOT_CHANCE;
   public static final DoubleValue SCROLL_REPEAT_XP;
   public static final BooleanValue LIBRARIAN_TRADE;
   public static final IntValue LIBRARIAN_PRICE;

   private HakiConfig() {
   }

   static {
      Builder b = new Builder();
      b.comment("Training Armament Haki: XP to the next level is xpBase * level ^ xpExponent.").push("progression");
      XP_BASE = b.defineInRange("xpBase", 120.0, 1.0, 100000.0);
      XP_EXPONENT = b.defineInRange("xpExponent", 1.5, 0.0, 4.0);
      XP_PER_HEALTH = b.comment("XP per point of the victim's maximum health for a bare-fist kill.").defineInRange("xpPerHealth", 1.0, 0.0, 100.0);
      XP_PER_FIST_HIT = b.comment("XP for every bare-fist blow that lands.").defineInRange("xpPerFistHit", 0.5, 0.0, 100.0);
      XP_COATED_MULTIPLIER = b.comment("XP multiplier while training with the coat on.").defineInRange("coatedMultiplier", 2.0, 0.0, 100.0);
      XP_LOGIA_VICTIM = b.defineInRange("logiaVictimMultiplier", 3.0, 0.0, 100.0);
      XP_HAKI_VICTIM = b.defineInRange("hakiVictimMultiplier", 3.0, 0.0, 100.0);
      XP_BOSS = b.comment("Multiplier for #c:bosses.").defineInRange("bossMultiplier", 5.0, 0.0, 100.0);
      XP_MIN_VICTIM_HEALTH = b.comment("Victims with less maximum health give no XP.").defineInRange("minVictimHealth", 6.0, 0.0, 1000.0);
      XP_SAME_TYPE_WINDOW = b.comment("Seconds a kill of an entity type counts against the next kills of that type.")
         .defineInRange("sameTypeWindowSeconds", 300, 0, 86400);
      XP_SAME_TYPE_FALLOFF = b.comment("XP multiplier per recent kill of the same type (diminishing returns).")
         .defineInRange("sameTypeFalloff", 0.85, 0.0, 1.0);
      XP_FROM_SPAWNERS = b.comment("Whether mobs from spawners give XP.").define("spawnerXp", false);
      XP_DAILY_SOFT_CAP = b.comment("XP per in-game day before the rest is reduced.").defineInRange("dailySoftCap", 2500.0, 0.0, 1.0E7);
      XP_OVER_CAP = b.defineInRange("overCapMultiplier", 0.25, 0.0, 1.0);
      b.pop();
      b.comment("Spirit, Haki's own energy. Maximum = base + perLevel * level.").push("spirit");
      SPIRIT_BASE = b.defineInRange("spiritBase", 60.0, 1.0, 10000.0);
      SPIRIT_PER_LEVEL = b.defineInRange("spiritPerLevel", 12.0, 0.0, 1000.0);
      DRAIN_PER_SECOND = b.comment("Drain per second of a full-body coat (scaled by the coated share, at least a fifth of it).")
         .defineInRange("spiritDrainPerSecond", 6.0, 0.0, 1000.0);
      ACTIVATION_COST = b.defineInRange("activationCost", 8.0, 0.0, 1000.0);
      REGEN_PER_SECOND = b.defineInRange("regenPerSecond", 4.0, 0.0, 1000.0);
      REGEN_DELAY = b.comment("Ticks after the coat comes off before spirit regenerates.").defineInRange("regenDelayTicks", 20, 0, 1200);
      DRAIN_NOVICE = b.comment("Drain multipliers per tier.").defineInRange("drainNovice", 1.6, 0.0, 10.0);
      DRAIN_ADEPT = b.defineInRange("drainAdept", 1.2, 0.0, 10.0);
      DRAIN_EXPERT = b.defineInRange("drainExpert", 0.9, 0.0, 10.0);
      DRAIN_MASTER = b.defineInRange("drainMaster", 0.6, 0.0, 10.0);
      b.pop();
      b.push("combat");
      ATTACK_PER_LEVEL = b.comment("Damage bonus of a coated strike per level (level 10: x1.6).").defineInRange("attackPerLevel", 0.06, 0.0, 10.0);
      DEFENSE_PER_LEVEL = b.comment("Damage reduction on a coated part per level (level 10: 50 %).").defineInRange("defensePerLevel", 0.05, 0.0, 0.1);
      LOGIA_FRACTION_BASE = b.comment("Share of a coated strike that reaches a Logia body: base + perLevel * level, at most 1 (full from level 4).")
         .defineInRange("logiaFractionBase", 0.2, 0.0, 1.0);
      LOGIA_FRACTION_PER_LEVEL = b.defineInRange("logiaFractionPerLevel", 0.2, 0.0, 1.0);
      LOGIA_ELEMENT_REDUCTION = b.comment("Extra reduction of Logia element techniques against a coated body.")
         .defineInRange("logiaElementReduction", 0.15, 0.0, 1.0);
      ABILITY_MULTIPLIER = b.comment("Damage multiplier of coatable techniques used with the coat on.").defineInRange("abilityMultiplier", 1.25, 1.0, 10.0);
      CLASH_BONUS = b.comment("Damage bonus of the side that wins a clash.").defineInRange("clashBonus", 0.25, 0.0, 10.0);
      CLASH_PENALTY = b.comment("Damage share left when the defender wins a clash.").defineInRange("clashPenalty", 0.5, 0.0, 1.0);
      CLASH_JITTER = b.comment("Random spread added to each side of a clash.").defineInRange("clashJitter", 1.0, 0.0, 10.0);
      RYUO_CHARGE_TICKS = b.comment("Ticks for a full Ryuo charge.").defineInRange("ryuoChargeTicks", 30, 1, 200);
      RYUO_COST = b.defineInRange("ryuoCost", 20.0, 0.0, 1000.0);
      RYUO_MULTIPLIER = b.comment("Damage multiplier of a fully charged Ryuo strike.").defineInRange("ryuoMultiplier", 1.8, 1.0, 20.0);
      RYUO_INTERNAL = b.comment("Share of the strike dealt again inside the target, ignoring armour (Expert).").defineInRange("ryuoInternal", 0.35, 0.0, 5.0);
      RYUO_INTERNAL_MASTER = b.defineInRange("ryuoInternalMaster", 0.6, 0.0, 5.0);
      b.pop();
      b.push("loot");
      SCROLL_LOOT = b.comment("Armament scrolls in pillager outpost, ancient city, stronghold library, end city and bastion treasure chests.")
         .define("scrollLoot", true);
      SCROLL_LOOT_CHANCE = b.defineInRange("scrollLootChance", 0.04, 0.0, 1.0);
      SCROLL_REPEAT_XP = b.comment("Haki XP an Armament scroll gives once Armament Haki is already awakened.")
         .defineInRange("scrollRepeatXp", 400.0, 0.0, 100000.0);
      LIBRARIAN_TRADE = b.comment("Master (level 5) librarians sell an Armament scroll.").define("librarianTrade", true);
      LIBRARIAN_PRICE = b.comment("Emeralds (plus a book) the librarian asks for a scroll.").defineInRange("librarianPrice", 32, 1, 64);
      b.pop();
      SPEC = b.build();
   }
}
