package com.devilfruits.core.poneglyph;

import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.EnumValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

public final class PoneglyphConfig {
   public static final ModConfigSpec SPEC;
   public static final IntValue RUB_TICKS;
   public static final BooleanValue CONSUME_PAPER;
   public static final BooleanValue CONSUME_INK;
   public static final IntValue CELL_COOLDOWN_SECONDS;
   public static final BooleanValue RUB_NOISE_AMBUSH;
   public static final IntValue AMBUSH_RADIUS;
   public static final EnumValue<PoneglyphConfig.CipherMode> CIPHER_MODE;
   public static final BooleanValue HARD_MODE;
   public static final IntValue ROUNDS;
   public static final IntValue OPTIONS;
   public static final IntValue MAX_MISTAKES;
   public static final IntValue LOCKOUT_TICKS;
   public static final IntValue XP_BASE;
   public static final DoubleValue XP_GROWTH;
   public static final IntValue XP_RUB;
   public static final IntValue XP_ROUND;
   public static final IntValue XP_DECIPHER;
   public static final IntValue L1_FEWER_DECOYS;
   public static final DoubleValue L3_LOCKOUT_FACTOR;
   public static final DoubleValue L3_RUB_FACTOR;
   public static final BooleanValue LIBRARIAN_TRADE;
   public static final IntValue LIBRARIAN_PRICE;
   public static final DoubleValue PRIMER_SHIPWRECK;
   public static final DoubleValue PRIMER_TREASURE;
   public static final DoubleValue PRIMER_UNDERWATER_RUIN;
   public static final DoubleValue PRIMER_PONEGLYPH_RUINS;
   public static final DoubleValue RUINS_CHANCE;
   public static final BooleanValue SEASTONE_ORE;
   public static final DoubleValue LEAD_MAP_CHANCE;
   public static final IntValue LEAD_MAP_RADIUS;
   public static final BooleanValue GATE_RECIPES;
   public static final ConfigValue<List<? extends String>> UNGATED_KNOWLEDGE;
   public static final BooleanValue GEAR_SUPPRESSES_HOLDER;
   public static final BooleanValue ARMOR_SUPPRESSES_FRUIT_USER;
   public static final DoubleValue ARMOR_FRUIT_DAMAGE_REDUCTION;
   public static final IntValue HIT_SUPPRESS_TICKS;
   public static final IntValue JITTE_STUN_TICKS;
   public static final IntValue CUFFS_TICKS;

   private PoneglyphConfig() {
   }

