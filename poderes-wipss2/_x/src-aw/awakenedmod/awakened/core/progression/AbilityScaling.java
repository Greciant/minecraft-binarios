package com.awakenedmod.awakened.core.progression;

import com.awakenedmod.awakened.core.skill.Stat;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;

public final class AbilityScaling {
   public static final AbilityScaling NONE = new AbilityScaling(List.of());
   private final List<AbilityScaling.Entry> entries;

   private AbilityScaling(List<AbilityScaling.Entry> entries) {
      this.entries = List.copyOf(entries);
   }

   public boolean isEmpty() {
      return this.entries.isEmpty();
   }

   public List<AbilityScaling.Entry> entries() {
      return this.entries;
   }

   public static AbilityScaling.Builder of() {
      return new AbilityScaling.Builder();
   }

   public static AbilityScaling.Entry radiusPerPointEntry(double base, double step, int cap) {
      return new AbilityScaling.Entry("radius", AbilityScaling.Unit.BLOCKS, AbilityScaling.Kind.PER_POINT, base, cap, step);
   }

   public static final class Builder {
      private final List<AbilityScaling.Entry> list = new ArrayList<>();

      public AbilityScaling.Builder damage(double base) {
         return this.value("damage", AbilityScaling.Unit.RAW, base);
      }

      public AbilityScaling.Builder healing(double base) {
         return this.value("healing", AbilityScaling.Unit.RAW, base);
      }

      public AbilityScaling.Builder knockback(double base) {
         return this.value("knockback", AbilityScaling.Unit.RAW, base);
      }

      public AbilityScaling.Builder radius(double blocks) {
         return this.value("radius", AbilityScaling.Unit.BLOCKS, blocks);
      }

      public AbilityScaling.Builder range(double blocks) {
         return this.value("range", AbilityScaling.Unit.BLOCKS, blocks);
      }

      public AbilityScaling.Builder durationTicks(int ticks) {
         return this.value("duration", AbilityScaling.Unit.SECONDS, (double)ticks / 20.0);
      }

      public AbilityScaling.Builder durationTicks(int ticks, int maxTicks) {
         if (maxTicks % 20 != 0) {
            throw new IllegalArgumentException("duration cap must be whole seconds: " + maxTicks);
         } else {
            this.list
               .add(new AbilityScaling.Entry("duration", AbilityScaling.Unit.SECONDS, AbilityScaling.Kind.MULTIPLIER, (double)ticks / 20.0, maxTicks / 20, 0.0));
            return this;
         }
      }

      public AbilityScaling.Builder targets(int base, int cap) {
         return this.count("targets", base, cap);
      }

      public AbilityScaling.Builder charges(int base, int cap) {
         return this.count("charges", base, cap);
      }

      public AbilityScaling.Builder boostsDamage() {
         return this.boosts("damage");
      }

      public AbilityScaling.Builder boostsRadius() {
         return this.boosts("radius");
      }

      public AbilityScaling.Builder boostsRange() {
         return this.boosts("range");
      }

      public AbilityScaling.Builder boostsDuration() {
         return this.boosts("duration");
      }

      public AbilityScaling.Builder boosts(String labelKey) {
         this.list.add(new AbilityScaling.Entry(labelKey, AbilityScaling.Unit.RAW, AbilityScaling.Kind.QUALITATIVE, 0.0, 0, 0.0));
         return this;
      }

      public AbilityScaling.Builder value(String labelKey, AbilityScaling.Unit unit, double base) {
         this.list.add(new AbilityScaling.Entry(labelKey, unit, AbilityScaling.Kind.MULTIPLIER, base, 0, 0.0));
         return this;
      }

      public AbilityScaling.Builder count(String labelKey, int base, int cap) {
         this.list.add(new AbilityScaling.Entry(labelKey, AbilityScaling.Unit.COUNT, AbilityScaling.Kind.COUNT, (double)base, cap, 0.0));
         return this;
      }

      public AbilityScaling.Builder radiusPerPoint(double base, double step, int cap) {
         return this.add(AbilityScaling.radiusPerPointEntry(base, step, cap));
      }

      public AbilityScaling.Builder add(AbilityScaling.Entry entry) {
         this.list.add(entry);
         return this;
      }

      public AbilityScaling build() {
         return this.list.isEmpty() ? AbilityScaling.NONE : new AbilityScaling(this.list);
      }
   }

   public static record Entry(String labelKey, AbilityScaling.Unit unit, AbilityScaling.Kind kind, double base, int cap, double step) {
      public double perPointValue(float abilityPower) {
         return this.capped(this.base + Math.max(0.0, (double)(abilityPower / Stat.ABILITY_POWER.perPoint())) * this.step);
      }

      public Component label() {
         return Component.translatable("awakened.scaling.awakened." + this.labelKey);
      }

      public double before(int masteryLevel) {
         return this.kind != AbilityScaling.Kind.COUNT && this.kind != AbilityScaling.Kind.PER_POINT
            ? this.capped(this.base * (double)MasteryScaling.multiplier(masteryLevel))
            : this.base;
      }

      public double after(int masteryLevel, float abilityPower) {
         if (this.kind == AbilityScaling.Kind.PER_POINT) {
            return this.perPointValue(abilityPower);
         } else if (this.kind == AbilityScaling.Kind.COUNT) {
            int steps = Math.round(abilityPower / Stat.ABILITY_POWER.perPoint());
            double value = this.base + (double)steps;
            return this.cap > 0 ? Math.min((double)this.cap, value) : value;
         } else {
            return this.capped(this.base * (double)MasteryScaling.multiplier(masteryLevel) * (1.0 + (double)abilityPower));
         }
      }

      private double capped(double value) {
         return this.cap > 0 ? Math.min((double)this.cap, value) : value;
      }
   }

   public static enum Kind {
      MULTIPLIER,
      COUNT,
      QUALITATIVE,
      PER_POINT;
   }

   public static enum Unit {
      RAW,
      BLOCKS,
      SECONDS,
      COUNT;
   }
}
