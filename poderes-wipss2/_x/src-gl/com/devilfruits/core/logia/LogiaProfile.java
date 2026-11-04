package com.devilfruits.core.logia;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;

public final class LogiaProfile {
   private final ResourceLocation element;
   private final Map<DamageCategory, HitRule> rules;
   private final List<LogiaCounter> counters;
   private final LogiaEnvironment environment;
   private final boolean elementalBody;
   private final float elementalDrain;

   private LogiaProfile(LogiaProfile.Builder b) {
      this.element = b.element;
      this.rules = new EnumMap<>(b.rules);
      this.counters = List.copyOf(b.counters);
      this.environment = b.environment;
      this.elementalBody = b.elementalBody;
      this.elementalDrain = b.elementalDrain;
      if (this.counters.size() > 16) {
         throw new IllegalStateException(this.element + ": at most 16 Logia counters");
      }
   }

   public static LogiaProfile.Builder builder(ResourceLocation element) {
      return new LogiaProfile.Builder(element);
   }

   public ResourceLocation element() {
      return this.element;
   }

   @Nullable
   public HitRule override(DamageCategory category) {
      return this.rules.get(category);
   }

   public List<LogiaCounter> counters() {
      return this.counters;
   }

   @Nullable
   public LogiaCounter counter(String id) {
      for (LogiaCounter counter : this.counters) {
         if (counter.id().equals(id)) {
            return counter;
         }
      }

      return null;
   }

   public int counterIndex(String id) {
      for (int i = 0; i < this.counters.size(); i++) {
         if (this.counters.get(i).id().equals(id)) {
            return i;
         }
      }

      return -1;
   }

   public LogiaEnvironment environment() {
      return this.environment;
   }

   public boolean allowsElementalBody() {
      return this.elementalBody;
   }

   public float elementalDrain() {
      return this.elementalDrain;
   }

   public static final class Builder {
      private final ResourceLocation element;
      private final Map<DamageCategory, HitRule> rules = new EnumMap<>(DamageCategory.class);
      private final List<LogiaCounter> counters = new ArrayList<>();
      private LogiaEnvironment environment = LogiaEnvironment.NONE;
      private boolean elementalBody = true;
      private float elementalDrain = 4.0F;

      private Builder(ResourceLocation element) {
         this.element = element;
      }

      public LogiaProfile.Builder rule(DamageCategory category, HitRule rule) {
         if (category == DamageCategory.ALWAYS) {
            throw new IllegalArgumentException("ALWAYS damage cannot be negated");
         } else {
            this.rules.put(category, rule);
            return this;
         }
      }

      public LogiaProfile.Builder counter(LogiaCounter counter) {
         this.counters.add(counter);
         return this;
      }

      public LogiaProfile.Builder counter(LogiaCounter.Builder counter) {
         return this.counter(counter.build());
      }

      public LogiaProfile.Builder environment(LogiaEnvironment environment) {
         this.environment = environment;
         return this;
      }

      public LogiaProfile.Builder elementalBody(boolean allowed, float drainPerSecond) {
         this.elementalBody = allowed;
         this.elementalDrain = drainPerSecond;
         return this;
      }

      public LogiaProfile build() {
         return new LogiaProfile(this);
      }
   }
}
