package com.awakenedmod.awakened.core.relic;

import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;

public final class Condition {
   public static final Condition ALWAYS = new Condition(null, ctx -> 1.0);
   @Nullable
   private final String id;
   private final ToDoubleFunction<RelicContext> factor;

   private Condition(@Nullable String id, ToDoubleFunction<RelicContext> factor) {
      this.id = id;
      this.factor = factor;
   }

   public static Condition gate(String id, Predicate<RelicContext> test) {
      return new Condition(id, ctx -> test.test(ctx) ? 1.0 : 0.0);
   }

   public static Condition scale(String id, ToDoubleFunction<RelicContext> factor) {
      return new Condition(id, ctx -> Math.max(0.0, factor.applyAsDouble(ctx)));
   }

   @Nullable
   public String id() {
      return this.id;
   }

   public float factor(RelicContext ctx) {
      return (float)this.factor.applyAsDouble(ctx);
   }

   public boolean isAlways() {
      return this.id == null;
   }

   @Nullable
   public Component describe() {
      return this.id == null ? null : Component.translatable("relic.awakened.cond." + this.id);
   }
}