   static {
      Builder b = new Builder();
      b.comment("Taking rubbings: Paper in the main hand, an Ink Sac in the off hand, hold use on a carved poneglyph block.").push("poneglyphRubbing");
      RUB_TICKS = b.comment("Ticks of rubbing per fragment (literacy 3 shortens it).").defineInRange("rubTicks", 60, 10, 400);
      CONSUME_PAPER = b.comment("A finished rubbing uses one Paper (never in creative).").define("consumePaper", true);
      CONSUME_INK = b.comment("A finished rubbing uses one Ink Sac (never in creative).").define("consumeInk", true);
      CELL_COOLDOWN_SECONDS = b.comment("Personal cooldown per carved block before the same player can rub it again.")
         .defineInRange("cellCooldownSeconds", 600, 0, 86400);
      RUB_NOISE_AMBUSH = b.comment("A finished rubbing alerts pirates nearby (if the pirate mobs are present).").define("rubNoiseAmbush", false);
      AMBUSH_RADIUS = b.defineInRange("ambushRadius", 24, 4, 96);
      b.pop();
      b.comment("The Decipher Desk puzzle.").push("poneglyphPuzzle");
      CIPHER_MODE = b.comment(
            "STANDARD: the vanilla Enchanting Table script letter for letter. WORLDBOUND: a per-world letter permutation (the Primer shows it)."
         )
         .defineEnum("cipherMode", PoneglyphConfig.CipherMode.STANDARD);
      HARD_MODE = b.comment("Too many mistakes smudge and destroy the blueprint (otherwise the words are redrawn).").define("hardMode", false);
      ROUNDS = b.defineInRange("rounds", 3, 1, 6);
      OPTIONS = b.comment("Glyph words offered per round (literacy 1 removes some decoys).").defineInRange("options", 10, 4, 12);
      MAX_MISTAKES = b.defineInRange("maxMistakes", 3, 1, 10);
      LOCKOUT_TICKS = b.comment("Input lockout after a wrong pick.").defineInRange("lockoutTicks", 100, 0, 1200);
      b.pop();
      b.comment("Poneglyph Literacy (levels 0-5). XP needed for level n+1: literacyXpBase * literacyXpGrowth^n.").push("poneglyphLiteracy");
      XP_BASE = b.defineInRange("literacyXpBase", 40, 1, 10000);
      XP_GROWTH = b.defineInRange("literacyXpGrowth", 1.4, 1.0, 4.0);
      XP_RUB = b.comment("XP per finished rubbing.").defineInRange("xpRubbing", 3, 0, 1000);
      XP_ROUND = b.comment("XP per solved round.").defineInRange("xpRound", 8, 0, 1000);
      XP_DECIPHER = b.comment("XP per deciphered blueprint.").defineInRange("xpDecipher", 50, 0, 10000);
      L1_FEWER_DECOYS = b.comment("Literacy 1+: this many fewer decoys.").defineInRange("level1FewerDecoys", 2, 0, 6);
      L3_LOCKOUT_FACTOR = b.comment("Literacy 3+: lockout multiplier.").defineInRange("level3LockoutFactor", 0.5, 0.0, 1.0);
      L3_RUB_FACTOR = b.comment("Literacy 3+: rubbing time multiplier.").defineInRange("level3RubFactor", 0.7, 0.1, 1.0);
      b.pop();
      b.comment("Where the Primer of the Ancient Script comes from.").push("poneglyphSources");
      LIBRARIAN_TRADE = b.comment("Journeyman librarians sell the Primer.").define("primerTrade", true);
      LIBRARIAN_PRICE = b.defineInRange("primerPrice", 12, 1, 64);
      PRIMER_SHIPWRECK = b.defineInRange("primerChanceShipwreck", 0.04, 0.0, 1.0);
      PRIMER_TREASURE = b.defineInRange("primerChanceBuriedTreasure", 0.08, 0.0, 1.0);
      PRIMER_UNDERWATER_RUIN = b.defineInRange("primerChanceUnderwaterRuin", 0.05, 0.0, 1.0);
      PRIMER_PONEGLYPH_RUINS = b.defineInRange("primerChancePoneglyphRuins", 0.25, 0.0, 1.0);
      b.pop();
      b.push("poneglyphWorld");
      RUINS_CHANCE = b.comment("Chance that a placement slot of the poneglyph ruins (structure set, spacing 64 chunks) really spawns one. 0 disables them.")
         .defineInRange("ruinsChance", 0.75, 0.0, 1.0);
      SEASTONE_ORE = b.comment("Seastone ore veins on the ocean floor.").define("seastoneOre", true);
      LEAD_MAP_CHANCE = b.comment("Chance that deciphering a blueprint also yields a Lead Map to the nearest undiscovered poneglyph ruins.")
         .defineInRange("leadMapChance", 0.5, 0.0, 1.0);
      LEAD_MAP_RADIUS = b.comment("Search radius of the Lead Map, in placement cells of the ruins (64 chunks each; vanilla explorer maps search 50-100).")
         .defineInRange("leadMapRadius", 16, 1, 64);
      b.pop();
      b.comment("Knowledge gating and Seastone gear.").push("poneglyphKnowledge");
      GATE_RECIPES = b.comment("Locked recipes need their knowledge. Off: every locked recipe is free.").define("gateRecipes", true);
      UNGATED_KNOWLEDGE = b.comment("Knowledge branches whose recipes are free for everyone, e.g. [\"devilfruits:seastone_tools\"].")
         .defineListAllowEmpty("ungatedKnowledge", List.of(), () -> "devilfruits:seastone_tools", o -> o instanceof String);
      GEAR_SUPPRESSES_HOLDER = b.comment("Holding Seastone tools and weapons suppresses the holder's own fruit like any seastone.")
         .define("gearSuppressesHolder", true);
      ARMOR_SUPPRESSES_FRUIT_USER = b.comment("Wearing Seastone armour suppresses a fruit user (canon).").define("armorSuppressesFruitUser", true);
      ARMOR_FRUIT_DAMAGE_REDUCTION = b.comment("Per worn piece: share of Devil Fruit technique damage absorbed (wearers without a fruit).")
         .defineInRange("armorFruitDamageReduction", 0.1, 0.0, 0.25);
      HIT_SUPPRESS_TICKS = b.comment("A hit with Seastone gear or a Seastone arrow suppresses a fruit user for this long.")
         .defineInRange("hitSuppressTicks", 60, 0, 1200);
      JITTE_STUN_TICKS = b.comment(
            new String[]{
               "A blow of the Seastone Jitte (its tip is seastone) stuns a fruit user for this long; the jitte never suppresses",
               "its own holder (only the tip is seastone)."
            }
         )
         .defineInRange("jitteStunTicks", 30, 0, 400);
      CUFFS_TICKS = b.comment("How long Seastone Cuffs restrain their target.").defineInRange("cuffsTicks", 600, 20, 12000);
      b.pop();
      SPEC = b.build();
   }

   public static enum CipherMode {
      STANDARD,
      WORLDBOUND;
   }
}
