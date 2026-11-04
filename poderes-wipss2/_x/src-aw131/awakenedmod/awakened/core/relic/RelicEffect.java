package com.awakenedmod.awakened.core.relic;

import com.awakenedmod.awakened.core.skill.Stat;

public final class RelicEffect {
   private final Stat stat;
   private final float baseAmount;
   private final Condition condition;

   public RelicEffect(Stat stat, float baseAmount, Condition condition) {
      this.stat = stat;
      this.baseAmount = baseAmount;
      this.condition = condition;
   }

   public Stat stat() {
      return this.stat;
   }

   public float baseAmount() {
      return this.baseAmount;
   }

   public Condition condition() {
      return this.condition;
   }

   public float amount(RelicContext ctx) {
      return this.baseAmount * this.condition.factor(ctx);
   }
}
