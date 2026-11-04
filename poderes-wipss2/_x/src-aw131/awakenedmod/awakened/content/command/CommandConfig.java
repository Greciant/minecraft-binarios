package com.awakenedmod.awakened.content.command;

import com.awakenedmod.awakened.content.command.vocab.Effect;
import com.awakenedmod.awakened.content.command.vocab.Target;
import com.awakenedmod.awakened.content.command.vocab.Vocabulary;
import java.util.LinkedHashMap;
import java.util.Map;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

public final class CommandConfig {
   private static final Builder BUILDER = new Builder();
   public static final DoubleValue LEVEL_STEP = BUILDER.comment("Level cost: price x (1 + this x (level - 1)).")
      .defineInRange("command.formula.levelStep", 1.0, 0.0, 10.0);
   public static final DoubleValue DURATION_SECONDS_PER_STEP = BUILDER.comment("Duration cost: price x (1 + seconds / this).")
      .defineInRange("command.formula.durationSecondsPerStep", 30.0, 1.0, 3600.0);
   public static final DoubleValue RADIUS_BLOCKS_PER_STEP = BUILDER.comment("Radius cost: price x (1 + blocks / this).")
      .defineInRange("command.formula.radiusBlocksPerStep", 10.0, 1.0, 256.0);
   public static final DoubleValue INFINITE_FACTOR = BUILDER.comment("Price multiplier for a rule with no time limit.")
      .defineInRange("command.formula.infiniteFactor", 10.0, 1.0, 100.0);
   public static final DoubleValue MASTERY_DISCOUNT_PER_LEVEL = BUILDER.comment("Price discount per mastery level. 0.02 = 2%.")
      .defineInRange("command.formula.masteryDiscountPerLevel", 0.02, 0.0, 0.09);
   public static final DoubleValue MASTERY_DISCOUNT_MAX = BUILDER.comment("Largest mastery discount. 0.2 = 20%.")
      .defineInRange("command.formula.masteryDiscountMax", 0.2, 0.0, 0.95);
   private static final Map<String, DoubleValue> EFFECT_BASE = defineEffects();
   private static final Map<String, DoubleValue> TARGET_WEIGHT = defineTargets();
   public static final ModConfigSpec SPEC = BUILDER.build();

   private CommandConfig() {
   }

   private static Map<String, DoubleValue> defineEffects() {
      Map<String, DoubleValue> table = new LinkedHashMap<>();

      for (Effect word : Vocabulary.effects()) {
         table.put(
            word.id(),
            BUILDER.comment("Stamina price of \"" + word.id() + "\" at level 1.")
               .defineInRange("command.effect." + word.id(), word.defaultWeight(), 0.0, 10000.0)
         );
      }

      return table;
   }

   private static Map<String, DoubleValue> defineTargets() {
      Map<String, DoubleValue> table = new LinkedHashMap<>();

      for (Target word : Vocabulary.targets()) {
         table.put(
            word.id(),
            BUILDER.comment("Price multiplier for \"" + word.id() + "\".").defineInRange("command.target." + word.id(), word.defaultWeight(), 0.0, 100.0)
         );
      }

      return table;
   }

   public static double weight(Effect word) {
      return live(EFFECT_BASE, word.id(), word.defaultWeight());
   }

   public static double weight(Target word) {
      return live(TARGET_WEIGHT, word.id(), word.defaultWeight());
   }

   private static double live(Map<String, DoubleValue> table, String id, double shipped) {
      DoubleValue value = table.get(id);
      return value != null && SPEC.isLoaded() ? (Double)value.get() : shipped;
   }

   static double value(DoubleValue config) {
      return SPEC.isLoaded() ? (Double)config.get() : (Double)config.getDefault();
   }

   static int value(IntValue config, int fallback) {
      return SPEC.isLoaded() ? (Integer)config.get() : fallback;
   }
}
