package com.devilfruits.core.config;

import com.devilfruits.core.ability.AbilityStats;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

public final class AbilityConfig {
   private final BooleanValue enabled;
   private final IntValue energyCost;
   private final IntValue cooldownTicks;
   private final IntValue requiredMastery;
   private final DoubleValue damage;
   private final DoubleValue range;
   private final DoubleValue knockback;
   private final Map<String, DoubleValue> extras = new LinkedHashMap<>();
   @Nullable
   private final BooleanValue blockDamage;
   private final Map<String, BooleanValue> flags = new LinkedHashMap<>();

   AbilityConfig(Builder b, AbilityStats d) {
      this.enabled = b.comment("Whether this ability can be used at all.").define("enabled", true);
      this.energyCost = b.comment("Energy consumed per cast.").defineInRange("energyCost", d.energyCost(), 0, 1000);
      this.cooldownTicks = b.comment("Cooldown in ticks (20 ticks = 1 second) before mastery reduction.")
         .defineInRange("cooldownTicks", d.cooldownTicks(), 0, 72000);
      this.requiredMastery = b.comment("Mastery level (1-10) required to unlock this ability.").defineInRange("requiredMastery", d.requiredMastery(), 1, 10);
      this.damage = b.comment("Base damage (half-hearts) before mastery scaling.").defineInRange("damage", d.damage(), 0.0, 1000.0);
      this.range = b.comment("Base radius / reach in blocks before mastery scaling.").defineInRange("range", d.range(), 0.5, 128.0);
      this.knockback = b.comment("Knockback strength.").defineInRange("knockback", d.knockback(), 0.0, 20.0);
      this.blockDamage = d.blockDamage() == null
         ? null
         : b.comment("Whether this ability may break / move blocks (the global world rules and mobGriefing still apply).")
            .define("blockDamage", d.blockDamage());

      for (AbilityStats.Extra extra : d.extras()) {
         this.extras.put(extra.name(), b.comment(extra.comment()).defineInRange(extra.name(), extra.defaultValue(), extra.min(), extra.max()));
      }

      for (AbilityStats.Flag flag : d.flags()) {
         this.flags.put(flag.name(), b.comment(flag.comment()).define(flag.name(), flag.defaultValue()));
      }
   }

   public boolean enabled() {
      return (Boolean)this.enabled.get();
   }

   public int energyCost() {
      return (Integer)this.energyCost.get();
   }

   public int cooldownTicks() {
      return (Integer)this.cooldownTicks.get();
   }

   public int requiredMastery() {
      return (Integer)this.requiredMastery.get();
   }

   public double damage() {
      return (Double)this.damage.get();
   }

   public double range() {
      return (Double)this.range.get();
   }

   public double knockback() {
      return (Double)this.knockback.get();
   }

   public boolean blockDamage() {
      return this.blockDamage == null || (Boolean)this.blockDamage.get();
   }

   public double extra(String name) {
      DoubleValue value = this.extras.get(name);
      if (value == null) {
         throw new IllegalArgumentException("Unknown ability config extra: " + name);
      } else {
         return (Double)value.get();
      }
   }

   public boolean flag(String name) {
      BooleanValue value = this.flags.get(name);
      if (value == null) {
         throw new IllegalArgumentException("Unknown ability config flag: " + name);
      } else {
         return (Boolean)value.get();
      }
   }
}
