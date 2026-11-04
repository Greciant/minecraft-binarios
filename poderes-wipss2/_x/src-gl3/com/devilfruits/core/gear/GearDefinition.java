package com.devilfruits.core.gear;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public record GearDefinition(
   String name, int ordinal, int unlockMastery, boolean available, @Nullable String form, @Nullable ResourceLocation icon, List<GearDefinition> subGears
) {
   public static GearDefinition.Builder builder(String name, int ordinal) {
      return new GearDefinition.Builder(name, ordinal);
   }

   public ResourceLocation iconFor(ResourceLocation fruit) {
      return this.icon != null ? this.icon : ResourceLocation.fromNamespaceAndPath(fruit.getNamespace(), "gear/" + fruit.getPath() + "/" + this.name);
   }

   public String translationKey(ResourceLocation fruit) {
      return "gear." + fruit.getNamespace() + "." + fruit.getPath().replace('/', '.') + "." + this.name;
   }

   public Component displayName(ResourceLocation fruit) {
      return Component.translatable(this.translationKey(fruit));
   }

   public boolean leaf() {
      return this.subGears.isEmpty();
   }

   @Nullable
   public GearDefinition sub(String name) {
      for (GearDefinition sub : this.subGears) {
         if (sub.name.equals(name)) {
            return sub;
         }
      }

      return null;
   }

   public static final class Builder {
      private final String name;
      private final int ordinal;
      private int unlockMastery = 1;
      private boolean available = true;
      @Nullable
      private String form;
      @Nullable
      private ResourceLocation icon;
      private final List<GearDefinition> subGears = new ArrayList<>();

      private Builder(String name, int ordinal) {
         if (!name.matches("[a-z0-9_]+")) {
            throw new IllegalArgumentException("Gear names must be [a-z0-9_]+: " + name);
         } else {
            this.name = name;
            this.ordinal = ordinal;
         }
      }

      public GearDefinition.Builder form(String form) {
         this.form = form;
         return this;
      }

      public GearDefinition.Builder mastery(int mastery) {
         this.unlockMastery = mastery;
         return this;
      }

      public GearDefinition.Builder placeholder() {
         this.available = false;
         return this;
      }

      public GearDefinition.Builder icon(ResourceLocation sprite) {
         this.icon = sprite;
         return this;
      }

      public GearDefinition.Builder sub(GearDefinition.Builder sub) {
         this.subGears.add(sub.build());
         return this;
      }

      public GearDefinition build() {
         return new GearDefinition(this.name, this.ordinal, this.unlockMastery, this.available, this.form, this.icon, List.copyOf(this.subGears));
      }
   }
}
