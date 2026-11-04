package com.devilfruits.core.config;

import com.devilfruits.core.logia.DamageCategory;
import com.devilfruits.core.logia.HitRule;
import com.devilfruits.core.logia.LogiaCounter;
import com.devilfruits.core.logia.LogiaProfile;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.EnumValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

public final class LogiaFruitConfig {
   private final Map<DamageCategory, EnumValue<LogiaFruitConfig.RuleOverride>> rules = new EnumMap<>(DamageCategory.class);
   private final DoubleValue elementalDrain;
   private final DoubleValue sourceRegen;
   private final DoubleValue opposedRegen;
   private final Map<String, LogiaFruitConfig.CounterConfig> counters = new HashMap<>();

   LogiaFruitConfig(Builder b, LogiaProfile profile) {
      b.comment("Logia body (element " + profile.element() + ").").push("logia");
      this.elementalDrain = b.comment("Energy per second while the whole body is the element.")
         .defineInRange("elementalDrainPerSecond", (double)profile.elementalDrain(), 0.0, 1000.0);
      b.comment("Energy regeneration multipliers from the environment.").push("environment");
      this.sourceRegen = b.comment("On or in the element's source blocks / biomes.")
         .defineInRange("sourceRegen", profile.environment().sourceRegen(), 0.0, 100.0);
      this.opposedRegen = b.comment("In an opposed biome or condition.").defineInRange("opposedRegen", profile.environment().opposedRegen(), 0.0, 100.0);
      b.pop();
      b.comment("Rule overrides for this element (DEFAULT = the server-wide rule in devilfruits-common.toml).").push("rules");

      for (DamageCategory category : DamageCategory.values()) {
         if (category != DamageCategory.ALWAYS) {
            HitRule override = profile.override(category);
            this.rules
               .put(
                  category,
                  b.defineEnum(
                     category.configName(), override == null ? LogiaFruitConfig.RuleOverride.DEFAULT : LogiaFruitConfig.RuleOverride.valueOf(override.name())
                  )
               );
         }
      }

      b.pop();
      if (!profile.counters().isEmpty()) {
         b.comment("Weaknesses of the element.").push("counters");

         for (LogiaCounter counter : profile.counters()) {
            b.push(counter.id());
            this.counters
               .put(
                  counter.id(),
                  new LogiaFruitConfig.CounterConfig(
                     b.comment("Whether this counter works at all.").define("enabled", true),
                     b.comment("Ticks it stays active after its last trigger.").defineInRange("lingerTicks", counter.lingerTicks(), 0, 72000),
                     b.comment("Energy drained per second while active (if the counter drains).")
                        .defineInRange("energyDrainPerSecond", (double)counter.energyDrainPerSecond(), 0.0, 1000.0),
                     b.comment("Ability damage factor while active (if the counter weakens abilities).")
                        .defineInRange("abilityPower", (double)counter.abilityPower(), 0.0, 1.0),
                     b.comment("Movement speed reduction while active (if the counter slows; 0.3 = -30%).")
                        .defineInRange("speedPenalty", (double)counter.speedPenalty(), 0.0, 1.0)
                  )
               );
            b.pop();
         }

         b.pop();
      }

      b.pop();
   }

   @Nullable
   public HitRule rule(DamageCategory category) {
      EnumValue<LogiaFruitConfig.RuleOverride> value = this.rules.get(category);
      return value != null && value.get() != LogiaFruitConfig.RuleOverride.DEFAULT
         ? HitRule.valueOf(((LogiaFruitConfig.RuleOverride)value.get()).name())
         : null;
   }

   public double elementalDrain() {
      return (Double)this.elementalDrain.get();
   }

   public double sourceRegen() {
      return (Double)this.sourceRegen.get();
   }

   public double opposedRegen() {
      return (Double)this.opposedRegen.get();
   }

   @Nullable
   public LogiaFruitConfig.CounterConfig counter(String id) {
      return this.counters.get(id);
   }

   public static record CounterConfig(BooleanValue enabled, IntValue lingerTicks, DoubleValue energyDrain, DoubleValue abilityPower, DoubleValue speedPenalty) {
   }

   public static enum RuleOverride {
      DEFAULT,
      PASS,
      PARTIAL,
      CONNECT;
   }
}
