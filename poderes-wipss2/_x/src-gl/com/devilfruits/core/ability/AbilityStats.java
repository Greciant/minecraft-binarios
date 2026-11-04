package com.devilfruits.core.ability;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

public record AbilityStats(
   int energyCost,
   int cooldownTicks,
   int requiredMastery,
   double damage,
   double range,
   double knockback,
   List<AbilityStats.Extra> extras,
   @Nullable Boolean blockDamage,
   List<AbilityStats.Flag> flags
) {
   public static AbilityStats.Builder builder() {
      return new AbilityStats.Builder();
   }

   public static final class Builder {
      private int energyCost = 10;
      private int cooldownTicks = 40;
      private int requiredMastery = 1;
      private double damage = 4.0;
      private double range = 5.0;
      private double knockback = 1.0;
      private final List<AbilityStats.Extra> extras = new ArrayList<>();
      @Nullable
      private Boolean blockDamage;
      private final List<AbilityStats.Flag> flags = new ArrayList<>();

      public AbilityStats.Builder energy(int cost) {
         this.energyCost = cost;
         return this;
      }

      public AbilityStats.Builder cooldown(int ticks) {
         this.cooldownTicks = ticks;
         return this;
      }

      public AbilityStats.Builder mastery(int level) {
         this.requiredMastery = level;
         return this;
      }

      public AbilityStats.Builder damage(double damage) {
         this.damage = damage;
         return this;
      }

      public AbilityStats.Builder range(double range) {
         this.range = range;
         return this;
      }

      public AbilityStats.Builder knockback(double knockback) {
         this.knockback = knockback;
         return this;
      }

      public AbilityStats.Builder blockDamage(boolean def) {
         this.blockDamage = def;
         return this;
      }

      public AbilityStats.Builder extra(String name, double def, double min, double max, String comment) {
         this.extras.add(new AbilityStats.Extra(name, def, min, max, comment));
         return this;
      }

      public AbilityStats.Builder flag(String name, boolean def, String comment) {
         this.flags.add(new AbilityStats.Flag(name, def, comment));
         return this;
      }

      public AbilityStats build() {
         return new AbilityStats(
            this.energyCost,
            this.cooldownTicks,
            this.requiredMastery,
            this.damage,
            this.range,
            this.knockback,
            Collections.unmodifiableList(new ArrayList<>(this.extras)),
            this.blockDamage,
            List.copyOf(this.flags)
         );
      }
   }

   public static record Extra(String name, double defaultValue, double min, double max, String comment) {
   }

   public static record Flag(String name, boolean defaultValue, String comment) {
   }
}
