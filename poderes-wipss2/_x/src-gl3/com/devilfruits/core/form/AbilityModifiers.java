package com.devilfruits.core.form;

import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;

public record AbilityModifiers(
   float damage, float range, float knockback, float energyCost, float cooldown, String variant, @Nullable ResourceLocation animation
) {
   public static final AbilityModifiers NONE = new AbilityModifiers(1.0F, 1.0F, 1.0F, 1.0F, 1.0F, "", null);

   public static AbilityModifiers.Builder variant(String variant) {
      return new AbilityModifiers.Builder().variant(variant);
   }

   public static AbilityModifiers.Builder builder() {
      return new AbilityModifiers.Builder();
   }

   public boolean isDefault() {
      return this.equals(NONE);
   }

   public static final class Builder {
      private float damage = 1.0F;
      private float range = 1.0F;
      private float knockback = 1.0F;
      private float energyCost = 1.0F;
      private float cooldown = 1.0F;
      private String variant = "";
      @Nullable
      private ResourceLocation animation;

      public AbilityModifiers.Builder damage(float multiplier) {
         this.damage = multiplier;
         return this;
      }

      public AbilityModifiers.Builder range(float multiplier) {
         this.range = multiplier;
         return this;
      }

      public AbilityModifiers.Builder knockback(float multiplier) {
         this.knockback = multiplier;
         return this;
      }

      public AbilityModifiers.Builder energyCost(float multiplier) {
         this.energyCost = multiplier;
         return this;
      }

      public AbilityModifiers.Builder cooldown(float multiplier) {
         this.cooldown = multiplier;
         return this;
      }

      public AbilityModifiers.Builder variant(String variant) {
         this.variant = variant;
         return this;
      }

      public AbilityModifiers.Builder animation(ResourceLocation animation) {
         this.animation = animation;
         return this;
      }

      public AbilityModifiers build() {
         return new AbilityModifiers(this.damage, this.range, this.knockback, this.energyCost, this.cooldown, this.variant, this.animation);
      }
   }
}
