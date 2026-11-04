package com.awakenedmod.awakened.core.relic;

import com.awakenedmod.awakened.core.skill.Stat;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Rarity;

public class Relic {
   private final String id;
   private final Rarity rarity;
   private final int themeColor;
   private final String powerId;
   private final boolean signature;
   private final List<RelicEffect> effects;

   protected Relic(Relic.Builder builder) {
      this.id = builder.id;
      this.rarity = builder.rarity;
      this.themeColor = builder.themeColor;
      this.powerId = builder.powerId;
      this.signature = builder.signature;
      this.effects = List.copyOf(builder.effects);
   }

   public final String id() {
      return this.id;
   }

   public Rarity rarity() {
      return this.rarity;
   }

   public int themeColor() {
      return this.themeColor;
   }

   public String powerId() {
      return this.powerId;
   }

   public boolean isUniversal() {
      return this.powerId == null;
   }

   public boolean isSignature() {
      return this.signature;
   }

   public List<RelicEffect> effects() {
      return this.effects;
   }

   public float statBonus(RelicContext ctx, Stat stat) {
      float sum = 0.0F;

      for (RelicEffect effect : this.effects) {
         if (effect.stat() == stat) {
            sum += effect.amount(ctx);
         }
      }

      return sum;
   }

   public Component displayName() {
      return Component.translatable("relic.awakened." + this.id);
   }

   public Component description() {
      return Component.translatable("relic.awakened." + this.id + ".desc");
   }

   public static Relic.Builder builder(String id) {
      return new Relic.Builder(id);
   }

   public static final class Builder {
      private final String id;
      private Rarity rarity = Rarity.RARE;
      private int themeColor = -1;
      private String powerId = null;
      private boolean signature = false;
      private final List<RelicEffect> effects = new ArrayList<>();

      private Builder(String id) {
         this.id = id;
      }

      public Relic.Builder rarity(Rarity rarity) {
         this.rarity = rarity;
         return this;
      }

      public Relic.Builder color(int argb) {
         this.themeColor = argb;
         return this;
      }

      public Relic.Builder power(String powerId) {
         this.powerId = powerId;
         return this;
      }

      public Relic.Builder signature() {
         this.signature = true;
         return this;
      }

      public Relic.Builder stat(Stat stat, float amount) {
         return this.stat(stat, amount, Condition.ALWAYS);
      }

      public Relic.Builder stat(Stat stat, float amount, Condition condition) {
         this.effects.add(new RelicEffect(stat, amount, condition));
         return this;
      }

      public Relic build() {
         return new Relic(this);
      }
   }
}
