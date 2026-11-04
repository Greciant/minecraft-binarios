package com.devilfruits.core.logia;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;

public record LogiaCounter(
   String id,
   List<CounterTrigger> triggers,
   Set<LogiaCounter.Action> actions,
   int lingerTicks,
   float energyDrainPerSecond,
   float abilityPower,
   float speedPenalty,
   List<Holder<MobEffect>> debuffs,
   @Nullable ResourceLocation hudIcon
) {
   public boolean has(LogiaCounter.Action action) {
      return this.actions.contains(action);
   }

   public static LogiaCounter.Builder builder(String id) {
      return new LogiaCounter.Builder(id);
   }

   public static enum Action {
      SUPPRESS_INTANGIBILITY,
      BLOCK_ABILITIES,
      FORCE_REFORM,
      DEBUFF,
      DRAIN_ENERGY,
      WEAKEN_ABILITIES,
      SLOW;
   }

   public static final class Builder {
      private final String id;
      private final List<CounterTrigger> triggers = new ArrayList<>();
      private final Set<LogiaCounter.Action> actions = EnumSet.noneOf(LogiaCounter.Action.class);
      private int lingerTicks;
      private float energyDrain;
      private float abilityPower = 1.0F;
      private float speedPenalty;
      private final List<Holder<MobEffect>> debuffs = new ArrayList<>();
      @Nullable
      private ResourceLocation hudIcon;

      private Builder(String id) {
         this.id = id;
      }

      public LogiaCounter.Builder trigger(CounterTrigger trigger) {
         this.triggers.add(trigger);
         return this;
      }

      public LogiaCounter.Builder trigger(CounterTrigger.Kind kind) {
         return this.trigger(CounterTrigger.of(kind));
      }

      public LogiaCounter.Builder action(LogiaCounter.Action... actions) {
         this.actions.addAll(List.of(actions));
         return this;
      }

      public LogiaCounter.Builder linger(int ticks) {
         this.lingerTicks = ticks;
         return this;
      }

      public LogiaCounter.Builder drain(float perSecond) {
         this.energyDrain = perSecond;
         this.actions.add(LogiaCounter.Action.DRAIN_ENERGY);
         return this;
      }

      public LogiaCounter.Builder weaken(float abilityPower) {
         this.abilityPower = abilityPower;
         this.actions.add(LogiaCounter.Action.WEAKEN_ABILITIES);
         return this;
      }

      public LogiaCounter.Builder slow(float penalty) {
         this.speedPenalty = penalty;
         this.actions.add(LogiaCounter.Action.SLOW);
         return this;
      }

      public LogiaCounter.Builder debuff(Holder<MobEffect> effect) {
         this.debuffs.add(effect);
         this.actions.add(LogiaCounter.Action.DEBUFF);
         return this;
      }

      public LogiaCounter.Builder icon(ResourceLocation sprite) {
         this.hudIcon = sprite;
         return this;
      }

      public LogiaCounter build() {
         if (this.triggers.isEmpty()) {
            throw new IllegalStateException("Logia counter " + this.id + " has no trigger");
         } else {
            return new LogiaCounter(
               this.id,
               List.copyOf(this.triggers),
               Set.copyOf(
                  (Collection<? extends LogiaCounter.Action>)(this.actions.isEmpty() ? EnumSet.of(LogiaCounter.Action.SUPPRESS_INTANGIBILITY) : this.actions)
               ),
               this.lingerTicks,
               this.energyDrain,
               this.abilityPower,
               this.speedPenalty,
               List.copyOf(this.debuffs),
               this.hudIcon
            );
         }
      }
   }
}
